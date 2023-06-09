import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

public class OnlineCoursesAnalyzer {

  List<Course> courses = new ArrayList<>();

  public OnlineCoursesAnalyzer(String datasetPath) {
    BufferedReader br = null;
    String line;
    try {
      br = new BufferedReader(new FileReader(datasetPath, StandardCharsets.UTF_8));
      br.readLine();
      while ((line = br.readLine()) != null) {
        String[] info = line.split(",(?=([^\\\"]*\\\"[^\\\"]*\\\")*[^\\\"]*$)", -1);
        Course course = new Course(info[0], info[1], new Date(info[2]), info[3], info[4], info[5],
            Integer.parseInt(info[6]), Integer.parseInt(info[7]), Integer.parseInt(info[8]),
            Integer.parseInt(info[9]), Integer.parseInt(info[10]), Double.parseDouble(info[11]),
            Double.parseDouble(info[12]), Double.parseDouble(info[13]), Double.parseDouble(info[14]),
            Double.parseDouble(info[15]), Double.parseDouble(info[16]), Double.parseDouble(info[17]),
            Double.parseDouble(info[18]), Double.parseDouble(info[19]), Double.parseDouble(info[20]),
            Double.parseDouble(info[21]), Double.parseDouble(info[22]));
        courses.add(course);
      }
    } catch (IOException e) {
      e.printStackTrace();
    } finally {
      if (br != null) {
        try {
          br.close();
        } catch (IOException e) {
          e.printStackTrace();
        }
      }
    }
  }

  //1
  public Map<String, Integer> getPtcpCountByInst() {
    return courses.stream()
        .collect(
            Collectors.groupingBy(
                c -> c.institution,
                Collectors.summingInt(Course::getParticipants)
            )
        )
        .entrySet()
        .stream()
        .sorted(Map.Entry.comparingByKey())
        .collect(
            Collectors.toMap(
                Map.Entry::getKey,
                Map.Entry::getValue,
                (oldVal, newVal) -> oldVal,
                LinkedHashMap::new
            )
        );
  }

  //2
  public Map<String, Integer> getPtcpCountByInstAndSubject() {
    return courses.stream()
        .collect(
            Collectors.groupingBy(
                c -> c.institution + "-" + c.subject,
                Collectors.summingInt(Course::getParticipants)
            )
        )
        .entrySet()
        .stream()
        .sorted(
            (e1, e2) -> (Objects.equals(e1.getValue(), e2.getValue())
                ? e1.getKey().compareTo(e2.getKey())
                : Integer.compare(e2.getValue(), e1.getValue())
            )
        )
        .collect(
            Collectors.toMap(
                Map.Entry::getKey,
                Map.Entry::getValue,
                (oldVal, newVal) -> oldVal,
                LinkedHashMap::new
            )
        );
  }

  //3
  public Map<String, List<List<String>>> getCourseListOfInstructor() {
    Map<String, List<List<String>>> result = new HashMap<>();
    courses.forEach(
        (c) -> {
          if (c.instructors.contains(",")) {
            for (String i : c.instructors.split(",")) {
              result.merge(
                  i.trim(),
                  new ArrayList<>(Arrays.asList(
                      new ArrayList<>(),
                      new ArrayList<>(Collections.singletonList(c.title))
                  )),
                  (oldVal, newVal) -> {
                    if (!oldVal.get(1).contains(newVal.get(1).get(0))) {
                      oldVal.get(1).add(newVal.get(1).get(0));
                      oldVal.get(1).sort(String::compareTo);
                    }
                    return oldVal;
                  }
              );
            }
          } else {
            result.merge(
                c.instructors.trim(),
                new ArrayList<>(Arrays.asList(new ArrayList<>(Collections.singletonList(c.title)), new ArrayList<>())),
                (oldVal, newVal) -> {
                  if (!oldVal.get(0).contains(newVal.get(0).get(0))) {
                    oldVal.get(0).add(newVal.get(0).get(0));
                    oldVal.get(0).sort(String::compareTo);
                  }
                  return oldVal;
                }
            );
          }
        }
    );
    return result;
  }

  //4
  public List<String> getCourses(int topK, String by) {
    return courses.stream()
        .sorted(
            (c1, c2) -> (
                by.equals("hours") ? (
                    c1.totalHours == c2.totalHours
                        ? c1.title.compareTo(c2.title)
                        : Double.compare(c2.totalHours, c1.totalHours)
                ) : (
                    c1.participants == c2.participants
                        ? c1.title.compareTo(c2.title)
                        : Integer.compare(c2.participants, c1.participants)
                )
            )
        )
        .map((c) -> c.title)
        .distinct()
        .limit(topK)
        .collect(Collectors.toList());
  }

  //5
  public List<String> searchCourses(String courseSubject, double percentAudited, double totalCourseHours) {
    return courses.stream()
        .filter(
            (c) -> (
                c.subject.toLowerCase().contains(courseSubject.toLowerCase())
                    && c.percentAudited >= percentAudited
                    && c.totalHours <= totalCourseHours
            )
        )
        .map(c -> c.title)
        .distinct()
        .sorted()
        .collect(Collectors.toList());
  }

  //6
  public List<String> recommendCourses(int age, int gender, int isBachelorOrHigher) {
    return courses.stream()
        .collect(Collectors.groupingBy(c -> c.number))
        .entrySet()
        .stream()
        .map(
            entry -> {
              double avgAge = entry.getValue().stream()
                  .collect(Collectors.averagingDouble(Course::getMedianAge));
              double avgMale = entry.getValue().stream()
                  .collect(Collectors.averagingDouble(Course::getPercentMale));
              double avgDegree = entry.getValue().stream()
                  .collect(Collectors.averagingDouble(Course::getPercentDegree));
              return new AbstractMap.SimpleImmutableEntry<>(
                  entry.getValue().stream()
                      .max(Comparator.comparing(c -> c.launchDate))
                      .get()
                      .title,
                  Math.pow(age - avgAge, 2)
                      + Math.pow(gender * 100 - avgMale, 2)
                      + Math.pow(isBachelorOrHigher * 100 - avgDegree, 2)
              );
            }
        )
        .sorted(
            (e1, e2) -> (
                e1.getValue().equals(e2.getValue())
                    ? e1.getKey().compareTo(e2.getKey())
                    : e1.getValue().compareTo(e2.getValue())
            )
        )
        .map(AbstractMap.SimpleImmutableEntry::getKey)
        .distinct()
        .limit(10)
        .toList();
  }

}

class Course {
  String institution;
  String number;
  Date launchDate;
  String title;
  String instructors;
  String subject;
  int year;
  int honorCode;
  int participants;
  int audited;
  int certified;
  double percentAudited;
  double percentCertified;
  double percentCertified50;
  double percentVideo;
  double percentForum;
  double gradeHigherZero;
  double totalHours;
  double medianHoursCertification;
  double medianAge;
  double percentMale;
  double percentFemale;
  double percentDegree;

  public Course(String institution, String number, Date launchDate,
                String title, String instructors, String subject,
                int year, int honorCode, int participants,
                int audited, int certified, double percentAudited,
                double percentCertified, double percentCertified50,
                double percentVideo, double percentForum, double gradeHigherZero,
                double totalHours, double medianHoursCertification,
                double medianAge, double percentMale, double percentFemale,
                double percentDegree) {
    this.institution = institution;
    this.number = number;
    this.launchDate = launchDate;
    if (title.startsWith("\"")) title = title.substring(1);
    if (title.endsWith("\"")) title = title.substring(0, title.length() - 1);
    this.title = title;
    if (instructors.startsWith("\"")) instructors = instructors.substring(1);
    if (instructors.endsWith("\"")) instructors = instructors.substring(0, instructors.length() - 1);
    this.instructors = instructors;
    if (subject.startsWith("\"")) subject = subject.substring(1);
    if (subject.endsWith("\"")) subject = subject.substring(0, subject.length() - 1);
    this.subject = subject;
    this.year = year;
    this.honorCode = honorCode;
    this.participants = participants;
    this.audited = audited;
    this.certified = certified;
    this.percentAudited = percentAudited;
    this.percentCertified = percentCertified;
    this.percentCertified50 = percentCertified50;
    this.percentVideo = percentVideo;
    this.percentForum = percentForum;
    this.gradeHigherZero = gradeHigherZero;
    this.totalHours = totalHours;
    this.medianHoursCertification = medianHoursCertification;
    this.medianAge = medianAge;
    this.percentMale = percentMale;
    this.percentFemale = percentFemale;
    this.percentDegree = percentDegree;
  }

  int getParticipants(){
    return participants;
  }

  double getMedianAge(){
    return medianAge;
  }

  double getPercentMale(){
    return percentMale;
  }

  double getPercentDegree(){
    return percentDegree;
  }
}