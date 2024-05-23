package com.automation;

public class AdditionalExam {
    String code;
    String name;
    String date;
    int credits;
    int ects;
    String grade;

    public AdditionalExam(String code, String name, String date, int credits, int ects, String grade) {
        this.code = code;
        this.name = name;
        this.date = date;
        this.credits = credits;
        this.ects = ects;
        this.grade = grade;
    }

    @Override
    public String toString() {
        return "AdditionalExam{" +
                "code='" + code + '\'' +
                ", name='" + name + '\'' +
                ", date='" + date + '\'' +
                ", credits=" + credits +
                ", ects=" + ects +
                ", grade='" + grade + '\'' +
                '}';
    }
}