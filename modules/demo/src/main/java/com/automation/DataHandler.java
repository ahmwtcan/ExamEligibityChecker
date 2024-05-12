package com.automation;

import java.util.*;
import java.util.regex.*;

public class DataHandler {

    private static final Pattern SEMESTER_PATTERN = Pattern.compile("20\\d{2} (FALL|SPRING|SUMMER)");

    private static final Pattern COURSE_PATTERN = Pattern.compile(
            "([A-Z]+\\s\\d{3})\\s+([^0-9]+?)\\s+(\\d+)\\s/\\s(\\d+)\\s([A-Z]{1,2}(?:\\([A-Z]{1,2}\\))?)\\s*(\\(R\\))?\\s(\\d+\\.\\d{2})");

    private static final Pattern SEMESTER_DETAILS_PATTERN = Pattern.compile(
            "CGPA:\\s*(\\d+\\.\\d{2})\\s*GPA:\\s*(\\d+\\.\\d{2})\\s*Completed Credits:\\s*(\\d+)\\s*Semester Credits:\\s*(\\d+)");

    // pattern for CGPA: 2.73 GPA: 3.79
    private static final Pattern CGPA_PATTERN = Pattern.compile("^CGPA:\\s*(\\d+\\.\\d+)\\s*GPA:\\s*(\\d+\\.\\d+)$",
            Pattern.MULTILINE);

    public static Student getStudent(String transcript) {

        Pattern simplePattern = Pattern.compile(
                "(?s)Name,\\s*Surname\\s*:\\s*(.+?)\\s*" +
                        "Student\\s*Number\\s*:\\s*(\\d+)\\s*",
                Pattern.DOTALL);

        Matcher simpleMatcher = simplePattern.matcher(transcript);

        if (simpleMatcher.find()) {
            String name = simpleMatcher.group(1).trim();
            String studentNumber = simpleMatcher.group(2).trim();

            Pattern totalCreditsPattern = Pattern
                    .compile("Completed\\/Total\\s*Credits\\s*(?:\\b|\\s)?(\\d+)\\s*(?:\\/|\\s)?(\\d+)\\s*");

            Matcher totalCreditsMatcher = totalCreditsPattern.matcher(transcript);
            String cCredits = "";
            String totalCredits = "";
            if (totalCreditsMatcher.find()) {
                cCredits = totalCreditsMatcher.group(1);
                totalCredits = totalCreditsMatcher.group(2);
            }

            System.out.println("Name: " + name);
            System.out.println("Student Number: " + studentNumber);
            System.out.println("Completed Credits: " + cCredits);
            System.out.println("Total Credits: " + totalCredits);
            List<Semester> semesters = parseTranscript(transcript);

            List<Course> courses = new ArrayList<>();
            for (Semester semester : semesters) {
                courses.addAll(semester.courses);
            }

            // copleted credits and cgpa
            int completedCredits = 0;
            double currentCGPA = Double.parseDouble(semesters.get(semesters.size() - 1).cgpa);
            for (Semester semester : semesters) {
                completedCredits += semester.completedCredits;
            }

            return new Student(name, studentNumber, completedCredits, currentCGPA, semesters, courses, false, true,
                    false,
                    false, countSemester(semesters), Integer.parseInt(totalCredits));

        } else {
            System.out.println("Failed to match extended info. Check regex and transcript format.");
        }
        return null;

    }

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

    public static List<Course> parseCourses(String section) {
        List<Course> courses = new ArrayList<>();
        Matcher courseMatcher = COURSE_PATTERN.matcher(section);

        while (courseMatcher.find()) {
            String code = courseMatcher.group(1);
            String name = courseMatcher.group(2).trim();
            int credits = Integer.parseInt(courseMatcher.group(3));
            String grade = courseMatcher.group(5).trim()
                    + (courseMatcher.group(6) != null ? courseMatcher.group(6) : "");
            double totalCredits = Double.parseDouble(courseMatcher.group(7)); // Ensure this captures the correct value

            courses.add(new Course(code, name, credits, grade, totalCredits));
        }
        return courses;
    }

}
