package com.automation;

import org.json.JSONArray;
import org.json.JSONObject;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;

public class RuleInterpreter {
    public static void main(String[] args) throws Exception {
        String jsonText = new String(Files.readAllBytes(Paths
                .get("C:\\Users\\Lenovo\\Desktop\\test\\modules\\demo\\src\\main\\java\\com\\automation\\rules.json")));
        JSONArray rules = new JSONArray(jsonText);
        Map<String, JSONObject> nodeMap = buildNodeMap(rules);
        Map<String, JSONObject> connectionsMap = buildConnectionsMap(rules);
        StringBuilder javaCode = new StringBuilder();
        generateIfElse(javaCode, "0", nodeMap, connectionsMap, "");
        System.out.println(javaCode.toString());
    }

    private static Map<String, JSONObject> buildNodeMap(JSONArray rules) {
        Map<String, JSONObject> nodeMap = new HashMap<>();
        for (int i = 0; i < rules.length(); i++) {
            JSONObject rule = rules.getJSONObject(i);
            JSONObject startNode = rule.getJSONObject("startNode");
            nodeMap.put(startNode.getString("id"), rule);
        }
        return nodeMap;
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

    private static void generateIfElse(StringBuilder javaCode, String nodeId, Map<String, JSONObject> nodeMap,
            Map<String, JSONObject> connectionsMap, String indent) {
        JSONObject currentNode = nodeMap.get(nodeId);
        if (currentNode == null)
            return;
        JSONObject startNode = currentNode.getJSONObject("startNode");
        JSONObject endNodeTrue = connectionsMap.get(startNode.getString("id") + true);
        JSONObject endNodeFalse = connectionsMap.get(startNode.getString("id") + false);

        javaCode.append(indent).append("if (/* condition for ").append(startNode.getString("text")).append(" */) {\n");
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
}
