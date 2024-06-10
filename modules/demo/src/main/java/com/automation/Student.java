package com.automation;

import java.util.List;

public class Student {
    String name;
    String studentNumber;
    public int completedCredits;
    public Double cgpa;
    public List<Semester> semesters;
    public List<Course> courses;
    public boolean maxStudyDurationExceeded;
    public boolean isAllCoursesTaken;
    public boolean gotExamRightAndUsed;
    public boolean gotExamRight;
    public int semesterCount;
    public int totalCredits;

    List<AdditionalExam> additionalExams;

    // Gerekli getter ve setter metodları

    Student(String name, String studentNumber,
            int completedCredits, Double cgpa, List<Semester> semesters, List<Course> courses,
            boolean maxStudyDurationExceeded, boolean isAllCoursesTaken, boolean gotExamRightAndUsed,
            boolean gotExamRight,
            int semesterCount, int totalCredits, List<AdditionalExam> additionalExams) {
        this.name = name;
        this.studentNumber = studentNumber;
        this.completedCredits = completedCredits;
        this.cgpa = cgpa;
        this.semesters = semesters;
        this.courses = courses;
        this.maxStudyDurationExceeded = maxStudyDurationExceeded;
        this.isAllCoursesTaken = isAllCoursesTaken;
        this.gotExamRightAndUsed = gotExamRightAndUsed;
        this.gotExamRight = gotExamRight;
        this.semesterCount = semesterCount;
        this.totalCredits = totalCredits;
        this.additionalExams = additionalExams;
    }

    @Override
    public String toString() {
        return "Student{" +
                "name='" + name + '\'' +
                ", studentNumber='" + studentNumber + '\'' +
                ", completedCredits=" + completedCredits +
                ", cgpa=" + cgpa +
                ", maxStudyDurationExceeded=" + maxStudyDurationExceeded +
                ", isAllCoursesTaken=" + isAllCoursesTaken +
                ", gotExamRightAndUsed=" + gotExamRightAndUsed +
                ", gotExamRight=" + gotExamRight +
                ", semesterCount=" + semesterCount +
                ", totalCredits=" + totalCredits +

                '}';
    }

}