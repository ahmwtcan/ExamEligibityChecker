package com.automation;

public class Course {
    String code;
    String name;
    int credits;
    String grade;
    double gradePoints;

    Course(String code, String name, int credits, String grade, double gradePoints) {
        this.code = code;
        this.name = name;
        this.credits = credits;
        this.grade = grade;
        this.gradePoints = gradePoints;
    }

    @Override
    public String toString() {
        return "Course{" +
                "code='" + code + '\'' +
                ", name='" + name + '\'' +
                ", credits=" + credits +
                ", grade='" + grade + '\'' +
                ", gradePoints=" + gradePoints +
                '}';
    }

}