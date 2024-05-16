package com.automation;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class PdfParser {

    // public static void main(String[] args) {

    // String transs =
    // extractFromResources(TableCourse.class.getResourceAsStream("/Tablo1.pdf"));

    // }

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

    public static String extractFromResources(InputStream pdfStream) {
        try (PDDocument document = PDDocument.load(pdfStream)) {
            PDFTextStripper stripper = new PDFTextStripper();
            String text = stripper.getText(document);
            document.close();
            return text;
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    public static List<TableCourse> parseTableCourses() {
        List<TableCourse> courses = new ArrayList<>();
        InputStream inputStream = TableCourse.class.getResourceAsStream("/Tablo1.pdf");
        String tableText = extractFromResources(inputStream);
        String[] lines = tableText.split("\\r?\\n");
        Pattern coursePattern = Pattern.compile("([A-Z]{2,4}\\s\\d{3,4}\\*?)\\s([A-Za-z0-9\\s&,.:'-]+)");
        Pattern splitPattern = Pattern.compile("(?<=\\*)(?=\\s[A-Z]{2,4}\\s\\d{3,4})");

        for (String line : lines) {
            if (splitPattern.matcher(line).find()) {
                String[] splitCourses = splitPattern.split(line, 2);
                for (String course : splitCourses) {
                    Matcher matcher = coursePattern.matcher(course.trim());
                    if (matcher.find()) {
                        courses.add(new TableCourse(matcher.group(1), matcher.group(2)));
                    }
                }
            } else {
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
