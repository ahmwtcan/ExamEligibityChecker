package com.automation;

import java.util.List;

public class Semester {
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