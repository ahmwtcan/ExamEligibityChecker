package com.automation;

import javax.swing.*;
import javax.swing.filechooser.FileSystemView;
import com.automation.DataHandler.Course;
import com.automation.DataHandler.Semester;

import java.awt.*;
import java.util.List;

public class TranscriptApp extends JFrame {
    private JTextArea textArea; // Add this line
    JScrollPane scrollPane = new JScrollPane(textArea); // Add scrolling capability
    JFileChooser j = new JFileChooser(FileSystemView.getFileSystemView());
    ImageIcon logoIcon = new ImageIcon("logo.png");
    JLabel logoLabel = new JLabel(logoIcon);

    public TranscriptApp() {
        // Frame settings
        setTitle("Exam Eligibility Checker");
        setSize(500, 400); // Adjusted for better visibility
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        // Use a different layout that allows more flexibility
        setLayout(new BorderLayout());

        // Initialize the JTextArea
        textArea = new JTextArea(15, 30); // Adjust size as needed
        textArea.setEditable(false); // Make the text area read-only
        // You can create scrollPane here with the initialized textArea
        scrollPane = new JScrollPane(textArea); // Add scrolling capability

        logoIcon = new ImageIcon(getClass().getResource("logo.png"));
        logoLabel = new JLabel(logoIcon);
        logoLabel.setSize(10, 10);

        // Add components to the pane
        JPanel topPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        topPanel.add(logoLabel);
        add(topPanel, BorderLayout.NORTH);

        JPanel centerPanel = new JPanel();
        centerPanel.add(scrollPane); // Add the JScrollPane, which contains the JTextArea
        add(centerPanel, BorderLayout.CENTER);

        JPanel bottomPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        JButton uploadButton = new JButton("Upload Transcript");
        JButton checkButton = new JButton("Check Eligibility");
        // Upload button and Check Eligibility button go here...
        bottomPanel.add(uploadButton);
        bottomPanel.add(checkButton);
        add(bottomPanel, BorderLayout.SOUTH);

        // Upload button

        uploadButton.addActionListener(e -> {
            int r = j.showOpenDialog(null);
            if (r == JFileChooser.APPROVE_OPTION) {
                String filePath = j.getSelectedFile().getAbsolutePath();
                String transcript = PdfParser.extractTextFromPDF(filePath);

                List<Semester> semesters = DataHandler.parseTranscript(transcript);

                System.out.println("Transcript: " + transcript);
                // Clear previous text
                textArea.setText("");

                textArea.append("Semester count: " + semesters.size() + "\n\n");
                System.out.println("Semester count " + semesters.size());

                for (Semester semester : semesters) {
                    System.out.println("Semester: " + semester.semesterName);
                    System.out.println("CGPA: " + semester.cgpa + ", GPA: " + semester.gpa + ", Completed Credits: "
                            + semester.completedCredits + ", Total Credits: " + semester.totalCredits);
                    for (Course course : semester.courses) {
                        System.out.println(course);
                    }
                    System.out.println();
                }
                for (Semester semester : semesters) {
                    textArea.append("Semester: " + semester.semesterName + "\n");
                    textArea.append("CGPA: " + semester.cgpa + ", GPA: " + semester.gpa + ", Completed Credits: "
                            + semester.completedCredits + ", Total Credits: " + semester.totalCredits + "\n");
                    for (Course course : semester.courses) {
                        textArea.append(course.toString() + "\n");
                    }
                    textArea.append("\n");
                }

                scrollPane.getVerticalScrollBar().setValue(0); // Scroll to the top
                scrollPane.add(textArea); // Add the JTextArea to the JScrollPane (not the frame
                                          // directly)

            }
        });
        add(uploadButton);

        add(textArea); // Add the JTextArea to the frame (not the JScrollPane, which contains the
                       // JTextArea)

        // Check eligibility button
        JLabel resultLabel = new JLabel(" ");
        checkButton.addActionListener(e -> {
            // Implement eligibility checking logic here
            resultLabel.setText("Eligible/Not Eligible");
        });
        add(checkButton);

        // Result label
        add(resultLabel);
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            TranscriptApp app = new TranscriptApp();
            app.setVisible(true);
        });
    }
}
