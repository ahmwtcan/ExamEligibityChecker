package com.automation;

import javax.swing.*;

import com.automation.EligibityChecker.EligibilityProcess;

import java.util.List;
import java.awt.*;
import java.io.File;

public class GUI1 extends JFrame {

    private JButton uploadButton, checkButton;
    private JLabel pdfNameLabel, nameLabel, statusLabel;
    private JFileChooser fileChooser;
    private JProgressBar progressBar;
    private EligibityChecker eligibilityChecker = new EligibityChecker();
    private Student currentStudent;
    private JPanel flagsPanel;

    public GUI1() {
        initializeUI();
    }

    private void initializeUI() {
        setTitle("Exam Eligibility Checker");
        setSize(800, 400); // Adjusted width to prevent overlap
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        getContentPane().setLayout(new BorderLayout()); // Set BorderLayout for the content pane
        // Menu Bar
        JMenuBar menuBar = new JMenuBar();
        JMenu helpMenu = new JMenu("Help");
        JMenuItem aboutItem = new JMenuItem("About");
        aboutItem.addActionListener(e -> showAboutDialog());
        helpMenu.add(aboutItem);
        menuBar.add(helpMenu);
        setJMenuBar(menuBar); // Add the menu bar to the frame

        JPanel topPanel = new JPanel();
        topPanel.setLayout(new BoxLayout(topPanel, BoxLayout.Y_AXIS));
        JLabel greetingLabel = new JLabel("Welcome to the Exam Eligibility Checker");
        greetingLabel.setFont(new Font("Arial", Font.BOLD, 16));
        topPanel.add(greetingLabel);

        JLabel explanationLabel = new JLabel(
                "<html><div style='width: 400px;'>" + // Set a preferred width for the label
                        "<p>This application is in development state . "
                        +
                        "For final desicion please contact with deanery.</p>" +
                        "</div></html>");
        explanationLabel.setFont(new Font("Arial", Font.PLAIN, 12));
        topPanel.add(explanationLabel);
        getContentPane().add(topPanel, BorderLayout.NORTH);

        // Center content
        JPanel centerPanel = new JPanel(new GridBagLayout());
        GridBagConstraints c = new GridBagConstraints();
        c.fill = GridBagConstraints.HORIZONTAL;
        c.weightx = 1;
        c.insets = new Insets(10, 10, 10, 10);

        // Add components to the center panel...
        // Remember to adjust GridBagConstraints for each component
        pdfNameLabel = new JLabel("Loaded: [none]");
        c.gridy = 0;
        centerPanel.add(pdfNameLabel, c);

        nameLabel = new JLabel(" ");
        nameLabel.setVisible(false);
        c.gridy = 1;
        centerPanel.add(nameLabel, c);

        statusLabel = new JLabel("Exam Status: ");
        statusLabel.setFont(new Font("Arial", Font.BOLD, 14));
        c.gridy = 2;
        centerPanel.add(statusLabel, c);

        progressBar = new JProgressBar(0, 100);
        progressBar.setVisible(false);
        c.gridy = 3;
        centerPanel.add(progressBar, c);

        getContentPane().add(centerPanel, BorderLayout.CENTER);

        // Eligibility flags panel (East content)
        flagsPanel = new JPanel(new GridLayout(8, 1)); // Assuming 8 different flags
        flagsPanel.setBorder(BorderFactory.createTitledBorder("Eligibility Flags"));
        addFlagsToPanel(); // Initialize with pending labels
        getContentPane().add(flagsPanel, BorderLayout.EAST);

        // South content (buttons)
        JPanel bottomPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        uploadButton = new JButton("Upload Transcript");
        checkButton = new JButton("Check Eligibility");
        nameLabel.setVisible(false);
        uploadButton.addActionListener(e -> uploadAction());
        checkButton.addActionListener(e -> checkEligibilityAction());

        bottomPanel.add(uploadButton);
        bottomPanel.add(checkButton);
        getContentPane().add(bottomPanel, BorderLayout.SOUTH);

        setLocationRelativeTo(null); // Center on screen
        setVisible(true);
    }

    private void showAboutDialog() {
        // The content of the About dialog
        String aboutText = "<html><body><h1>Exam Eligibility Checker</h1>"
                + "<p>This application checks if a student is eligible for the exam "
                + "based on their academic transcript.</p>"
                + "<p>Version: 1.0</p>"
                + "<p>Author: fuzulia</p>"
                + "<p>© 2024 Yeditepe University</p>"
                + "</body></html>";

        // Create and display the dialog
        JOptionPane.showMessageDialog(this, aboutText, "About Exam Eligibility Checker",
                JOptionPane.INFORMATION_MESSAGE);
    }

    private void uploadAction() {
        fileChooser = new JFileChooser();
        fileChooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
        fileChooser.setFileFilter(new javax.swing.filechooser.FileNameExtensionFilter("PDF files", "pdf"));

        // clear previous data
        nameLabel.setVisible(false);
        flagsPanel.removeAll();
        statusLabel.setText("Exam Status: ");
        addFlagsToPanel();
        flagsPanel.revalidate();
        flagsPanel.repaint();

        int returnValue = fileChooser.showOpenDialog(GUI1.this);
        if (returnValue == JFileChooser.APPROVE_OPTION) {
            File selectedFile = fileChooser.getSelectedFile();
            pdfNameLabel.setText("Loaded: " + selectedFile.getName());

            String transcript = PdfParser.extractTextFromPDF(selectedFile.getAbsolutePath());

            // Get student from transcript (replace with your logic)
            Student student = DataHandler.getStudent(transcript);

            // iff student is null, show error message
            if (student == null) {
                JOptionPane.showMessageDialog(this, "Failed to parse the transcript. Check the format and try again.",
                        "Error", JOptionPane.ERROR_MESSAGE);
                return;
            }

            System.out.println(student.courses.size());
            int semesterCount = DataHandler.countSemester(student.semesters);
            System.out.println("Semester count: " + semesterCount);

            if (semesterCount < 14) {
                student.maxStudyDurationExceeded = false;
            } else {
                student.maxStudyDurationExceeded = true;
            }

            // // for (Semester semester : student.semesters) {
            // // System.out.println(semester.semesterName + " " + semester.cgpa + " " +
            // // semester.gpa + " "
            // // + semester.completedCredits + " " + semester.totalCredits + "\n");
            // // for (Course course : semester.courses) {
            // // System.out.println(course.code + " " + course.name + " " + course.grade +
            // " "
            // // + course.credits);
            // // }

            // System.out.println("\n");
            // }

            nameLabel.setText("Student Name: " + student.name);
            nameLabel.setVisible(true);

            currentStudent = student;

        }
    }

    private void checkEligibilityAction() {

        if (currentStudent == null) {
            JOptionPane.showMessageDialog(this, "Please upload a transcript first.", "Error",
                    JOptionPane.ERROR_MESSAGE);
            return;
        }
        // Ask pre-eligibility questions
        boolean questionsPassed = askPreEligibilityQuestions();
        if (!questionsPassed) {
            JOptionPane.showMessageDialog(this, "Based on your answers, you are not eligible.", "Not Eligible",
                    JOptionPane.WARNING_MESSAGE);
            return; // Stop further processing if not eligible
        }

        progressBar.setVisible(true);
        progressBar.setValue(0);

        SwingWorker<EligibilityProcess, Integer> worker = new SwingWorker<EligibilityProcess, Integer>() {
            @Override
            protected EligibilityProcess doInBackground() throws Exception {

                statusLabel.setText("Checking eligibility...");
                System.out.println("Checking eligibility for student: " + currentStudent);

                EligibilityProcess process = eligibilityChecker.checkEligibility(currentStudent);

                for (int i = 1; i <= 100; i++) {
                    publish(i); // Publish progress updates
                    Thread.sleep(10); // Simulate some work
                }
                statusLabel.setText("Done checking eligibility.");

                System.out.println("Eligibility process: ");
                System.out.println("WorLflag: " + process.WorLflag);
                System.out.println("internshipFlag: " + process.internshipFlag);
                System.out.println("allCoursesFlag: " + process.allCoursesFlag);
                System.out.println("tableCourseFlag: " + process.tableCourseFlag);
                System.out.println("GPAFlag: " + process.GPAFlag);
                System.out.println("gradeImprovementFlag: " + process.gradeImprovementFlag);
                System.out.println("maxStudyDurationFlag: " + process.maxStudyDurationFlag);
                System.out.println("FFgradeFlag: " + process.FFgradeFlag);
                System.out.println("examRight: " + process.examRight);

                return process;
            }

            @Override
            protected void process(List<Integer> chunks) {
                int progress = chunks.get(chunks.size() - 1); // Get the last value
                progressBar.setValue(progress); // Update the progress bar
            }

            @Override
            protected void done() {
                try {
                    EligibilityProcess process = get(); // Get the eligibility result
                    updateFlagsPanel(process); // Update the flags panel based on the process results

                    statusLabel.setText("Exam Status: " + (process.examRight));

                } catch (Exception e) {
                    e.printStackTrace();
                } finally {
                    progressBar.setValue(100);
                    progressBar.setVisible(false);
                }
            }

        };
        worker.execute();
    }

    private boolean askPreEligibilityQuestions() {
        // Array to store the questions
        String[] questions = {
                "Have you taken all the required courses at least once (pass/fail)?",
                "Have you been given the right to take additional exams before?",
                "Did you use your given exam rights?"
        };

        // Loop over the questions and ask them one by one
        for (String question : questions) {
            int answer = JOptionPane.showConfirmDialog(
                    this,
                    question,
                    "Eligibility Pre-Check",
                    JOptionPane.YES_NO_OPTION,
                    JOptionPane.QUESTION_MESSAGE);

            if (question.equals(questions[1]) && answer == JOptionPane.NO_OPTION) {
                return true;
            }

            if (question.equals(questions[2]) && answer == JOptionPane.YES_OPTION) {
                currentStudent.gotExamRightAndUsed = true;
                return true;
            } else if (question.equals(questions[2]) && answer == JOptionPane.NO_OPTION) {
                currentStudent.gotExamRightAndUsed = false;
                return false;
            }

            if (answer == JOptionPane.NO_OPTION) {
                return false;
            }
        }

        // If all answers were YES, return true
        return true;
    }

    private void updateFlagsPanel(EligibilityProcess process) {
        // Clear existing flags
        flagsPanel.removeAll();

        if (process.WorLflag) {
            flagsPanel.add(new JLabel(formatFlag("Withdrawals or Leaves issue", true)));
        }
        if (process.internshipFlag) {
            flagsPanel.add(new JLabel(formatFlag("Internship requirement not met", true)));
        }
        if (process.allCoursesFlag) {
            flagsPanel.add(new JLabel(formatFlag("Not all courses taken", true)));
        }
        if (process.tableCourseFlag) {
            flagsPanel.add(new JLabel(formatFlag("Failed course with no Exam Right", true)));
        }
        if (process.GPAFlag) {
            flagsPanel.add(new JLabel(formatFlag("GPA is below 2.00", true)));
        }
        if (process.gradeImprovementFlag) {
            flagsPanel.add(new JLabel(formatFlag("Grade improvement required", true)));
        }
        if (process.maxStudyDurationFlag) {
            flagsPanel.add(new JLabel(formatFlag("Maximum study duration reached", true)));
        }
        if (process.FFgradeFlag) {
            flagsPanel.add(new JLabel(formatFlag("More than 5 FF grades", true)));
        }

        // Ensure the panel updates to display the new components
        flagsPanel.revalidate();
        flagsPanel.repaint();
    }

    private String formatFlag(String label, boolean isIssue) {
        return label + ": " + (isIssue ? "X" : "✓");
    }

    private void addFlagsToPanel() {
        flagsPanel.add(new JLabel("Withdrawals and Leaves: Pending"));
        flagsPanel.add(new JLabel("Internship: Pending"));
        flagsPanel.add(new JLabel("All Courses Taken: Pending"));
        flagsPanel.add(new JLabel("Table Course: Pending"));
        flagsPanel.add(new JLabel("GPA: Pending"));
        flagsPanel.add(new JLabel("Max Study Duration: Pending"));
        flagsPanel.add(new JLabel("FF Grades: Pending"));
        flagsPanel.add(new JLabel("Grade Improvement: Pending")).setVisible(false);

    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new GUI1().setVisible(true));
    }
}
