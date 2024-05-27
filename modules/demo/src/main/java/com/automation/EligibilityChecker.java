package com.automation;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

import com.automation.PdfParser.TableCourse;

public class EligibilityChecker {
    EligibilityProcess process = new EligibilityProcess();
    double requiredGPA = 2.0;
    int maxStudyDuration = 14;
    int maxFFCount = 5;
    int maxWLCount = 5;
    int minFFCount = 0;
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

            if (hasExcessiveTotalFailures(student, 6)) {
                process.tableCourseFlag = true;
                process.examRight = ExamRight.ILISIGI_KESILDI;
                return process;
            }
            long ffCount = countFFgrades(student);

            boolean hasFF = checkFFgrades(student, maxFFCount, minFFCount);

            System.out.println("FF Count: " + ffCount);
            System.out.println("Has FF: " + hasFF);

            if (student.gotExamRight) {
                if (!student.gotExamRightAndUsed) {
                    process.examRight = ExamRight.HAK_YOK;
                    process.message += "Student has exceeded the maximum study duration and has not used exam right"
                            + "\n";
                    return process;
                } else if (process.gotExamRightAndUsed) {

                    if (ffCount > maxFFCount) {
                        process.examRight = ExamRight.ILISIGI_KESILDI;
                        process.message += "Student has exceeded the maximum study duration and has more than 5 FF grades"
                                + "\n";
                        return process;
                    }

                }
            }

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

                process.message += "Student has " + ffCount + " FF grades " + "\n";

                process.FFgradeFlag = false;
                process.examRight = ExamRight.SINAV;
                return process;
            } else if (ffCount == 1) {
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

        System.out.println("Check Withdrawals and Leaves: " + countFFgrades(student));

        if (!checkWithdrawalsAndLeaves(student)) {
            process.WorLflag = true;
            process.examRight = ExamRight.HAK_YOK;
            process.message += "Student has a W or L grade without passing subsequently in any course" + "\n";
            return process;

        } else {
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
            process.allCoursesFlag = true;
            process.examRight = ExamRight.HAK_YOK;
            process.message += "Student has not taken all courses" + "\n";
            return process;
        } else {
            process.allCoursesFlag = false;
            process.message += "Student has taken all courses" + "\n";
        }

        if (student.semesterCount < 9) {
            process.examRight = ExamRight.HAK_YOK;
            process.message += "Student has not completed normal study duration " + "\n";
            return process;
        }

        // 5. Step
        long FirstffCount = countFFgrades(student);

        System.out.println("FirstffCount: " + FirstffCount);

        if (FirstffCount == 1) {
            Boolean flag = hasFailedTableCourseWithoutRetake(student);
            if (flag) {
                process.tableCourseFlag = true;
                process.examRight = ExamRight.HAK_YOK;
                process.message += "Student has a FF grade in any table course" + "\n";
                return process;
            }
            process.FFgradeFlag = false;
            process.examRight = ExamRight.EK_SINAV;
            process.message += "Student has only 1 FF grade and have exam right" + "\n";
            return process;
        } else if (FirstffCount > 1) {
            process.FFgradeFlag = true;
            process.message += "Student has more than 1 FF grade" + "\n";
            process.examRight = ExamRight.HAK_YOK;
            return process;
        } else {
            process.FFgradeFlag = false;
            process.message += "Student has no FF grades" + "\n";
        }

        // 6. Step
        if (!checkGPA(student, requiredGPA)) {

            process.GPAFlag = true;

            if (canGradeImprovementRaiseCGPA(student, requiredGPA)) {
                process.gradeImprovementFlag = true;
                process.examRight = ExamRight.EK_SINAV;
                return process;
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
                    return false;
                }
            }
        }

        return true;
    }

    boolean checkMaxStudyDuration(Student student, int threshold) {

        if (student.maxStudyDurationExceeded) {
            process.message += "Student has already exceeded the study duration threshold " + threshold + "\n";
            return true;
        }

        if (student.semesterCount >= threshold) {
            student.maxStudyDurationExceeded = true;
            process.message += "Student has exceeded the study duration threshold " + threshold + "\n";
            return true;
        }
        student.maxStudyDurationExceeded = false;
        process.message += "Student has not exceeded the study duration threshold " + threshold + "\n";
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

        return !grade.matches("^(W|L|NC|FF|FA|I|X|T|ND|ADD|DR|AU).*");
    }

    boolean checkInternship(Student student) {
        String internshipRegex = "([A-Z]{2,3} 400)";
        // Regex to match "P", "P(R)", "P (R)", "P (R)", etc.
        String passedGradeRegex = "P\\s*\\(R\\)|P";

        for (Course course : student.courses) {
            if (course.code.matches(internshipRegex)) {
                if (course.grade.matches(passedGradeRegex)) {
                    process.message += "Student has passed the internship course: " + course.code + "  " + course.grade
                            + "\n";
                    return true;
                } else {
                    process.message += "Student has not passed the internship course: " + course.code + "\n";
                    return false;
                }
            }
        }
        process.message += "Student has not taken the internship course" + "\n";
        return false;
    }

    public boolean isAllCoursesTaken(Student student) {

        if (!student.isAllCoursesTaken) {
            process.message += "Student has not taken all courses" + "\n";
            return false;
        } else {
            process.message += "Student has taken all courses" + "\n";
        }

        return student.isAllCoursesTaken;
    }

    public boolean gotExamRightAndUsed(Student student) {

        if (student.gotExamRightAndUsed) {
            process.message += "Student has exam right and used it" + "\n";
            return student.gotExamRightAndUsed;
        } else {
            process.message += "Student has exam right but did not use it" + "\n";
        }

        return student.gotExamRightAndUsed;
    }

    public boolean gotExamRight(Student student) {

        if (student.gotExamRight) {
            process.message += "Student got exam right before" + "\n";
            return student.gotExamRight;
        } else {
            process.message += "Student does not have exam right before" + "\n";
        }

        return student.gotExamRight;
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
                process.message += "Student failed to retake and pass table course: " + entry.getKey() + "\n";
                return true;
            }
        }

        return false; // No table courses with failed attempts without successful retakes found
    }

    public boolean hasExcessiveTotalFailures(Student student, int maxNumber) {
        // Map to track if the student passed the course after failing or withdrawing.
        HashMap<String, Boolean> passedCourses = new HashMap<>();

        for (Course course : student.courses) {
            for (TableCourse tableCourse : tableCourses) {
                if (course.code.equals(tableCourse.getCode())) {
                    if (isPassingGrade(course.grade)) {
                        passedCourses.put(course.code, true); // Mark that the course was passed eventually.
                    } else if (!isPassingGrade(course.grade)) {
                        passedCourses.putIfAbsent(course.code, false); // Mark as not passed, only if not already
                                                                       // passed.
                    }
                }
            }
        }

        // Count the number of courses that were never passed after failing or
        // withdrawing.
        long failedCoursesCount = passedCourses.values().stream().filter(passed -> !passed).count();

        // Log and return based on the count compared to maxNumber
        if (failedCoursesCount > maxNumber) {
            process.message += "Student has excessive total failures/withdrawals: " + failedCoursesCount + "\n";
            return true;
        }
        System.out
                .println("Student has not exceeded the maximum number of failures/withdrawals: " + failedCoursesCount);
        process.message += "Student has not exceeded the maximum number of failures/withdrawals: " + failedCoursesCount
                + "\n";

        return false;
    }

    public boolean checkFFgrades(Student student, int maxFFCount, int minFFCount) {
        Map<String, String> bestGrades = new HashMap<>();
        Set<String> failingElectives = new HashSet<>();
        Map<String, String> poolMap = new HashMap<>();

        // First pass to collect the best grades and identify failing electives
        for (Course course : student.courses) {
            boolean isFirstAttempt = student.courses.stream()
                    .filter(c -> c.code.equals(course.code))
                    .findFirst().get().equals(course);

            if (isElectiveCourse(course.code) && !isInternshipCourse(course.code)) {
                System.out.println("Elective course: " + course.code + " Grade: " + course.grade + " isFirstAttempt: "
                        + isFirstAttempt);

                if (isFirstAttempt && course.grade.contains("(R)")) {
                    // This course is a retake for a previously failed elective course
                    poolMap.put(course.code, course.grade);
                } else if (course.grade.startsWith("FF") || course.grade.startsWith("FA")) {
                    failingElectives.add(course.code);
                }
                bestGrades.compute(course.code,
                        (key, currentBestGrade) -> currentBestGrade == null
                                || isHigherGrade(course.grade, currentBestGrade)
                                        ? course.grade
                                        : currentBestGrade);
            } else if (!isInternshipCourse(course.code)) {
                bestGrades.compute(course.code,
                        (key, currentBestGrade) -> currentBestGrade == null
                                || isHigherGrade(course.grade, currentBestGrade)
                                        ? course.grade
                                        : currentBestGrade);
            }
        }

        // Collect keys to be removed
        List<String> keysToRemove = new ArrayList<>();
        for (Map.Entry<String, String> entry : poolMap.entrySet()) {
            System.out.println("Checking replacement course: " + entry.getKey());

            boolean hasPassingGrade = bestGrades.entrySet().stream()
                    .anyMatch(e -> !e.getKey().equals(entry.getKey()) && !e.getValue().startsWith("FF")
                            && !e.getValue().startsWith("FA") && poolMap.containsKey(e.getKey()));
            if (!hasPassingGrade) {
                keysToRemove.add(entry.getKey());
            }
        }

        // Remove keys after iteration
        for (String key : keysToRemove) {
            poolMap.remove(key);
        }

        // Calculate the final failing grades set
        Set<String> finalFailedCourses = new HashSet<>(failingElectives);
        for (String courseCode : failingElectives) {
            if (poolMap.containsKey(courseCode) && isElectiveCourse(courseCode)) {
                finalFailedCourses.remove(courseCode);
            }
        }

        // Log best grades
        for (Map.Entry<String, String> entry : bestGrades.entrySet()) {
            System.out.println("Course code: " + entry.getKey() + " Grade: " + entry.getValue());
        }

        // Log final failing grades count
        System.out.println("Final failing grades count: " + (finalFailedCourses.size() - poolMap.size()));

        // Log failed courses
        for (String courseCode : finalFailedCourses) {
            System.out.println("Failed course code: " + courseCode);
        }

        long failingGradesCount = finalFailedCourses.size() - poolMap.size();

        System.out.println("FFFFFFFFFFFFFFF grades count: " + failingGradesCount);

        process.message += "Student has " + failingGradesCount + " failing grades " + "\n";

        // Check if the count of failing grades is within the specified range
        return (failingGradesCount > minFFCount && failingGradesCount <= maxFFCount);
    }

    public long countFFgrades(Student student) {
        Map<String, String> bestGrades = new HashMap<>();
        Set<String> failingElectives = new HashSet<>();
        Map<String, String> poolMap = new HashMap<>();

        // First pass to collect the best grades and identify failing electives
        for (Course course : student.courses) {
            boolean isFirstAttempt = student.courses.stream()
                    .filter(c -> c.code.equals(course.code))
                    .findFirst().get().equals(course);

            if (isElectiveCourse(course.code) && !isInternshipCourse(course.code)) {
                System.out.println("Elective course: " + course.code + " Grade: " + course.grade + " isFirstAttempt: "
                        + isFirstAttempt);

                if (isFirstAttempt && course.grade.contains("(R)")) {
                    // This course is a retake for a previously failed elective course
                    poolMap.put(course.code, course.grade);
                } else if (course.grade.startsWith("FF") || course.grade.startsWith("FA")) {
                    failingElectives.add(course.code);
                }
                bestGrades.compute(course.code,
                        (key, currentBestGrade) -> currentBestGrade == null
                                || isHigherGrade(course.grade, currentBestGrade)
                                        ? course.grade
                                        : currentBestGrade);
            } else if (!isInternshipCourse(course.code)) {
                bestGrades.compute(course.code,
                        (key, currentBestGrade) -> currentBestGrade == null
                                || isHigherGrade(course.grade, currentBestGrade)
                                        ? course.grade
                                        : currentBestGrade);
            }
        }

        // Collect keys to be removed
        List<String> keysToRemove = new ArrayList<>();
        for (Map.Entry<String, String> entry : poolMap.entrySet()) {
            System.out.println("Checking replacement course: " + entry.getKey());

            boolean hasPassingGrade = bestGrades.entrySet().stream()
                    .anyMatch(e -> !e.getKey().equals(entry.getKey()) && !e.getValue().startsWith("FF")
                            && !e.getValue().startsWith("FA") && poolMap.containsKey(e.getKey()));
            if (!hasPassingGrade) {
                keysToRemove.add(entry.getKey());
            }
        }

        // Remove keys after iteration
        for (String key : keysToRemove) {
            poolMap.remove(key);
        }

        // Calculate the final failing grades set
        Set<String> finalFailedCourses = new HashSet<>(failingElectives);
        for (String courseCode : failingElectives) {
            if (poolMap.containsKey(courseCode) && isElectiveCourse(courseCode)) {
                finalFailedCourses.remove(courseCode);
            }
        }

        // Log best grades
        for (Map.Entry<String, String> entry : bestGrades.entrySet()) {
            System.out.println("Course code: " + entry.getKey() + " Grade: " + entry.getValue());
        }

        // Log final failing grades count
        System.out.println("Final failing grades count: " + (finalFailedCourses.size() - poolMap.size()));

        // Log failed courses
        for (String courseCode : finalFailedCourses) {
            System.out.println("Failed course code: " + courseCode);
        }

        return finalFailedCourses.size() - poolMap.size();
    }

    private boolean isInternshipCourse(String code) {
        return code.matches("[A-Z]{2,3} 400");
    }

    private boolean isElectiveCourse(String courseCode) {
        return courseCode.matches("[A-Z]{2,3} [4-9][0-9]{2}");
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

        process.message += "Student has " + wlCount + " courses with only unresolved W or L grades " + "\n";

        return wlCount > minWLCount && wlCount <= maxWLCount;
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

        return wlCount;
    }

    public long countFailedCourses(Student student) {

        return countFFgrades(student) + countWorLgrades(student);
    }

    public boolean checkFailedCourses(Student student, int max, int min) {
        long failedCourses = countFailedCourses(student);

        process.message += "Student has " + failedCourses + " failed courses " + "\n";

        return failedCourses > min && failedCourses <= max;
    }

    private boolean isHigherGrade(String newGrade, String oldGrade) {
        // Normalize grades by removing "(R)" if present
        String normalizedNewGrade = newGrade.replaceAll("\\(R\\)", "").trim();
        String normalizedOldGrade = oldGrade.replaceAll("\\(R\\)", "").trim();

        // The order of grades from best to worst, with 'AA' being the best and 'FF' and
        // 'FA' both being failing grades.
        List<String> gradesOrder = List.of("AA", "BA", "BB", "CB", "CC", "DC", "DD", "FD", "FF", "FA", "W");

        // If the new grade comes before the old grade in this list, it's higher
        // (better).
        return gradesOrder.indexOf(normalizedNewGrade) < gradesOrder.indexOf(normalizedOldGrade);
    }

    public boolean checkGPA(Student student, double requiredGPA) {
        // if student's GPA is less than 2.0, return false
        if (student.cgpa <= requiredGPA) {

            process.message += "Student's GPA is less than " + requiredGPA + " " + student.cgpa + "\n";

            return false;
        }

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

        if (student.totalCredits <= totalCredits) {
            totalCredits = student.totalCredits;
        }

        double currentCGPA = totalCredits > 0 ? totalGradePoints / totalCredits : 0.0;

        System.out.println("Current CGPA: " + currentCGPA);
        System.out.println("Total Credits: " + totalCredits);
        System.out.println("Total Grade Points: " + totalGradePoints);
        System.out.println("Student CGPA: " + student.cgpa);

        if (student.cgpa < currentCGPA) {
            currentCGPA = student.cgpa;
        }

        if (currentCGPA >= requiredCGPA) {
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

                // " + fistGrade + " to "
                // + nextGrade + " raises CGPA from " + df.format(currentCGPA) + " to " +
                // df.format(newCGPA));

                if ((Math.round(newCGPA * 100) / 100.0) >= requiredCGPA) {

                    process.message += "Improvement found on course " + bestAttempt.code + ": " + fistGrade + " to "
                            + nextGrade + " raises CGPA from " + df.format(currentCGPA) + " to " + df.format(newCGPA)
                            + "\n";

                    return true; // Found an improvement that meets the requirement
                }
                currentGrade = nextGrade; // Update to the next higher grade for further checking
            }
        }

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
        SINAV,
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
