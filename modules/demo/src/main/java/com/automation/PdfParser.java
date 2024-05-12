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

        String transs = extractTextFromPDF(
                "C:\\Users\\Lenovo\\Desktop\\test\\modules\\demo\\src\\main\\java\\com\\automation\\transkipt.pdf");

        System.out.println(transs);
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
        Pattern coursePattern = Pattern.compile("([A-Z]{2,4}\\s\\d{3,4}\\*?)\\s([A-Za-z0-9\\s&,.:'-]+)");
        Pattern splitPattern = Pattern.compile("(?<=\\*)(?=\\s[A-Z]{2,4}\\s\\d{3,4})");

        for (String line : lines) {
            // Check if line contains a course code immediately following an asterisk
            if (splitPattern.matcher(line).find()) {
                // Split the line into two assuming only one such pattern exists per line
                String[] splitCourses = splitPattern.split(line, 2);
                for (String course : splitCourses) {
                    Matcher matcher = coursePattern.matcher(course.trim());
                    if (matcher.find()) {
                        courses.add(new TableCourse(matcher.group(1), matcher.group(2)));
                    }
                }
            } else {
                // Regular course parsing
                Matcher matcher = coursePattern.matcher(line);
                if (matcher.find()) {
                    courses.add(new TableCourse(matcher.group(1), matcher.group(2)));
                }
            }
        }
        return courses;
    }

    static class TableCourse {
        String code;
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
