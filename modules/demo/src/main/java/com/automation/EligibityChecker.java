package com.automation;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.automation.PdfParser.TableCourse;

public class EligibityChecker {

    List<TableCourse> tableCourses = PdfParser.parseTableCourses();

    public EligibilityProcess checkEligibility(Student student) {
        EligibilityProcess process = new EligibilityProcess();
        // 2.Step
        // if student has a W OR L Grade in any course beside creditless Language
        // courses, return false
        if (!checkWithdrawalsAndLeaves(student)) {
            process.WorLflag = false;
        } else {
            System.out.println("Student has no W or L grade in any course beside creditless Language courses");
        }

        // 3. Step
        // if student has an internship course and grade is not P, return false

        if (!checkInternship(student)) {
            System.out.println("Student has not passed the internship course");
            process.internshipFlag = false;
        } else {
            System.out.println("Student has passed the internship course");
        }

        // 4. Step
        if (!student.isAllCoursesTaken) {
            System.out.println("Student has not taken all courses");
            process.allCoursesFlag = false;
        } else {
            System.out.println("Student has taken all courses");
        }
        // 5. Step
        if (!checkTableCourses(student)) {
            System.out.println("Student has a FF grade in a table course");
            process.tableCourseFlag = false;
        } else {
            System.out.println("Student has no FF grade in a table course");
        }

        // 6. Step
        if (!checkGPA(student)) {
            System.out.println("Student's GPA is less than 2.0");
            process.GPAFlag = false;
        } else {
            System.out.println("Student's GPA is above 2.0");
        }

        // 7. Step
        if (canGradeImprovementRaiseCGPA(student.semesters, tableCourses)) {
            System.out.println("Student can improve a grade to raise CGPA above 2.0");
            process.gradeImprovementFlag = true;
        }

        // 8. Step
        if (student.maxStudyDurationExceeded) {

            // if student has a FF grade in any table course or has a W or L grade in more
            // than 5 courses, return false
            if (!checkTableCourses(student)) {
                process.FFgradeFlag = false;
            }

        }

        return process;

    }

    public boolean checkWithdrawalsAndLeaves(Student student) {
        HashMap<String, List<String>> courseGrades = new HashMap<>();

        // Collect all grades for each course
        for (Course course : student.courses) {
            courseGrades.putIfAbsent(course.code, new ArrayList<>());
            courseGrades.get(course.code).add(course.grade);
        }

        // Check for W or L grades and validate subsequent completion
        for (Map.Entry<String, List<String>> entry : courseGrades.entrySet()) {
            boolean hasWithdrawalOrLeave = entry.getValue().stream()
                    .anyMatch(grade -> grade.equals("W") || grade.equals("L"));
            if (hasWithdrawalOrLeave) {
                // Check for any subsequent passing grade
                boolean hasPassedSubsequently = entry.getValue().stream()
                        .anyMatch(grade -> isPassingGrade(grade));
                if (!hasPassedSubsequently) {
                    System.out.println(
                            "Student has a W or L grade without passing subsequently in course: " + entry.getKey());
                    return false;
                }
            }
        }

        return true;
    }

    private boolean isPassingGrade(String grade) {
        // Define passing grades based on the academic grading system
        return !grade.matches("W|L|F|FF|FA");
    }

    private boolean checkInternship(Student student) {
        String intershipRegex = "([A-Z]{2,3} 400)";

        for (Course course : student.courses) {
            if (course.code.matches(intershipRegex) && !course.grade.equals("P")) {
                System.out.println("Student has not passed the internship course: " + course.code);
                return false;
            } else if (course.code.matches(intershipRegex) && course.grade.equals("P")) {
                System.out.println("Student has passed the internship course: " + course.code + "  " + course.grade);
                return true;
            }
        }
        System.out.println("Student has not taken the internship course");
        return false;
    }

    // if student has a FF grade in any table course, return false
    // else if student has only 1 FF grade in any other course then he/she should
    // have exam right
    public boolean checkTableCourses(Student student) {
        HashMap<String, List<String>> courseGrades = new HashMap<>();

        // Collect all grades for each course
        for (Course course : student.courses) {
            courseGrades.putIfAbsent(course.code, new ArrayList<>());
            courseGrades.get(course.code).add(course.grade);
        }

        // Check each course for the presence of an 'FF' grade and a subsequent passing
        // grade
        for (Map.Entry<String, List<String>> entry : courseGrades.entrySet()) {
            if (entry.getValue().contains("FF")) {
                // Check if there is a retake that is passed
                boolean hasPassedRetake = false;
                for (String grade : entry.getValue()) {
                    if (!grade.equals("FF") && !grade.equals("W") && !grade.equals("L") && !grade.equals("FA")) {
                        hasPassedRetake = true;
                        break;
                    }
                }
                // If there's no successful retake, return false
                if (!hasPassedRetake) {
                    System.out.println("Student failed to pass after an FF in course: " + entry.getKey());
                    return false;
                }
            }
        }

        // If all failing courses were later passed, return true
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
        if (student.cgpa < 2.0) {
            System.out.println("Student's GPA is less than 2.0 " + student.cgpa);
            return false;
        }
        return true;
    }

    public static boolean canGradeImprovementRaiseCGPA(List<Semester> semesters, List<TableCourse> table1Courses) {
        // Assume CGPA is taken from the last semester's CGPA
        double currentCGPA = Double.parseDouble(semesters.get(semesters.size() - 1).cgpa);
        if (currentCGPA >= 2.00) {
            System.out.println("Student's CGPA is already above 2.0: " + currentCGPA);
            return false; // Already meets or exceeds the threshold
        }

        // Total credits and grade points up to the last semester
        double totalCredits = 0;
        double totalGradePoints = 0;
        for (Semester semester : semesters) {
            for (Course course : semester.courses) {
                boolean containsCode = false;
                for (TableCourse tableCourse : table1Courses) {
                    if (tableCourse.getCode().equals(course.code)) {
                        containsCode = true;
                        break;
                    }
                }
                if (!containsCode && !course.grade.equals("P") && !course.grade.equals("FF")) {
                    double gradePoints = getGradePoints(course.grade);
                    totalCredits += course.credits;
                    totalGradePoints += gradePoints * course.credits;
                }
            }
        }

        // Iterate over each course and simulate incremental grade improvements
        for (Semester semester : semesters) {
            for (Course course : semester.courses) {
                boolean containsCode = false;
                for (TableCourse tableCourse : table1Courses) {
                    if (tableCourse.getCode().equals(course.code)) {
                        containsCode = true;
                        break;
                    }
                }
                if (!course.grade.equals("P") && !course.grade.equals("FF") && !containsCode) {
                    String currentGrade = course.grade;
                    while (!currentGrade.equals("AA")) { // Assuming "AA" is the highest grade
                        currentGrade = getNextHigherGrade(currentGrade);
                        double improvedGradePoints = getGradePoints(currentGrade);
                        double newTotalGradePoints = totalGradePoints - (getGradePoints(course.grade) * course.credits)
                                + (improvedGradePoints * course.credits);
                        double potentialCGPA = newTotalGradePoints / totalCredits;
                        if (potentialCGPA >= 2.00) {
                            return true; // Found a course improvement that can raise CGPA above 2.00
                        }
                    }
                }
            }
        }
        return false;
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

    private static String getNextHigherGrade(String currentGrade) {
        return switch (currentGrade) {
            case "DD" -> "DC";
            case "DC" -> "CC";
            case "CC" -> "CB";
            case "CB" -> "BB";
            case "BB" -> "BA";
            case "BA" -> "AA";
            default -> currentGrade; // No higher grade available or unrecognized grade
        };
    }

    public enum ExamRight {
        EK_SINAV,
        SINIRSIZ_SINAV,
        BELIRLI_DONEM_SINAV_HAKKI,
        HAK_YOK
    }

    public class EligibilityProcess {
        boolean WorLflag;
        boolean internshipFlag;
        boolean allCoursesFlag;
        boolean tableCourseFlag;
        boolean GPAFlag;
        boolean FFgradeFlag;
        boolean maxStudyDurationFlag;
        boolean gradeImprovementFlag;
        ExamRight examRight;
    }

}
