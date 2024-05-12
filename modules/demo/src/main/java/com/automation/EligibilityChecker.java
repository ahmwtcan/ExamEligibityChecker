package com.automation;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import com.automation.PdfParser.TableCourse;

public class EligibilityChecker {
    EligibilityProcess process = new EligibilityProcess();
    double requiredGPA = 2.0;
    int maxStudyDuration = 14;
    int maxFFCount = 5;
    int maxWLCount = 5;
    int minFFCount = 1;
    int minWLCount = 1;
    private List<String> nonContributingGrades = List.of("W", "L", "I", "X", "T", "ND", "ADD", "DR", "AU");
    private static final Pattern NON_CONTRIBUTING_PATTERN = Pattern.compile("NC\\([A-Z]+\\)");

    static List<TableCourse> tableCourses = PdfParser.parseTableCourses();

    public EligibilityProcess checkEligibility(Student student) {

        process.message = "";
        // Check the student's eligibility for the exam right

        checkMaxStudyDuration(student, maxStudyDuration);
        // 1. Step
        if (student.maxStudyDurationExceeded) {
            System.out.println("Student has exceeded the maximum study duration");

            if (!hasFailedTableCourseWithoutRetake(student) || !checkWorLgrades(student, maxWLCount, minWLCount)) {
                process.tableCourseFlag = true;
                process.examRight = ExamRight.ILISIGI_KESILDI;
                return process;
            }
            long ffCount = countFFgrades(student);
            System.out.println("Student has " + ffCount + " FF grades");
            if (ffCount > 5) {

                if (process.gotExamRightAndUsed) {
                    process.examRight = ExamRight.ILISIGI_KESILDI;
                    return process;
                }
                process.examRight = ExamRight.EK_SINAV;
                process.message += "Student has more than 5 FF grades with  " + ffCount + "\n";
                process.FFgradeFlag = true;
                return process;
            } else if ((6 > ffCount >> 1)) {
                System.out.println("Student has " + ffCount + " FF grades");

                process.message += "Student has " + ffCount + " FF grades " + "\n";

                process.FFgradeFlag = false;
                process.examRight = ExamRight.EK_SINAV;
                return process;
            } else if (ffCount == 1) {
                System.out.println("Student has 1 FF grade");
                process.FFgradeFlag = false;
                process.message += "Student has 1 FF grade " + "\n";
                process.examRight = ExamRight.SINIRSIZ_SINAV;
                return process;
            }

            // check gpa
            process.GPAFlag = checkGPA(student, requiredGPA);

            if (!process.GPAFlag) {
                process.examRight = ExamRight.SINIRSIZ_SINAV;
                return process;
            } else if (!process.GPAFlag && !hasFailedTableCourseWithoutRetake(student)) {
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
            process.message += "Student has a W or L grade without passing subsequently in any course" + "\n";
            return process;

        } else {
            System.out.println("Student has no W or L grade in any course beside creditless Language courses");
            process.WorLflag = false;
            process.message += "Student has no W or L grade in any course beside creditless Language courses" + "\n";
        }
        // 3. Step
        // if student has an internship course and grade is not P, return false
        if (!checkInternship(student)) {
            process.internshipFlag = true;
            process.examRight = ExamRight.HAK_YOK;
            return process;
        } else {
            process.internshipFlag = false;
        }

        // 4. Step
        if (!student.isAllCoursesTaken) {
            System.out.println("Student has not taken all courses");
            process.allCoursesFlag = true;
            process.examRight = ExamRight.HAK_YOK;
            process.message += "Student has not taken all courses" + "\n";
            return process;
        } else {
            process.allCoursesFlag = false;
            System.out.println("Student has taken all courses");
            process.message += "Student has taken all courses" + "\n";
        }
        // 5. Step
        long FirstffCount = countFFgrades(student);

        System.out.println("Student has " + FirstffCount + " FF grades");

        if (FirstffCount == 1) {
            Boolean flag = hasFailedTableCourseWithoutRetake(student);
            if (flag) {
                System.out.println("Student has a FF grade in any table course");
                process.tableCourseFlag = true;
                process.examRight = ExamRight.HAK_YOK;
                process.message += "Student has a FF grade in any table course" + "\n";
                return process;
            }
            System.out.println("Student has only 1 FF grade");
            process.FFgradeFlag = false;
            process.examRight = ExamRight.EK_SINAV;
            process.message += "Student has only 1 FF grade" + "\n";
            return process;
        } else if (FirstffCount > 1) {
            System.out.println("Student has more than 1 FF grade");
            process.FFgradeFlag = true;
            process.message += "Student has more than 1 FF grade" + "\n";
            process.examRight = ExamRight.HAK_YOK;
            return process;
        } else {
            process.FFgradeFlag = false;
            System.out.println("Student has no FF grades");
            process.message += "Student has no FF grades" + "\n";
        }

        // 6. Step
        if (!checkGPA(student, requiredGPA)) {
            System.out.println("Student's GPA is less than " + requiredGPA);

            process.GPAFlag = true;

            if (canGradeImprovementRaiseCGPA(student, requiredGPA)) {
                System.out.println("Student can improve a grade to raise CGPA above 2.0");
                process.gradeImprovementFlag = true;
                process.examRight = ExamRight.EK_SINAV;
            }
            process.examRight = ExamRight.HAK_YOK;
            return process;
        } else {
            process.GPAFlag = false;
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

    boolean checkMaxStudyDuration(Student student, int threshold) {

        if (student.semesterCount >= threshold) {
            System.out.println("treshold " + (threshold));
            student.maxStudyDurationExceeded = true;
            process.message += "Student has exceeded the  study duration threshold " + threshold + "\n";
            return true;
        }
        student.maxStudyDurationExceeded = false;
        return false;

    }

    private boolean isNonContributingGrade(String grade) {
        // Check if the grade matches any predefined non-contributing grades or follows
        // the pattern NC(XX)
        return nonContributingGrades.contains(grade) || NON_CONTRIBUTING_PATTERN.matcher(grade).matches();
    }

    private static boolean isPassingGrade(String grade) {
        // Exclude non-passing grades, considering additional notes like "(R)"
        // (Repeated) or specific annotations.
        // Adjust the pattern to correctly handle cases such as "FF(R)" or "NC(AA)".

        return !grade.matches("^(W|L|NC|FF|FA|I|X|T|ND|ADD|DR|AU).*");
    }

    boolean checkInternship(Student student) {
        String intershipRegex = "([A-Z]{2,3} 400)";

        for (Course course : student.courses) {
            if (course.code.matches(intershipRegex) && !course.grade.equals("P")) {
                System.out.println("Student has not passed the internship course: " + course.code);
                process.message += "Student has not passed the internship course: " + course.code + "\n";
                return false;
            } else if (course.code.matches(intershipRegex) && course.grade.equals("P")) {
                System.out.println("Student has passed the internship course: " + course.code + "  " + course.grade);
                process.message += "Student has passed the internship course: " + course.code + "  " + course.grade
                        + "\n";
                return true;
            }
        }
        return false;
    }

    // if student has a FF grade in any table course, return false
    // else if student has only 1 FF grade in any other course then he/she should
    // have exam right
    public boolean hasFailedTableCourseWithoutRetake(Student student) {
        // Map to keep track of the grades for each table course code
        HashMap<String, List<String>> tableCourseGrades = new HashMap<>();

        // Populate the map only with grades from table courses
        for (Course course : student.courses) {
            for (TableCourse tableCourse : tableCourses) {
                if (course.code.equals(tableCourse.getCode())) {
                    tableCourseGrades.putIfAbsent(course.code, new ArrayList<>());
                    tableCourseGrades.get(course.code).add(course.grade);
                }
            }
        }

        // Check each table course for failing grades and subsequent retakes
        for (Map.Entry<String, List<String>> entry : tableCourseGrades.entrySet()) {
            boolean foundFailingGrade = false;
            boolean passedAfterFailing = false;

            for (String grade : entry.getValue()) {
                if (grade.equals("FF") || grade.equals("FA")) {
                    foundFailingGrade = true;
                }
                // Check if a passing grade follows after a failing grade
                if (foundFailingGrade && isPassingGrade(grade)) {
                    passedAfterFailing = true;
                    break; // Stop checking once a passing grade is found after failing
                }
            }

            // If there's a failing grade with no subsequent passing grade, return true
            if (foundFailingGrade && !passedAfterFailing) {
                System.out.println("Student failed to retake and pass table course: " + entry.getKey());
                process.message += "Student failed to retake and pass table course: " + entry.getKey() + "\n";
                return true;
            }
        }

        return false; // No table courses with failed attempts without successful retakes found
    }

    public boolean checkFFgrades(Student student, int maxFFCount, int minFFCount) {
        Map<String, String> bestGrades = new HashMap<>();

        for (Course course : student.courses) {
            bestGrades.compute(course.code,
                    (key, currentBestGrade) -> currentBestGrade == null || isHigherGrade(course.grade, currentBestGrade)
                            ? course.grade
                            : currentBestGrade);
        }

        long failingGradesCount = bestGrades.values().stream()
                .filter(grade -> grade.equals("FF") || grade.equals("FA"))
                .count();

        System.out.println("Student has " + failingGradesCount + " courses with FF or FA grades");

        process.message += "Student has " + failingGradesCount + " courses with FF or FA grades" + "\n";

        // Check if the count of failing grades is within the specified range
        return (failingGradesCount >= minFFCount && failingGradesCount < maxFFCount);
    }

    public long countFFgrades(Student student) {
        Map<String, String> bestGrades = new HashMap<>();

        for (Course course : student.courses) {
            bestGrades.compute(course.code,
                    (key, currentBestGrade) -> currentBestGrade == null || isHigherGrade(course.grade, currentBestGrade)
                            ? course.grade
                            : currentBestGrade);
        }

        long failingGradesCount = bestGrades.values().stream()
                .filter(grade -> grade.equals("FF") || grade.equals("FA"))
                .count();

        return failingGradesCount;
    }

    public boolean checkWorLgrades(Student student, int maxWLCount, int minWLCount) {
        Map<String, List<String>> courseGrades = new HashMap<>();

        // Collect all grades for each course
        for (Course course : student.courses) {
            courseGrades.computeIfAbsent(course.code, k -> new ArrayList<>()).add(course.grade);
        }

        // Count the number of courses where the final or highest grade is still W or L,
        // but also check if there is a subsequent passing grade.
        long wlCount = 0;
        for (Map.Entry<String, List<String>> entry : courseGrades.entrySet()) {
            List<String> grades = entry.getValue();
            String highestGrade = grades.stream().reduce("",
                    (max, cur) -> max.isEmpty() || isHigherGrade(cur, max) ? cur : max);

            if ((highestGrade.equals("W") || highestGrade.equals("L")) && !hasSubsequentPassingGrade(grades)) {
                wlCount++;
            }
        }

        System.out.println("Student has " + wlCount + " courses with only unresolved W or L grades ");

        process.message += "Student has " + wlCount + " courses with only unresolved W or L grades " + "\n";

        return wlCount >= minWLCount && wlCount < maxWLCount;
    }

    private static boolean hasSubsequentPassingGrade(List<String> grades) {
        // Check if there's any passing grade after a W or L
        return grades.stream().anyMatch(EligibilityChecker::isPassingGrade);
    }

    public long countWorLgrades(Student student) {
        Map<String, String> highestGrades = new HashMap<>();
        for (Course course : student.courses) {
            highestGrades.merge(course.code, course.grade,
                    (oldGrade, newGrade) -> isHigherGrade(newGrade, oldGrade) ? newGrade : oldGrade);
        }

        long wlCount = highestGrades.values().stream()
                .filter(grade -> grade.equals("W") || grade.equals("L"))
                .count();

        System.out.println("Student has " + wlCount + " courses with only W or L grades");
        return wlCount;
    }

    public long countFailedCourses(Student student) {

        return countFFgrades(student) + countWorLgrades(student);
    }

    public boolean checkFailedCourses(Student student, int max, int min) {
        long failedCourses = countFailedCourses(student);

        System.out.println("Student has " + failedCourses + " failed courses");

        process.message += "Student has " + failedCourses + " failed courses " + "\n";

        return failedCourses >= min && failedCourses <= max;
    }

    private boolean isHigherGrade(String newGrade, String oldGrade) {
        // The order of grades from best to worst, with 'AA' being the best and 'FF' and
        // 'FA' both being failing grades.
        List<String> gradesOrder = List.of("AA", "BA", "BB", "CB", "CC", "DC", "DD", "FD", "FF", "FA");

        // If the new grade comes before the old grade in this list, it's higher
        // (better).
        return gradesOrder.indexOf(newGrade) < gradesOrder.indexOf(oldGrade);
    }

    public boolean checkGPA(Student student, double requiredGPA) {
        // if student's GPA is less than 2.0, return false
        if (student.cgpa <= requiredGPA) {
            System.out.println("Student's GPA is less than " + requiredGPA + student.cgpa);

            process.message += "Student's GPA is less than " + requiredGPA + " " + student.cgpa + "\n";

            return false;
        }

        System.out.println("Student's GPA is above 2.0");
        process.message += "Student's GPA is above " + requiredGPA + "  " + student.cgpa + "\n";
        return true;
    }

    public boolean canGradeImprovementRaiseCGPA(Student student, double requiredCGPA) {
        Map<String, List<Course>> courseAttempts = new HashMap<>();
        Map<String, Course> bestPassedAttempts = new HashMap<>();
        double totalCredits = 0;
        double totalGradePoints = 0;
        DecimalFormat df = new DecimalFormat("#.00");

        // Collect all attempts for each course
        for (Semester semester : student.semesters) {
            for (Course course : semester.courses) {
                courseAttempts.putIfAbsent(course.code, new ArrayList<>());
                courseAttempts.get(course.code).add(course);
            }
        }

        // Determine the best attempt that contributes to CGPA for each course
        for (Map.Entry<String, List<Course>> entry : courseAttempts.entrySet()) {
            Course bestAttempt = determineBestAttemptConsideringRetakes(entry.getValue());
            if (bestAttempt != null && !isNonContributingGrade(bestAttempt.grade)) {
                Course previousBest = bestPassedAttempts.get(entry.getKey());
                if (previousBest == null || getGradePoints(bestAttempt.grade) > getGradePoints(previousBest.grade)) {
                    bestPassedAttempts.put(entry.getKey(), bestAttempt);
                }
            }
        }

        // Calculate total credits and grade points from the best attempts
        for (Course course : bestPassedAttempts.values()) {
            totalCredits += course.credits;
            totalGradePoints += getGradePoints(course.grade) * course.credits;
        }

        System.out.println("Total credits: " + totalCredits);
        System.out.println("Total grade points: " + totalGradePoints);

        if (student.totalCredits <= totalCredits) {
            totalCredits = student.totalCredits;
        }

        System.out.println("Total grade points    asasdasd: " + totalCredits);

        double currentCGPA = totalCredits > 0 ? totalGradePoints / totalCredits : 0.0;
        System.out.println("Calculated CGPA: " + df.format(currentCGPA));

        if (currentCGPA >= requiredCGPA) {
            System.out.println("Current CGPA meets or exceeds the required CGPA.");
            process.message += "Current CGPA " + currentCGPA + " meets or exceeds the required CGPA " + requiredCGPA
                    + "\n";
            return false; // No need for improvements if the required CGPA is already met.
        }

        // Check potential improvements for each course
        for (Course bestAttempt : bestPassedAttempts.values()) {
            double originalGradePoints = getGradePoints(bestAttempt.grade) * bestAttempt.credits;
            String currentGrade = bestAttempt.grade;
            String fistGrade = bestAttempt.grade;
            boolean isTableCourse = tableCourses.stream().anyMatch(course -> course.code.equals(bestAttempt.code));

            while (!currentGrade.equals("AA") && !isTableCourse) {
                String nextGrade = getNextHigherGrade(currentGrade);
                if (nextGrade == null || nextGrade.equals(currentGrade)) {
                    break;
                }

                double nextGradePoints = getGradePoints(nextGrade) * bestAttempt.credits;
                double newTotalGradePoints = totalGradePoints - originalGradePoints + nextGradePoints;
                double newCGPA = newTotalGradePoints / totalCredits;

                System.out.println("Checking improvement for course " + bestAttempt.code + ": " + currentGrade + " to "
                        + nextGrade + " raises CGPA from " + df.format(currentCGPA) + " to " + df.format(newCGPA));

                if (newCGPA >= requiredCGPA) {
                    System.out.println("Improvement found that raises CGPA above required level.");

                    process.message += "Improvement found on course " + bestAttempt.code + ": " + fistGrade + " to "
                            + nextGrade + " raises CGPA from " + df.format(currentCGPA) + " to " + df.format(newCGPA)
                            + "\n";

                    System.out.println("Improvement found on course " + bestAttempt.code + ": " + fistGrade + " to "
                            + nextGrade + " raises CGPA from " + df.format(currentCGPA) + " to " + df.format(newCGPA));

                    return true; // Found an improvement that meets the requirement
                }
                currentGrade = nextGrade; // Update to the next higher grade for further checking
            }
        }

        System.out.println("No single-course improvement found that can raise CGPA to required level.");
        process.message += "No single-course improvement found that can raise CGPA to required level." + "\n";
        return false; // No possible grade improvement found that meets the required CGPA
    }

    private Course determineBestAttemptConsideringRetakes(List<Course> attempts) {
        Course bestAttempt = null;
        for (Course attempt : attempts) {
            if (bestAttempt == null || getGradePoints(attempt.grade) > getGradePoints(bestAttempt.grade)) {
                bestAttempt = attempt;
            }
        }
        return bestAttempt;
    }

    public static String getNextHigherGrade(String currentGrade) {
        currentGrade = currentGrade.replace("(R)", "").trim(); // Remove the repeat notation for grade comparison

        switch (currentGrade) {
            case "DD":
                return "DC";
            case "DC":
                return "CC";
            case "CC":
                return "CB";
            case "CB":
                return "BB";
            case "BB":
                return "BA";
            case "BA":
                return "AA";
            default:
                return null; // Return null if "AA" or an unhandled grade
        }
    }

    public static double getGradePoints(String grade) {
        grade = grade.replace("(R)", "").trim(); // Remove the repeat notation for grade comparison

        switch (grade) {
            case "AA":
                return 4.0;
            case "BA":
                return 3.5;
            case "BB":
                return 3.0;
            case "CB":
                return 2.5;
            case "CC":
                return 2.0;
            case "DC":
                return 1.5;
            case "DD":
                return 1.0;
            case "FF":
                return 0.0;
            case "FA":
                return 0.0; // Assuming FA is treated like FF
            default:
                return 0.0; // Non-credit and other non-standard grades assumed to contribute no points
        }
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
        String message;

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
            this.message = "";
        }
    }

}