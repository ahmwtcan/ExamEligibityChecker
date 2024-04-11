package com.automation;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.automation.DataHandler.Course;
import com.automation.DataHandler.Semester;
import com.automation.PdfParser.TableCourse;

public class EligibityChecker {

    List<TableCourse> tableCourses = PdfParser.parseTableCourses();
    private static final Map<String, Double> gradePoints = Map.of(
            "AA", 4.0,
            "BA", 3.5,
            "BB", 3.0,
            "CB", 2.5,
            "CC", 2.0,
            "DC", 1.5,
            "DD", 1.0,
            "FA", 0.0,
            "FF", 0.0);

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

    // if student has a FF grade in any table course, return false
    // else if student has a FF grade in any other course then he/she should have

    public boolean checkTableCourses(Student student) {
        for (Course course : student.courses) {
            for (TableCourse tableCourse : tableCourses) {
                if (course.code.equals(tableCourse.getCode()) && course.grade.equals("FF")) {
                    return false;
                } else if (!course.code.equals(tableCourse.getCode()) && course.grade.equals("FF")) {
                    return true;
                }
            }
        }
        return true;
    }

    public boolean checkFFgrades(Student student) {
        // if student has a FF grade more than once, return false
        int count = 0;
        for (Course course : student.courses) {
            if (course.grade.equals("FF")) {
                count++;
            }
        }
        if (count > 1) {
            return false;
        }
        return true;

    }

    public boolean checkGPA(Student student) {
        // if student's GPA is less than 2.0, return false
        if (student.gpa < 2.0) {
            return false;
        }
        return true;
    }

    public static boolean canGradeImprovementRaiseCGPA(List<Semester> semesters, List<String> table1Courses) {
        // Assume CGPA is taken from the last semester's CGPA
        double currentCGPA = Integer.parseInt(semesters.get(semesters.size() - 1).cgpa);
        if (currentCGPA >= 2.00) {
            return false; // Already meets or exceeds the threshold
        }

        // Total credits and grade points up to the last semester
        double totalCredits = 0;
        double totalGradePoints = 0;
        for (Semester semester : semesters) {
            for (Course course : semester.courses) {
                // Skip courses from Table 1 and courses with 'P' grade or 'FF' grade
                if (!table1Courses.contains(course.code) && !course.grade.equals("P") && !course.grade.equals("FF")) {
                    double gradePoints = getGradePoints(course.grade);
                    totalCredits += course.credits;
                    totalGradePoints += gradePoints * course.credits;
                }
            }
        }

        // The simplest improvement scenario: improving the grade of any course to the
        // next level
        // This simulates the minimal improvement necessary to check if CGPA can be
        // raised above 2.00
        double improvedGradePoints = getGradePoints("BA"); // Assuming BA as a significant improvement
        for (Course course : semesters.get(semesters.size() - 1).courses) {
            if (!course.grade.equals("P") && !course.grade.equals("FF") && !table1Courses.contains(course.code)) {
                // Calculate potential CGPA with an improved grade for this course
                double newTotalGradePoints = totalGradePoints - (getGradePoints(course.grade) * course.credits)
                        + (improvedGradePoints * course.credits);
                double potentialCGPA = newTotalGradePoints / totalCredits;
                if (potentialCGPA >= 2.00) {
                    return true; // Found a course improvement that can raise CGPA above 2.00
                }
            }
        }

        return false; // No single course improvement found that can raise CGPA above 2.00
    }

    // Convert a letter grade to grade pointss
    private static double getGradePoints(String grade) {
        return switch (grade) {
            case "AA" -> 4.0;
            case "BA" -> 3.5;
            case "BB" -> 3.0;
            case "CB" -> 2.5;
            case "CC" -> 2.0;
            case "DC" -> 1.5;
            case "DD" -> 1.0;
            // FF indicates fail, cannot be improved in this context
            case "FF" -> 0.0;
            default -> 0.0;
        };
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
