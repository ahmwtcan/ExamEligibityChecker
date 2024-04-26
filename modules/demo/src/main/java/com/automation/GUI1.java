package com.automation;

import javax.swing.*;
import java.awt.*;
import java.io.File;

public class GUI1 extends JFrame {

    private JButton uploadButton;
    private JButton checkButton;
    private JLabel pdfNameLabel;
    private JLabel nameLabel;
    private JLabel statusTextArea;
    private JFileChooser fileChooser;

    public GUI1() {
        initializeUI();
    }

    private void initializeUI() {
        setTitle("Exam Eligibility Checker");
        setSize(600, 400);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout(10, 10)); // 10-pixel horizontal and vertical gaps

        add(createTopPanel(), BorderLayout.NORTH);
        add(createCenterPanel(), BorderLayout.CENTER);
        add(createBottomPanel(), BorderLayout.SOUTH);

        fileChooser = new JFileChooser();
        uploadButton.addActionListener(e -> uploadAction());
        checkButton.addActionListener(e -> checkEligibilityAction());

        setLocationRelativeTo(null); // Center on screen
        setVisible(true);
    }

    private JPanel createTopPanel() {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JLabel greetingLabel = new JLabel("Welcome to the Exam Eligibility Checker");
        nameLabel = new JLabel("Name: ");
        nameLabel.setVisible(false);
        panel.add(greetingLabel);
        panel.add(nameLabel);
        return panel;
    }

    private JPanel createCenterPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        pdfNameLabel = new JLabel("PDF Name: [none]");
        statusTextArea = new JLabel("Exam Status [none]");
        statusTextArea.setVisible(true);
        panel.add(statusTextArea, BorderLayout.CENTER);
        panel.add(pdfNameLabel, BorderLayout.PAGE_START);
        return panel;
    }

    private JPanel createBottomPanel() {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        uploadButton = new JButton("Upload Transcript");
        checkButton = new JButton("Check Eligibility");
        panel.add(uploadButton);
        panel.add(checkButton);
        return panel;
    }

    private void uploadAction() {
        fileChooser = new JFileChooser();
        fileChooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
        fileChooser.setFileFilter(new javax.swing.filechooser.FileNameExtensionFilter("PDF files", "pdf"));

        int returnValue = fileChooser.showOpenDialog(GUI1.this);
        if (returnValue == JFileChooser.APPROVE_OPTION) {
            File selectedFile = fileChooser.getSelectedFile();
            pdfNameLabel.setText("Loaded: " + selectedFile.getName());

            String transcript = PdfParser.extractTextFromPDF(selectedFile.getAbsolutePath());

            // get student from transcript
            Student student = DataHandler.getStudent(transcript);
            nameLabel.setText(" " + student.name);
            nameLabel.setVisible(true);
            System.out.println(student);

            // Here you would use the parsed transcript to check eligibility
            boolean isEligible = true; // This would be determined by your actual checking logic

            if (isEligible) {
                statusTextArea.setText("Eligible for the exam");
            } else {
                statusTextArea.setText("Not eligible for the exam");
            }

        }
    }

    private void checkEligibilityAction() {
        // Add eligibility checking logic here
        statusTextArea.setText("Checking eligibility...");
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new GUI1().setVisible(true));
    }
}
