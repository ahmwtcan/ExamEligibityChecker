package com.automation;

import javax.swing.*;
import javax.swing.filechooser.FileSystemView;
import com.automation.DataHandler.Course;
import com.automation.DataHandler.Semester;

import java.awt.*;
import java.io.File;
import java.util.List;

public class TranscriptApp extends JFrame {

    private JTextArea textArea;
    private JFileChooser fileChooser;

    public TranscriptApp() {
        initializeComponents();
        layoutComponents();
        addListeners();
    }

    private void initializeComponents() {
        setTitle("Exam Eligibility Checker");
        setSize(500, 400);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        textArea = new JTextArea(15, 30);
        textArea.setEditable(false);

        fileChooser = new JFileChooser(FileSystemView.getFileSystemView());

    }

    private void layoutComponents() {
        // Layout Manager
        setLayout(new BorderLayout());
        // Inside your TranscriptApp constructor or initialization method
        try {
            // Replace "/path/to/logo.png" with the actual path to your logo image
            ImageIcon imgIcon = new ImageIcon(getClass().getResource("logo.png"));
            Image img = imgIcon.getImage();
            this.setIconImage(img);
        } catch (Exception ex) {
            ex.printStackTrace();
            // Handle the error gracefully, maybe log it or show a default icon
        }
        // Text area in the center with scroll
        JScrollPane scrollPane = new JScrollPane(textArea);
        add(scrollPane, BorderLayout.CENTER);

        // Buttons at the bottom
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        JButton uploadButton = new JButton("Upload Transcript");
        JButton checkButton = new JButton("Check Eligibility");
        buttonPanel.add(uploadButton);
        buttonPanel.add(checkButton);
        add(buttonPanel, BorderLayout.SOUTH);
        uploadButton.addActionListener(e -> {
            int returnValue = fileChooser.showOpenDialog(null);
            if (returnValue == JFileChooser.APPROVE_OPTION) {
                File selectedFile = fileChooser.getSelectedFile();
                System.out.println(selectedFile.getAbsolutePath());
            }
        });

        checkButton.addActionListener(e -> {
            // String transcript = textArea.getText();
            // List<Semester> semesters = DataHandler.parseTranscript(transcript);
            // EligibityChecker checker = new EligibityChecker();
            boolean isEligible = true;
            if (isEligible) {
                JOptionPane.showMessageDialog(null, "Eligible for the exam");
            } else {
                JOptionPane.showMessageDialog(null, "Not eligible for the exam");
            }
        });
    }

    private void addListeners() {

        fileChooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
        fileChooser.setAcceptAllFileFilterUsed(false);
        fileChooser.setFileFilter(new javax.swing.filechooser.FileNameExtensionFilter("PDF files", "pdf"));

        fileChooser.addActionListener(e -> {
            if (e.getActionCommand().equals(JFileChooser.APPROVE_SELECTION)) {
                String filePath = fileChooser.getSelectedFile().getAbsolutePath();
                String transcript = PdfParser.extractTextFromPDF(filePath);
                List<Semester> semesters = DataHandler.parseTranscript(transcript);
                ;
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
