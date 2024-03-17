package com.automation;

import java.util.List;

import com.automation.DataHandler.Course;

public class EligibityChecker {

    public boolean checkEligibility(Student student) {

        // if student has a W OR L Grade in any course beside creditless Language
        // courses, return false
        for (Course course : student.courses) {
            if (course.credits != 0 && (course.grade.equals("W") || course.grade.equals("L"))) {
                return false;
            }

            checkInternship(course); // if student has an internship course and grade is not P, return false
        }

        return true;
    }

    private boolean checkInternship(Course course) {
        String intershipRegex = "([A-Z]{2}400)";

        if (!course.code.matches(intershipRegex)) {
            return false;
        }
        if (course.code.matches(intershipRegex) && !course.grade.equals("P")) {
            return false;
        }

        return true;
    }

    public class Semester {
        private String semesterName;
        private String cgpa;
        private String gpa;
        private int completedCredits;
        private int totalCredits;
        private List<Course> courses;

        // Gerekli getter ve setter metodları

        Semester(String semesterName, String cgpa, String gpa, int completedCredits, int totalCredits,
                List<Course> courses) {
            this.semesterName = semesterName;
            this.cgpa = cgpa;
            this.gpa = gpa;
            this.completedCredits = completedCredits;
            this.totalCredits = totalCredits;
            this.courses = courses;
        }

        @Override
        public String toString() {
            return "Semester{" +
                    "semesterName='" + semesterName + '\'' +
                    ", cgpa='" + cgpa + '\'' +
                    ", gpa='" + gpa + '\'' +
                    ", completedCredits=" + completedCredits +
                    ", totalCredits=" + totalCredits +
                    ", courses=" + courses +
                    '}';
        }
    }

    public class Student {
        private String id;
        private String name;
        private double gpa; // Genel Not Ortalaması
        private List<Course> courses; // Alınan dersler listesi
        private boolean maxStudyDurationExceeded; // Azami öğrenim süresi aşıldı mı?
        private AcademicStatus academicStatus;

        // Gerekli getter ve setter metodları

        Student(String id, String name, double gpa, List<Course> courses, boolean maxStudyDurationExceeded,
                AcademicStatus academicStatus) {
            this.id = id;
            this.name = name;
            this.gpa = gpa;
            this.courses = courses;
            this.maxStudyDurationExceeded = maxStudyDurationExceeded;
            this.academicStatus = academicStatus;
        }

        @Override
        public String toString() {
            return "Student{" +
                    "id='" + id + '\'' +
                    ", name='" + name + '\'' +
                    ", gpa=" + gpa +
                    ", courses=" + courses +
                    ", maxStudyDurationExceeded=" + maxStudyDurationExceeded +
                    ", academicStatus=" + academicStatus +
                    '}';
        }

    }

    public class AcademicStatus {
        private boolean compulsoryInternshipCompleted; // Zorunlu staj tamamlandı mı?
        private ExamRight examRight; // Sınav hakkı

        // Gerekli getter ve setter metodları

        AcademicStatus(boolean compulsoryInternshipCompleted, ExamRight examRight) {
            this.compulsoryInternshipCompleted = compulsoryInternshipCompleted;
            this.examRight = examRight;
        }

        @Override
        public String toString() {
            return "AcademicStatus{" +
                    "compulsoryInternshipCompleted=" + compulsoryInternshipCompleted +
                    ", examRight=" + examRight +
                    '}';
        }
    }

    public enum ExamRight {
        EK_SINAV,
        SINIRSIZ_SINAV,
        BELIRLI_DONEM_SINAV_HAKKI,
        HAK_YOK
    }

    public class ExamStatus {
        private Course course;
        private ExamRight examRight;
        private boolean isUsed; // Sınav hakkı kullanıldı mı?

        // Gerekli getter ve setter metodları

        ExamStatus(Course course, ExamRight examRight, boolean isUsed) {
            this.course = course;
            this.examRight = examRight;
            this.isUsed = isUsed;
        }

        @Override
        public String toString() {
            return "ExamStatus{" +
                    "course=" + course +
                    ", examRight=" + examRight +
                    ", isUsed=" + isUsed +
                    '}';
        }
    }
}
