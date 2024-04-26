package com.automation;

import javax.swing.*;

import com.automation.EligibityChecker.EligibilityProcess;

import java.util.List;
import java.awt.*;
import java.io.File;

public class test2 extends JFrame {

    private JButton uploadButton, checkButton;
    private JLabel pdfNameLabel, nameLabel, statusLabel;
    private JFileChooser fileChooser;
    private JProgressBar progressBar;
    private EligibityChecker eligibilityChecker = new EligibityChecker();
    private Student currentStudent;
    private JPanel flagsPanel;

    public test2() {
        initializeUI();
    }

    private void initializeUI() {
        setTitle("Exam Eligibility Checker");
        setSize(600, 400);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new GridBagLayout());
        GridBagConstraints c = new GridBagConstraints();
        c.fill = GridBagConstraints.HORIZONTAL;
        c.weightx = 1;
        c.insets = new Insets(10, 10, 10, 10);

        // Top Panel
        JLabel greetingLabel = new JLabel("Welcome to the Exam Eligibility Checker");
        greetingLabel.setFont(new Font("Arial", Font.BOLD, 16));
        c.gridy = 0;
        add(greetingLabel, c);

        // Center Panel
        pdfNameLabel = new JLabel("Loaded: [none]");
        c.gridy = 1;
        add(pdfNameLabel, c);

        nameLabel = new JLabel(" ");
        nameLabel.setVisible(false);
        c.gridy = 2;
        add(nameLabel, c);

        statusLabel = new JLabel("Exam Status: ");
        statusLabel.setFont(new Font("Arial", Font.BOLD, 14));
        c.gridy = 3;
        add(statusLabel, c);

        progressBar = new JProgressBar(0, 100);
        progressBar.setVisible(false);
        c.gridy = 4;
        add(progressBar, c);
        // Right Panel for Eligibility Flags
        flagsPanel = new JPanel(new GridLayout(8, 1)); // Assuming 8 different flags
        flagsPanel.setBorder(BorderFactory.createTitledBorder("Eligibility Flags"));
        addFlagsToPanel(); // Add flag labels to panel initially marked as pending

        c.gridx = 1; // Position to the right
        c.gridy = 0;
        c.gridheight = GridBagConstraints.REMAINDER;
        c.fill = GridBagConstraints.VERTICAL;
        add(flagsPanel, c);

        setLocationRelativeTo(null);
        setVisible(true);

        // Bottom Panel
        JPanel buttonPanel = new JPanel();
        uploadButton = new JButton("Upload Transcript", new ImageIcon("logo.png"));
        uploadButton.addActionListener(e -> uploadAction());
        checkButton = new JButton("Check Eligibility", new ImageIcon("logo.png"));
        checkButton.addActionListener(e -> checkEligibilityAction());
        buttonPanel.add(uploadButton);
        buttonPanel.add(checkButton);
        c.gridy = 5;
        add(buttonPanel, c);

        setLocationRelativeTo(null);
        setVisible(true);
    }

    private void uploadAction() {
        fileChooser = new JFileChooser();
        fileChooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
        fileChooser.setFileFilter(new javax.swing.filechooser.FileNameExtensionFilter("PDF files", "pdf"));

        int returnValue = fileChooser.showOpenDialog(test2.this);
        if (returnValue == JFileChooser.APPROVE_OPTION) {
            File selectedFile = fileChooser.getSelectedFile();
            pdfNameLabel.setText("Loaded: " + selectedFile.getName());

            String transcript = PdfParser.extractTextFromPDF(selectedFile.getAbsolutePath());

            System.out.println(transcript);

            // Get student from transcript (replace with your logic)
            Student student = DataHandler.getStudent(transcript);
            System.out.println(student.courses.size());

            for (Semester semester : student.semesters) {
                System.out.println(semester.semesterName + " " + semester.cgpa + " " +
                        semester.gpa + " "
                        + semester.completedCredits + " " + semester.totalCredits + "\n");
                for (Course course : semester.courses) {
                    System.out.println(course.code + " " + course.name + " " + course.grade + " "
                            + course.credits);
                }

                System.out.println("\n");
            }

            nameLabel.setText("Student Name: " + student.name);
            nameLabel.setVisible(true);

            currentStudent = student;

        }
    }

    private void checkEligibilityAction() {
        progressBar.setVisible(true);
        progressBar.setValue(0);

        SwingWorker<EligibilityProcess, Integer> worker = new SwingWorker<EligibilityProcess, Integer>() {
            @Override
            protected EligibilityProcess doInBackground() throws Exception {

                statusLabel.setText("Checking eligibility...");
                EligibilityProcess process = eligibilityChecker.checkEligibility(currentStudent);

                for (int i = 1; i <= 100; i++) {
                    publish(i); // Publish progress updates
                    Thread.sleep(10); // Simulate some work
                }
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

    private void updateFlagsPanel(EligibilityProcess process) {
        ((JLabel) flagsPanel.getComponent(0)).setText(formatFlag("Withdrawals and Leaves", process.WorLflag));
        ((JLabel) flagsPanel.getComponent(1)).setText(formatFlag("Internship", process.internshipFlag));
        ((JLabel) flagsPanel.getComponent(2)).setText(formatFlag("All Courses Taken", process.allCoursesFlag));
        ((JLabel) flagsPanel.getComponent(3)).setText(formatFlag("Table Course", process.tableCourseFlag));
        ((JLabel) flagsPanel.getComponent(4)).setText(formatFlag("GPA", process.GPAFlag));
        ((JLabel) flagsPanel.getComponent(5)).setText(formatFlag("Grade Improvement", process.gradeImprovementFlag));
        ((JLabel) flagsPanel.getComponent(6)).setText(formatFlag("Max Study Duration", process.maxStudyDurationFlag));
        ((JLabel) flagsPanel.getComponent(7)).setText(formatFlag("FF Grades", process.FFgradeFlag));
    }

    private String formatFlag(String label, boolean flag) {
        return label + ": " + (flag ? "✓" : "X");
    }

    private void addFlagsToPanel() {
        flagsPanel.add(new JLabel("Withdrawals and Leaves: Pending"));
        flagsPanel.add(new JLabel("Internship: Pending"));
        flagsPanel.add(new JLabel("All Courses Taken: Pending"));
        flagsPanel.add(new JLabel("Table Course: Pending"));
        flagsPanel.add(new JLabel("GPA: Pending"));
        flagsPanel.add(new JLabel("Grade Improvement: Pending"));
        flagsPanel.add(new JLabel("Max Study Duration: Pending"));
        flagsPanel.add(new JLabel("FF Grades: Pending"));
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new test2().setVisible(true));
    }
}
