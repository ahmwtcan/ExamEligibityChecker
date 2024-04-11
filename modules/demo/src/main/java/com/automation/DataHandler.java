package com.automation;

import java.util.*;
import java.util.regex.*;

public class DataHandler {

    private static final Pattern SEMESTER_PATTERN = Pattern.compile("20\\d{2} (FALL|SPRING|SUMMER)");

    private static final Pattern COURSE_PATTERN = Pattern.compile(
            "([A-Z]+\\s\\d{3})\\s+([^0-9]+)\\s+(\\d+)\\s/\\s(\\d+)\\s([A-Z]+\\(?(AA)?\\)?|[WF])\\s(\\d+\\.\\d{2})");
    private static final Pattern SEMESTER_DETAILS_PATTERN = Pattern.compile(
            "CGPA:\\s*(\\d+\\.\\d{2})\\s*GPA:\\s*(\\d+\\.\\d{2})\\s*Completed Credits:\\s*(\\d+)\\s*Semester Credits:\\s*(\\d+)");

    // pattern for CGPA: 2.73 GPA: 3.79
    private static final Pattern CGPA_PATTERN = Pattern.compile("^CGPA:\\s*(\\d+\\.\\d+)\\s*GPA:\\s*(\\d+\\.\\d+)$",
            Pattern.MULTILINE);

    public static List<Semester> parseTranscript(String transcript) {
        List<Semester> semesters = new ArrayList<>();
        Matcher semesterMatcher = SEMESTER_PATTERN.matcher(transcript);
        while (semesterMatcher.find()) {
            int start = semesterMatcher.start();
            // Adjust how you find 'end' if needed to ensure it accurately captures the
            // section's end
            int end = transcript.indexOf("CGPA:", start) + 1;
            end = end > 0 ? transcript.indexOf("20", end) : transcript.length();
            if (end == -1)
                end = transcript.length();

            String section = transcript.substring(start, end);
            Semester semester = new Semester();
            semester.semesterName = semesterMatcher.group();
            Matcher detailsMatcher = SEMESTER_DETAILS_PATTERN.matcher(section);
            Matcher cgpaMatcher = CGPA_PATTERN.matcher(section);

            if (detailsMatcher.find()) {
                semester.cgpa = detailsMatcher.group(1);
                semester.gpa = detailsMatcher.group(2);
                semester.completedCredits = Integer.parseInt(detailsMatcher.group(3));
                semester.totalCredits = Integer.parseInt(detailsMatcher.group(4));
            } else if (cgpaMatcher.find()) { // Ensure this comes after checking detailsMatcher to avoid missing details
                semester.cgpa = cgpaMatcher.group(1);
                semester.gpa = cgpaMatcher.group(2);

            }

            semester.courses = parseCourses(section);
            semesters.add(semester);
        }
        return semesters;
    }

    // count semerster number excluding summer
    public static int countSemester(List<Semester> semesters) {
        Pattern summerPattern = Pattern.compile("SUMMER");
        int count = 0;
        for (Semester semester : semesters) {
            Matcher semesterMatcher = summerPattern.matcher(semester.semesterName);
            if (!semesterMatcher.find()) { // If the name does not match "SUMMER"
                count++;
            }
        }
        return count;
    }

    private static List<Course> parseCourses(String section) {
        List<Course> courses = new ArrayList<>();
        Matcher courseMatcher = COURSE_PATTERN.matcher(section);

        while (courseMatcher.find()) {
            Course course = new Course(
                    courseMatcher.group(1), // code
                    courseMatcher.group(2), // name
                    Integer.parseInt(courseMatcher.group(3)), // credits
                    courseMatcher.group(5).trim() // grade, including handling of "(R)" notation
            );
            courses.add(course);
        }

        return courses;
    }

    static class Semester {
        List<Course> courses;
        String cgpa;
        String gpa;
        String semesterName;
        int completedCredits;
        int totalCredits;

        @Override
        public String toString() {
            return "Semester: " + semesterName + "\n" +
                    "CGPA: " + cgpa + ", GPA: " + gpa + ", Completed Credits: "
                    + completedCredits + ", Total Credits: " + totalCredits + "\n";
        }
    }

    static class Course {
        String code;
        String name;
        int credits;
        String grade;

        Course(String code, String name, int credits, String grade) {
            this.code = code;
            this.name = name;
            this.credits = credits;
            this.grade = grade;
        }

        @Override
        public String toString() {
            return "Course{" +
                    "code='" + code + '\'' +
                    ", name='" + name + '\'' +
                    ", credits=" + credits +
                    ", grade='" + grade + '\'' +
                    '}';
        }

    }

}
