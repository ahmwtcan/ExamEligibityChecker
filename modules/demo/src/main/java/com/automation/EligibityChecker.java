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

        // 1. Step
        if (student.maxStudyDurationExceeded) {
            System.out.println("Student has exceeded the maximum study duration");
            // if student has a FF grade in any table course or has a W or L grade in more
            // than 5 courses, return false

            if (!checkTableCourses(student) || !checkWorLgrades(student)) {
                process.tableCourseFlag = true;
                process.examRight = ExamRight.ILISIGI_KESILDI;
                return process;
            }
            long ffCount = checkFFgrades(student);
            System.out.println("Student has " + ffCount + " FF grades");
            if (ffCount > 5) {

                if (process.gotExamRightAndUsed) {
                    process.examRight = ExamRight.ILISIGI_KESILDI;
                    return process;
                }
                process.examRight = ExamRight.EK_SINAV;
                System.out.println("Student has more than 5 FF grades");
                process.FFgradeFlag = true;
                return process;
            } else if ((6 > ffCount >> 1)) {
                System.out.println("Student has " + ffCount + " FF grades");
                process.FFgradeFlag = false;
                process.examRight = ExamRight.EK_SINAV;
                return process;
            } else if (ffCount == 1) {
                System.out.println("Student has 1 FF grade");
                process.FFgradeFlag = false;
                process.examRight = ExamRight.SINIRSIZ_SINAV;
                return process;
            }

            // check gpa
            process.GPAFlag = checkGPA(student);

            if (!process.GPAFlag) {
                process.examRight = ExamRight.SINIRSIZ_SINAV;
                return process;
            } else if (!process.GPAFlag && !checkTableCourses(student)) {
                process.examRight = ExamRight.SINIRSIZ_SINAV;
                return process;
            }

        }
        // 2.Step
        // if student has a W OR L Grade in any course beside creditless Language
        // courses, return false
        if (!checkWithdrawalsAndLeaves(student)) {
            process.WorLflag = true;
            process.examRight = ExamRight.ILISIGI_KESILDI;
        } else {
            System.out.println("Student has no W or L grade in any course beside creditless Language courses");
        }
        // 3. Step
        // if student has an internship course and grade is not P, return false
        if (!checkInternship(student)) {
            System.out.println("Student has not passed the internship course");
            process.internshipFlag = true;
        } else {
            process.internshipFlag = false;
            System.out.println("Student has passed the internship course");
        }

        // 4. Step
        if (!student.isAllCoursesTaken) {
            System.out.println("Student has not taken all courses");
            process.allCoursesFlag = true;
        } else {
            process.allCoursesFlag = false;
            System.out.println("Student has taken all courses");
        }
        // 5. Step
        long FirstffCount = checkFFgrades(student);
        if (FirstffCount == 1) {
            Boolean flag = checkTableCourses(student);
            if (flag) {
                System.out.println("Student has a FF grade in any table course");
                process.tableCourseFlag = true;
                process.examRight = ExamRight.HAK_YOK;
                return process;
            }
            System.out.println("Student has only 1 FF grade");
            process.FFgradeFlag = false;
            process.examRight = ExamRight.EK_SINAV;
            return process;
        } else if (FirstffCount > 1) {
            System.out.println("Student has more than 1 FF grade");
            process.FFgradeFlag = true;
            process.examRight = ExamRight.HAK_YOK;
            return process;
        } else {
            process.FFgradeFlag = false;
        }

        // 6. Step
        if (!checkGPA(student)) {
            System.out.println("Student's GPA is less than 2.0");
            process.GPAFlag = true;

            if (canGradeImprovementRaiseCGPA(student.semesters, tableCourses)) {
                System.out.println("Student can improve a grade to raise CGPA above 2.0");
                process.gradeImprovementFlag = true;
                process.examRight = ExamRight.EK_SINAV;
            }
            process.examRight = ExamRight.HAK_YOK;
            return process;
        } else {
            process.GPAFlag = false;
            System.out.println("Student's GPA is above 2.0");
            process.examRight = ExamRight.HAK_YOK;
            return process;
        }

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

    public long checkFFgrades(Student student) {
        // Map to store the best grade per course.
        Map<String, String> bestGrades = new HashMap<>();

        // Iterate over courses to populate the map with the best grade per course.
        for (Course course : student.courses) {
            bestGrades.compute(course.code, (key, currentBestGrade) -> {
                if (currentBestGrade == null || isHigherGrade(course.grade, currentBestGrade)) {
                    return course.grade; // If it's the first grade or higher than the current, store it.
                } else {
                    return currentBestGrade; // Otherwise, keep the current best grade.
                }
            });
        }

        // Count the number of courses where the best grade is still a failing grade
        // ('FF' or 'FA').
        return bestGrades.values().stream()
                .filter(grade -> grade.equals("FF") || grade.equals("FA"))
                .count();
    }

    public boolean checkWorLgrades(Student student) {
        // Create a map to keep track of the highest grade for each course
        Map<String, String> highestGrades = new HashMap<>();
        for (Course course : student.courses) {
            highestGrades.merge(course.code, course.grade,
                    (oldGrade, newGrade) -> isHigherGrade(newGrade, oldGrade) ? newGrade : oldGrade);
        }

        // Count courses with only 'W' or 'L' as the highest grade
        long wlCount = highestGrades.values().stream().filter(grade -> grade.equals("W") || grade.equals("L")).count();

        System.out.println("Student has " + wlCount + " courses with only W or L grades");
        return wlCount <= 5;
    }

    private boolean isHigherGrade(String newGrade, String oldGrade) {
        // The order of grades from best to worst, with 'AA' being the best and 'FF' and
        // 'FA' both being failing grades.
        List<String> gradesOrder = List.of("AA", "BA", "BB", "CB", "CC", "DC", "DD", "FD", "FF", "FA");

        // If the new grade comes before the old grade in this list, it's higher
        // (better).
        return gradesOrder.indexOf(newGrade) < gradesOrder.indexOf(oldGrade);
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
        HAK_YOK,
        DERSE_DEVAM,
        ILISIGI_KESILDI
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
        boolean gotExamRight;
        boolean gotExamRightAndUsed;
        ExamRight examRight;

        public EligibilityProcess() {
            this.WorLflag = false;
            this.internshipFlag = false;
            this.allCoursesFlag = false;
            this.tableCourseFlag = false;
            this.GPAFlag = false;
            this.FFgradeFlag = false;
            this.maxStudyDurationFlag = false;
            this.gradeImprovementFlag = false;
            this.gotExamRight = false;
            this.gotExamRightAndUsed = false;
            this.examRight = ExamRight.BELIRLI_DONEM_SINAV_HAKKI;
        }
    }

}
