package com.automation;

import javax.swing.*;

import com.automation.EligibilityChecker.EligibilityProcess;

import java.awt.*;
import java.awt.event.ActionEvent;
import java.util.List;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Paths;

public class GUI1 extends JFrame {

    private JButton uploadButton, checkButton, viewLegacyLogButton, viewConfiguredLogButton;
    private JLabel pdfNameLabel, nameLabel, statusLabel, configStatusLabel, rulesStatusLabel;
    private JFileChooser fileChooser;
    private JProgressBar progressBar;
    private EligibilityChecker eligibilityChecker = new EligibilityChecker();
    private Student currentStudent;
    private JPanel examLogPanel;
    private String rulesJson;
    private String result;
    private String configuredLogs;
    private String legacyLogs;
    private JPanel configuredExamLogPanel;

    public GUI1() {
        initializeUI();
    }

    private void initializeUI() {
        setTitle("Exam Eligibility Checker");
        setSize(800, 600);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        getContentPane().setLayout(new BorderLayout());

        // Menu Bar
        JMenuBar menuBar = new JMenuBar();
        JMenu helpMenu = new JMenu("Help");
        JMenuItem aboutItem = new JMenuItem("About");
        aboutItem.addActionListener(e -> showAboutDialog());
        helpMenu.add(aboutItem);
        menuBar.add(helpMenu);
        JMenu fileMenu = new JMenu("File");
        JMenuItem uploadJsonItem = new JMenuItem("Upload Rules JSON");
        uploadJsonItem.addActionListener(this::uploadJsonAction);
        fileMenu.add(uploadJsonItem);
        menuBar.add(fileMenu);
        setJMenuBar(menuBar);
        JPanel topPanel = new JPanel();
        topPanel.setLayout(new BoxLayout(topPanel, BoxLayout.Y_AXIS));
        JLabel greetingLabel = new JLabel("Welcome to the Exam Eligibility Checker");
        greetingLabel.setFont(new Font("Arial", Font.BOLD, 16));
        greetingLabel.setAlignmentX(Component.CENTER_ALIGNMENT); // Center the label

        configStatusLabel = new JLabel("No configuration loaded.");
        configStatusLabel.setFont(new Font("Arial", Font.BOLD, 14));
        configStatusLabel.setAlignmentX(Component.CENTER_ALIGNMENT); // Center the label

        // Adding space between the configuration status label and the explanation label
        topPanel.add(greetingLabel);
        topPanel.add(Box.createVerticalStrut(20)); // Add space
        topPanel.add(configStatusLabel);
        topPanel.add(Box.createVerticalStrut(10)); // Add space

        JLabel explanationLabel = new JLabel(
                "<html><div style='width: 400px;'>" +
                        "<p>This application is in development state. For final decision please contact with deanery.</p>"
                        +
                        "</div></html>");
        explanationLabel.setFont(new Font("Arial", Font.BOLD, 12));
        explanationLabel.setAlignmentX(Component.CENTER_ALIGNMENT); // Center the label

        topPanel.add(explanationLabel);
        getContentPane().add(topPanel, BorderLayout.NORTH);

        // Center content
        JPanel centerPanel = new JPanel(new GridBagLayout());
        GridBagConstraints c = new GridBagConstraints();
        c.fill = GridBagConstraints.HORIZONTAL;
        c.weightx = 1;
        c.insets = new Insets(10, 10, 10, 10);

        // Add components to the center panel...
        pdfNameLabel = new JLabel("Loaded Transcript: [none]");
        c.gridy = 0;
        centerPanel.add(pdfNameLabel, c);

        nameLabel = new JLabel(" ");
        nameLabel.setVisible(false);
        c.gridy = 1;
        centerPanel.add(nameLabel, c);

        statusLabel = new JLabel("Legacy Exam Status: ");
        statusLabel.setFont(new Font("Arial", Font.BOLD, 14));
        c.gridy = 2;
        centerPanel.add(statusLabel, c);

        rulesStatusLabel = new JLabel("Configured Exam Status: ");
        rulesStatusLabel.setFont(new Font("Arial", Font.BOLD, 14));
        c.gridy = 3;
        centerPanel.add(rulesStatusLabel, c);

        progressBar = new JProgressBar(0, 100);
        progressBar.setVisible(false);
        c.gridy = 4;
        centerPanel.add(progressBar, c);

        getContentPane().add(centerPanel, BorderLayout.CENTER);

        // Add a new JPanel for Configured Exam Logs
        configuredExamLogPanel = new JPanel(new GridLayout(8, 1));
        configuredExamLogPanel.setBorder(BorderFactory.createTitledBorder("Configured Exam Logs"));
        getContentPane().add(configuredExamLogPanel, BorderLayout.WEST);
        // Eligibility flags panel (East content)
        examLogPanel = new JPanel(new GridLayout(8, 1));
        examLogPanel.setBorder(BorderFactory.createTitledBorder("Eligibility Logs"));
        addFlagsToPanel();
        getContentPane().add(examLogPanel, BorderLayout.EAST);

        // South content (buttons)
        JPanel bottomPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        uploadButton = new JButton("Upload Transcript");
        checkButton = new JButton("Check Eligibility");
        nameLabel.setVisible(false);
        uploadButton.addActionListener(e -> uploadAction());
        checkButton.addActionListener(e -> {
            try {
                checkEligibilityAction();
            } catch (Exception e1) {
                e1.printStackTrace();
            }
        });

        viewLegacyLogButton = new JButton("View Legacy Log");
        viewLegacyLogButton.setEnabled(false); // Initially disable the button
        viewLegacyLogButton.addActionListener(e -> showLegacyLogWindow());

        viewConfiguredLogButton = new JButton("View Configured Log");
        viewConfiguredLogButton.setEnabled(false); // Initially disable the button
        viewConfiguredLogButton.addActionListener(e -> showConfiguredLogWindow());

        bottomPanel.add(uploadButton);
        bottomPanel.add(checkButton);
        bottomPanel.add(viewLegacyLogButton);
        bottomPanel.add(viewConfiguredLogButton);
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

    private void showLegacyLogWindow() {
        if ("".equals(statusLabel.getText().trim())) {
            JOptionPane.showMessageDialog(this, "No legacy eligibility check performed yet.", "Information",
                    JOptionPane.INFORMATION_MESSAGE);
            return;
        }

        JFrame logWindow = new JFrame("Legacy Eligibility Log");
        JTextArea logTextArea = new JTextArea(legacyLogs);
        logTextArea.setEditable(false);
        logWindow.getContentPane().add(new JScrollPane(logTextArea), BorderLayout.CENTER);
        logWindow.setSize(400, 300);
        logWindow.setLocationRelativeTo(this);
        logWindow.setVisible(true);
    }

    private void showConfiguredLogWindow() {
        if (configuredLogs == null) {
            JOptionPane.showMessageDialog(this, "No configured eligibility check performed yet.", "Information",
                    JOptionPane.INFORMATION_MESSAGE);
            return;
        }

        JFrame logWindow = new JFrame("Configured Eligibility Log");
        JTextArea logTextArea = new JTextArea(configuredLogs);
        logTextArea.setEditable(false);
        logWindow.getContentPane().add(new JScrollPane(logTextArea), BorderLayout.CENTER);
        logWindow.setSize(400, 300);
        logWindow.setLocationRelativeTo(this);
        logWindow.setVisible(true);
    }

    private void uploadJsonAction(ActionEvent event) {
        fileChooser = new JFileChooser();
        fileChooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
        fileChooser.setFileFilter(new javax.swing.filechooser.FileNameExtensionFilter("JSON files", "json"));

        int returnValue = fileChooser.showOpenDialog(GUI1.this);
        if (returnValue == JFileChooser.APPROVE_OPTION) {
            File selectedFile = fileChooser.getSelectedFile();
            try {
                rulesJson = new String(Files.readAllBytes(selectedFile.toPath()));
                configStatusLabel.setText("Configuration loaded: " + selectedFile.getName());

            } catch (Exception e) {
                configStatusLabel.setText("Failed to load configuration.");
                JOptionPane.showMessageDialog(this, "Failed to load the JSON file.", "Error",
                        JOptionPane.ERROR_MESSAGE);
                e.printStackTrace();
            }
        }
    }

    private void uploadAction() {
        fileChooser = new JFileChooser();
        fileChooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
        fileChooser.setFileFilter(new javax.swing.filechooser.FileNameExtensionFilter("PDF files", "pdf"));

        // clear previous data
        pdfNameLabel.setText("Loaded: [none]");
        examLogPanel.removeAll();
        nameLabel.setVisible(false);
        statusLabel.setText("Legacy Exam Status: ");
        viewConfiguredLogButton.setEnabled(false);
        viewLegacyLogButton.setEnabled(false);

        addFlagsToPanel();
        rulesStatusLabel.setText("Configured Exam Status: ");

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
            System.out.println("Semester count: " + student.semesterCount);

            // for (Semester semester : student.semesters) {
            // System.out.println(semester.semesterName + " " + semester.cgpa + " " +
            // semester.gpa + " "
            // + semester.completedCredits + " " + semester.totalCredits + "\n");
            // for (Course course : semester.courses) {
            // System.out.println(course.code + " " + course.name + " " + course.grade +
            // " "
            // + course.credits + " " + course.gradePoints + "\n");
            // }

            // System.out.println("\n");
            // }

            nameLabel.setText("Student Name: " + student.name);
            nameLabel.setVisible(true);

            currentStudent = student;

        }
    }

    private void checkEligibilityAction() throws Exception {

        if (currentStudent == null) {
            JOptionPane.showMessageDialog(this, "Please upload a transcript first.", "Error",
                    JOptionPane.ERROR_MESSAGE);
            return;
        }

        Integer questionsPassed = askPreEligibilityQuestions();
        if (questionsPassed == null) {
            return;
        }

        // rulesJson = loadRulesJson();

        progressBar.setVisible(true);
        progressBar.setValue(0);

        System.out.println("STUDENNNTTTTT: " + currentStudent);

        if (rulesJson != null) {
            eligibilityChecker.process.message = "";

            // Initialize the rule interpreter with the loaded rules and eligibility checker
            RuleInterpreter ruleInterpreter = new RuleInterpreter(rulesJson, eligibilityChecker);
            String ruleResults = ruleInterpreter.evaluateRules(currentStudent);

            System.out.println("Rule results: ");
            System.out.println(ruleResults);

            System.out.println("Configured Logs : ");
            configuredLogs = eligibilityChecker.process.message;
            eligibilityChecker.process.message = "";
            System.out.println(configuredLogs);

            // Extract final decision from rule results
            String[] lines = ruleResults.split("\n");
            String finalDecision = lines[lines.length - 1];
            result = finalDecision.split(":")[1].trim();

            System.out.println("Final Decision: " + result);

            configuredLogs += "\n\n" + result;

        }

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
                    // updateexamLogPanel(process); // Update the flags panel based on the process
                    // results

                    legacyLogs = process.message;

                    statusLabel.setText("Legacy Exam Status: " + (process.examRight));

                    if (result != null) {
                        rulesStatusLabel.setText("Configured Exam Status: " + result);
                    }

                    legacyLogs += "\n\n" + process.examRight;

                    System.out.println("Process Eaxm: " + process.examRight);
                    // Enable view buttons after successful check
                    viewLegacyLogButton.setEnabled(true);
                    viewConfiguredLogButton.setEnabled(true);

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

    private String loadRulesJson() throws Exception {

        String jsonText = new String(Files.readAllBytes(Paths
                .get("C:\\Users\\Lenovo\\Desktop\\test\\modules\\demo\\src\\main\\java\\com\\automation\\genelRule.json")));
        return jsonText;

    }

    private Integer askPreEligibilityQuestions() {
        // Array to store the questions
        String[] questions = {
                "Have you taken all the required courses at least once (pass/fail)?",
                "Have you been given the right to take additional exams before?",
                "Did you use your given exam rights?"
        };

        // Loop over the questions and ask them one by one
        for (String question : questions) {
            // Create a JOptionPane with Yes, No, and Cancel options
            JOptionPane optionPane = new JOptionPane(
                    question,
                    JOptionPane.QUESTION_MESSAGE,
                    JOptionPane.YES_NO_CANCEL_OPTION);

            // Create a JDialog and add the JOptionPane to it
            JDialog dialog = optionPane.createDialog(this, "Eligibility Pre-Check");
            dialog.setDefaultCloseOperation(JDialog.DO_NOTHING_ON_CLOSE);
            dialog.setVisible(true);

            // Get the user's answer
            Integer answer = (Integer) optionPane.getValue();

            // If the user clicked the close button or the cancel button, do nothing
            if (answer == null || answer == JOptionPane.CANCEL_OPTION || answer == JOptionPane.CLOSED_OPTION) {
                return null;
            }
            if (question.equals(questions[0]) && answer == JOptionPane.YES_OPTION) {
                currentStudent.isAllCoursesTaken = true;
            }
            if (question.equals(questions[0]) && answer == JOptionPane.NO_OPTION) {
                currentStudent.isAllCoursesTaken = false;
            }

            if (question.equals(questions[1]) && answer == JOptionPane.NO_OPTION) {
                currentStudent.gotExamRight = false;
                return 1;
            }
            if (question.equals(questions[1]) && answer == JOptionPane.YES_OPTION) {
                currentStudent.gotExamRight = true;
            }

            if (question.equals(questions[2]) && answer == JOptionPane.YES_OPTION) {
                currentStudent.gotExamRightAndUsed = true;
                return 1;
            } else if (question.equals(questions[2]) && answer == JOptionPane.NO_OPTION) {
                currentStudent.gotExamRightAndUsed = false;
                return 1;
            }

        }

        // If all answers were YES, return true
        return 1;
    }

    private void addFlagsToPanel() {
        examLogPanel.add(new JLabel("Withdrawals and Leaves"));
        examLogPanel.add(new JLabel("Internship"));
        examLogPanel.add(new JLabel("All Courses Taken"));
        examLogPanel.add(new JLabel("Table Course"));
        examLogPanel.add(new JLabel("GPA"));
        examLogPanel.add(new JLabel("Max Study Duration"));
        examLogPanel.add(new JLabel("FF Grades"));
        examLogPanel.add(new JLabel("Grade Improvement"));

    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new GUI1().setVisible(true));
    }
}
