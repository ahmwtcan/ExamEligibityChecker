package com.automation;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class PdfParser {

    public static void main(String[] args) {

        List<TableCourse> courses = parseTableCourses();

        System.out.println("Courses: ");
        System.out.println("Course count: " + courses.size());
        for (TableCourse course : courses) {
            System.out.println(course.getCode() + " " + course.getName());
        }
    }

    public static String extractTextFromPDF(String filePath) {
        try (PDDocument document = PDDocument.load(new File(filePath))) {
            if (!document.isEncrypted()) {
                PDFTextStripper stripper = new PDFTextStripper();
                return stripper.getText(document);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    public static List<TableCourse> parseTableCourses() {
        List<TableCourse> courses = new ArrayList<>();
        String filePath = "C:\\Users\\Lenovo\\Desktop\\test\\modules\\demo\\src\\main\\java\\com\\automation\\Tablo1.pdf";
        String tableText = extractTextFromPDF(filePath);
        String[] lines = tableText.split("\\r?\\n"); // Split by new lines
        String currentCode = "";
        String currentName = "";
        for (String line : lines) {
            Matcher matcher = Pattern.compile("([A-Z]{2,4}\\s\\d{3,4})\\s([A-Za-z0-9\\s&,.:'-]+)").matcher(line);
            if (matcher.find()) {
                // If we have a previous course being built, add it before starting a new one
                if (!currentCode.isEmpty()) {
                    courses.add(new TableCourse(currentCode, currentName.trim()));
                    currentCode = ""; // Reset for the next course
                    currentName = "";
                }
                currentCode = matcher.group(1).trim();
                currentName = matcher.group(2).trim();
            } else if (!currentCode.isEmpty()) {
                // If this line doesn't match a new course but we have an ongoing course, append
                // this line
                currentName += " " + line.trim();
            }
        }
        // Don't forget to add the last course if present
        if (!currentCode.isEmpty()) {
            courses.add(new TableCourse(currentCode, currentName.trim()));
        }
        return courses;
    }

    static class TableCourse {
        private String code;
        private String name;

        public TableCourse(String code, String name) {
            this.code = code;
            this.name = name;
        }

        public String getCode() {
            return code;
        }

        public String getName() {
            return name;
        }
    }
}
