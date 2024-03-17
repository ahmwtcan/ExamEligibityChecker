package com.automation;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;

import com.automation.DataHandler.Course;
import com.automation.DataHandler.Semester;

import java.io.File;
import java.io.IOException;
import java.util.List;

public class PdfParser {

    public static void main(String[] args) {
        String filePath = "C:\\Users\\Lenovo\\Desktop\\test\\modules\\demo\\src\\main\\java\\com\\automation\\transkipt.pdf";
        String trascript = extractTextFromPDF(filePath);

        List<DataHandler.Semester> semesters = DataHandler.parseTranscript(trascript);

        // System.out.println("Transcript: " + trascript);
        // semester count
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

    }

    public static String extractTextFromPDF(String filePath) {
        try (PDDocument document = PDDocument.load(new File(filePath))) {
            if (!document.isEncrypted()) {
                PDFTextStripper stripper = new PDFTextStripper();
                String text = stripper.getText(document);

                return text;
            }
        } catch (IOException e) {

            e.printStackTrace();

        }
        return null;
    }
}
