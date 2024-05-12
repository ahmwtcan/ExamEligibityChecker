package com.automation;

import org.json.JSONArray;
import org.json.JSONObject;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class RuleInterpreter {
    private static Student student;
    private static EligibilityChecker checker;
    private JSONArray rules;

    public RuleInterpreter(String ruleData, EligibilityChecker checker) {
        this.rules = new JSONArray(ruleData);
        RuleInterpreter.checker = checker;
    }

    private static Map<String, JSONObject> buildNodeMap(JSONArray rules) {
        Map<String, JSONObject> nodeMap = new HashMap<>();
        Set<String> startNodeIds = new HashSet<>();
        for (int i = 0; i < rules.length(); i++) {
            JSONObject rule = rules.getJSONObject(i);
            JSONObject startNode = rule.getJSONObject("startNode");
            JSONObject endNode = rule.getJSONObject("endNode");

            // Add startNode to map
            nodeMap.put(startNode.getString("id"), new JSONObject()
                    .put("startNode", startNode)
                    .put("endNode", endNode));
            startNodeIds.add(startNode.getString("id"));

            // Check if the endNode is also a startNode
            if (!startNodeIds.contains(endNode.getString("id"))) {
                // Add endNode as a terminal node if it's not a startNode elsewhere
                nodeMap.putIfAbsent(endNode.getString("id"), new JSONObject()
                        .put("startNode", endNode));
            }
        }
        return nodeMap;
    }

    private static String generateIfElse(StringBuilder result, String nodeId, Map<String, JSONObject> nodeMap,
            Map<String, JSONObject> connectionsMap, String indent, Student student) {
        JSONObject currentNode = nodeMap.get(nodeId);
        if (currentNode == null) {
            // New addition: Check if nodeId is a terminal node from connectionsMap and
            // return its text
            JSONObject terminalNode = connectionsMap.get(nodeId);
            if (terminalNode != null) {
                result.append(indent).append("Terminal node reached: ").append(terminalNode.getString("text"))
                        .append(".\n");
                return terminalNode.getString("text");
            } else {
                result.append(indent).append("No more nodes to evaluate or invalid node ID: ").append(nodeId)
                        .append(".\n");
                return "Invalid node ID: " + nodeId; // Graceful error handling
            }
        }

        JSONObject startNode = currentNode.getJSONObject("startNode");
        boolean condition = evaluateCondition(startNode, student, checker);

        result.append(indent).append("Evaluating: ").append(startNode.getString("text")).append(" -> ")
                .append(condition ? "True" : "False").append("\n");

        JSONObject nextNode = condition ? connectionsMap.get(startNode.getString("id") + true)
                : connectionsMap.get(startNode.getString("id") + false);

        if (nextNode != null) {
            String nextNodeId = nextNode.getString("id");
            result.append(indent).append(condition ? "True branch: " : "False branch: ")
                    .append(nextNode.getString("text")).append("\n");
            return generateIfElse(result, nextNodeId, nodeMap, connectionsMap, indent + "    ", student);
        } else {
            result.append(indent).append("End of path reached at node ID: ").append(nodeId).append("\n");
            return startNode.getString("text"); // Return the text of the final node if no further nodes are found.
        }
    }

    public String evaluateRules(Student student) {
        Map<String, JSONObject> nodeMap = buildNodeMap(this.rules);
        Map<String, JSONObject> connectionsMap = buildConnectionsMap(this.rules);
        String startNodeId = findStartNodeId(nodeMap);
        StringBuilder result = new StringBuilder();
        String finalNodeText = generateIfElse(result, startNodeId, nodeMap, connectionsMap, "", student);
        result.append("Final Decision Node Text: ").append(finalNodeText).append("\n");

        return result.toString();
    }

    private static String findStartNodeId(Map<String, JSONObject> nodeMap) {
        for (Map.Entry<String, JSONObject> entry : nodeMap.entrySet()) {
            JSONObject node = entry.getValue().getJSONObject("startNode");
            if (node.getString("text").equals("Start")) {
                return node.getString("id");
            }
        }
        return "0"; // Return a default value or handle it as error
    }

    private static Map<String, JSONObject> buildConnectionsMap(JSONArray rules) {
        Map<String, JSONObject> connectionsMap = new HashMap<>();
        for (int i = 0; i < rules.length(); i++) {
            JSONObject rule = rules.getJSONObject(i);
            JSONObject startNode = rule.getJSONObject("startNode");
            JSONObject endNode = rule.getJSONObject("endNode");
            connectionsMap.put(startNode.getString("id") + rule.getBoolean("type"), endNode);
        }
        return connectionsMap;
    }

    private static boolean evaluateCondition(JSONObject startNode, Student student2, EligibilityChecker checker2) {
        String text = startNode.getString("text");
        if (text.startsWith("CGPA >=")) {
            double requiredGPA = Double.parseDouble(text.substring(8).trim());
            return checker2.checkGPA(student2, requiredGPA);
        } else if (text.startsWith("Study Duration >=")) {
            int maxDuration = Integer.parseInt(text.substring(18).trim());
            System.out.println("maxDuration: " + maxDuration);
            return checker2.checkMaxStudyDuration(student2, maxDuration);
        } else if (text.startsWith("FF_COUNT")) {
            String details = text.substring(text.indexOf("(") + 1, text.indexOf(")"));
            String[] parts = details.split("<=");
            int maxFFAllowed = Integer.parseInt(parts[1].trim());
            int minFFAllowed = Integer.parseInt(parts[0].trim());
            boolean tableCourseEnabled = text.contains("T.Course: Enabled");
            if (tableCourseEnabled)
                return checker2.hasFailedTableCourseWithoutRetake(student2);
            else
                return checker2.checkFFgrades(student2, maxFFAllowed, minFFAllowed);
        } else if (text.startsWith("WrL_COUNT")) {
            String details = text.substring(text.indexOf("(") + 1, text.indexOf(")"));
            String[] parts = details.split("<=");
            int maxAllowed = Integer.parseInt(parts[1].trim());
            int minAllowed = Integer.parseInt(parts[0].trim());
            return checker2.checkWorLgrades(student2, maxAllowed, minAllowed);
        } else if (text.matches(".*\\bXX400\\b.*")) {
            return checker2.checkInternship(student2);
        } else if (text.startsWith("FAILED_COUNT")) {
            String details = text.substring(text.indexOf("(") + 1, text.indexOf(")"));
            String[] parts = details.split("<=");
            int maxAllowed = Integer.parseInt(parts[1].trim());
            int minAllowed = Integer.parseInt(parts[0].trim());
            return checker2.checkFailedCourses(student2, maxAllowed, minAllowed);
        } else if (text.contains("not yükseltirsem") || text.matches(".*\\d+\\.\\d{2}\\+.*")) {
            Matcher gpaMatcher = Pattern.compile("(\\d+\\.\\d+)\\+").matcher(text);
            if (gpaMatcher.find()) {
                double requiredGPA = Double.parseDouble(gpaMatcher.group(1));
                return checker2.canGradeImprovementRaiseCGPA(student2, requiredGPA);
            }
        } else if (text.matches(".*(bütün dersleri).*")
                && (text.matches(".*(başarılı|başarısız).*") || text.matches(".*(hepsinin).*"))) {
            return checker2.isAllCoursesTaken(student2);
        } else if (text.matches(".*(hakkımı|kullandım).*")) {
            return checker2.gotExamRightAndUsed(student2);
        } else if (text.matches(
                ".*(hakkımda).*(ek\\s*sınav|ek_sınav|ek\\s*sınavları|ek\\s*imtihan|derse\\s*bir\\s*defa\\s*devam).*((kararı\\s*alınmıştı)|(kararı\\s*alındı)).*")) {
            return checker2.gotExamRight(student2);
        }

        // Default condition if text is not recognized
        return true;

    }

    private static String generateCondition(JSONObject node, Student student, EligibilityChecker checker) {
        String text = node.getString("text");
        if (text.startsWith("CGPA >=")) {
            double requiredGPA = Double.parseDouble(text.substring(8).trim());
            return "checker.checkGPA(student, " + requiredGPA + ")";
        } else if (text.startsWith("Study Duration >=")) {
            int maxDuration = Integer.parseInt(text.substring(18).trim());
            return "checker.checkMaxStudyDuration(student, " + maxDuration + ")";
        } else if (text.startsWith("FF_COUNT")) {
            String details = text.substring(text.indexOf("(") + 1, text.indexOf(")"));
            String[] parts = details.split("<=");
            int maxFFAllowed = Integer.parseInt(parts[1].trim());
            int minFFAllowed = Integer.parseInt(parts[0].trim());
            boolean tableCourseEnabled = text.contains("T.Course: Enabled");
            if (tableCourseEnabled)
                return "checker.checkFFgrades(student, " + maxFFAllowed + ", " + minFFAllowed + ", true)";
            else
                return "checker.checkFFgrades(student, " + maxFFAllowed + ", " + minFFAllowed + ")";
        } else if (text.startsWith("WrL_COUNT")) {
            String details = text.substring(text.indexOf("(") + 1, text.indexOf(")"));
            String[] parts = details.split("<=");
            int maxAllowed = Integer.parseInt(parts[1].trim());
            return "checker.checkWithdrawalAndLeavesCount(student, " + maxAllowed + ")";
        } else if (text.matches(".*\\bXX400\\b.*")) {
            return "checker.checkInternship(student)";
        } else if (text.startsWith("FAILED_COUNT")) {
            String details = text.substring(text.indexOf("(") + 1, text.indexOf(")"));
            String[] parts = details.split("<=");
            int maxAllowed = Integer.parseInt(parts[1].trim());
            int minAllowed = Integer.parseInt(parts[0].trim());
            return "checker.checkFailedCourses(student, " + maxAllowed + ", " + minAllowed + ")";
        } else if (text.contains("not yükseltirsem") || text.matches(".*\\d+\\.\\d{2}\\+.*")) {
            Matcher gpaMatcher = Pattern.compile("(\\d+\\.\\d+)\\+").matcher(text);
            if (gpaMatcher.find()) {
                double requiredGPA = Double.parseDouble(gpaMatcher.group(1));
                return "checker.canGradeImprovementRaiseCGPA(student, " + requiredGPA + ")";
            }
        } else if (text.matches(".*(bütün dersleri).*")
                && (text.matches(".*(başarılı|başarısız).*") || text.matches(".*(hepsinin).*"))) {
            return "checker.isAllCoursesTaken(student)";
        } else if (text.matches(".*(hakkımı|kullandım).*")) {
            return "checker.gotExamRightAndUsed(student)";
        } else if (text.matches(
                ".*(hakkımda).*(ek\\s*sınav|ek_sınav|ek\\s*sınavları|ek\\s*imtihan|derse\\s*bir\\s*defa\\s*devam).*((kararı\\s*alınmıştı)|(kararı\\s*alındı)).*")) {
            return "checker.gotExamRight(student)";
        }

        // Default condition if text is not recognized
        return "true";
    }

    private static void generateIfElse(StringBuilder javaCode, String nodeId, Map<String, JSONObject> nodeMap,
            Map<String, JSONObject> connectionsMap, String indent) {
        JSONObject currentNode = nodeMap.get(nodeId);
        if (currentNode == null)
            return;

        JSONObject startNode = currentNode.getJSONObject("startNode");
        String condition = generateCondition(startNode, student, checker);
        JSONObject endNodeTrue = connectionsMap.get(startNode.getString("id") + true);
        JSONObject endNodeFalse = connectionsMap.get(startNode.getString("id") + false);

        javaCode.append(indent).append("if (").append(condition).append(") {\n");
        if (endNodeTrue != null) {
            javaCode.append(indent).append("    System.out.println(\"").append(endNodeTrue.getString("text"))
                    .append("\");\n");
            generateIfElse(javaCode, endNodeTrue.getString("id"), nodeMap, connectionsMap, indent + "    ");
        }
        javaCode.append(indent).append("} else {\n");
        if (endNodeFalse != null) {
            javaCode.append(indent).append("    System.out.println(\"").append(endNodeFalse.getString("text"))
                    .append("\");\n");
            generateIfElse(javaCode, endNodeFalse.getString("id"), nodeMap, connectionsMap, indent + "    ");
        } else {
            javaCode.append(indent).append("    System.out.println(\"End of line or alternative path.\");\n");
        }
        javaCode.append(indent).append("}\n");
    }

    public static void main(String[] args) throws Exception {
        String jsonText = new String(Files.readAllBytes(Paths
                .get("C:\\Users\\Lenovo\\Desktop\\test\\modules\\demo\\src\\main\\java\\com\\automation\\rules.json")));
        JSONArray rules = new JSONArray(jsonText);
        Map<String, JSONObject> nodeMap = buildNodeMap(rules);
        Map<String, JSONObject> connectionsMap = buildConnectionsMap(rules);
        StringBuilder javaCode = new StringBuilder();
        String startNodeId = findStartNodeId(nodeMap); // Find the start node ID dynamically

        generateIfElse(javaCode, startNodeId, nodeMap, connectionsMap, "");
        System.out.println(javaCode.toString());

    }

}
