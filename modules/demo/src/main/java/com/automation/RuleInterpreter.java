package com.automation;

import org.json.JSONArray;
import org.json.JSONObject;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;

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
        for (int i = 0; i < rules.length(); i++) {
            JSONObject rule = rules.getJSONObject(i);
            JSONObject startNode = rule.getJSONObject("startNode");
            JSONObject endNode = rule.getJSONObject("endNode");
            nodeMap.put(startNode.getString("id"),
                    new JSONObject().put("startNode", startNode).put("endNode", endNode));
        }
        return nodeMap;
    }

    public String evaluateRules(Student student) {
        Map<String, JSONObject> nodeMap = buildNodeMap(this.rules);

        Map<String, JSONObject> connectionsMap = buildConnectionsMap(this.rules);

        System.out.println("Node Map: " + nodeMap);

        String startNodeId = findStartNodeId(nodeMap);
        StringBuilder result = new StringBuilder();
        String finalNodeId = generateIfElse(result, startNodeId, nodeMap, connectionsMap, "", student);
        result.append("Final Decision Node ID: ").append(finalNodeId).append("\n");

        JSONObject finalNode = connectionsMap.get(finalNodeId + "true");
        System.out.println("Final Node: " + finalNode);
        if (finalNode != null && finalNode.has("endNode")) {
            String finalDecision = finalNode.getJSONObject("endNode").getString("text");
            result.append("Final Decision: ").append(finalDecision).append("\n");
        } else {
            result.append("Error: Final node or decision not properly configured.\n");
        }
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

    private static String generateIfElse(StringBuilder result, String nodeId, Map<String, JSONObject> nodeMap,
            Map<String, JSONObject> connectionsMap, String indent, Student student) {
        JSONObject currentNode = nodeMap.get(nodeId);
        if (currentNode == null) {
            result.append(indent).append("No more nodes to evaluate. Node ID: ").append(nodeId)
                    .append(" is terminal.\n");
            return nodeId; // Return the current node ID as the final node if no further nodes are found.
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
            return nodeId; // Return current node ID if there are no further true/false branches to follow.
        }
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
                return checker2.checkTableCourses(student2);
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
