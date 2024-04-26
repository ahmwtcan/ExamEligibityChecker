package com.automation;

public class Course {
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