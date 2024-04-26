package com.automation;

import javax.swing.*;

import java.awt.*;
import java.io.File;

public class TranscriptApp extends JFrame {
    private JButton uploadButton;
    private JButton checkButton;
    private JLabel pdfNameLabel;
    private JLabel nameLabel;
    private JTextField statusTextField;
    private JFileChooser fileChooser;

    public TranscriptApp() {
        setTitle("Exam Eligibility Checker");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout()); // Main layout

        // top panel for greeting
        JPanel topPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JLabel greetingLabel = new JLabel("Welcome to the Exam Eligibility Checker");
        nameLabel = new JLabel("Name: ");
        nameLabel.setVisible(false);
        topPanel.add(greetingLabel);
        topPanel.add(nameLabel);
        add(topPanel, BorderLayout.CENTER);

        // Center Panel for status display
        JPanel centerPanel = new JPanel();
        centerPanel.setLayout(new BoxLayout(centerPanel, BoxLayout.Y_AXIS));
        pdfNameLabel = new JLabel("PDF Name: [none]");
        centerPanel.add(pdfNameLabel);
        add(centerPanel, BorderLayout.CENTER);

        // Right Panel for checkboxes
        JPanel rightPanel = new JPanel();
        rightPanel.setLayout(new BoxLayout(rightPanel, BoxLayout.Y_AXIS));
        statusTextField = new JTextField("Exam Status");
        statusTextField.setEditable(false);
        rightPanel.add(statusTextField);
        rightPanel.setVisible(true);
        add(rightPanel, BorderLayout.EAST);

        // Bottom Panel for buttons
        JPanel bottomPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        uploadButton = new JButton("Upload Transcript");
        checkButton = new JButton("Check Eligibility");
        bottomPanel.add(uploadButton);
        bottomPanel.add(checkButton);
        add(bottomPanel, BorderLayout.SOUTH);

        setSize(600, 500);
        setLocationRelativeTo(null); // Center on screen

        addListeners();
    }

    private void addListeners() {
        uploadButton.addActionListener(e -> {
            // File chooser for selecting the transcript
            fileChooser = new JFileChooser();
            fileChooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
            fileChooser.setFileFilter(new javax.swing.filechooser.FileNameExtensionFilter("PDF files", "pdf"));

            int returnValue = fileChooser.showOpenDialog(TranscriptApp.this);
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
                    statusTextField.setText("Eligible for the exam");
                } else {
                    statusTextField.setText("Not eligible for the exam");
                }

            }
        });

        checkButton.addActionListener(e -> {
            // Here you would use the parsed transcript to check eligibility
            boolean isEligible = true; // This would be determined by your actual checking logic
            if (isEligible) {
                JOptionPane.showMessageDialog(TranscriptApp.this, "Eligible for the exam");

            } else {
                JOptionPane.showMessageDialog(TranscriptApp.this, "Not eligible for the exam");
            }
        });
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            TranscriptApp app = new TranscriptApp();
            app.setVisible(true);
        });
    }
}
