package com.foodapp.util;

import java.util.*;

/**
 * Lightweight JSON parser and formatter utility for zero-dependency Java execution.
 */
public class SimpleJson {

    public static String escape(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }

    public static Map<String, String> parseSimpleObject(String json) {
        Map<String, String> map = new LinkedHashMap<>();
        if (json == null || json.trim().isEmpty()) return map;
        
        String trimmed = json.trim();
        if (trimmed.startsWith("{")) trimmed = trimmed.substring(1);
        if (trimmed.endsWith("}")) trimmed = trimmed.substring(0, trimmed.length() - 1);
        
        // Split top-level properties carefully
        List<String> pairs = splitPairs(trimmed);
        for (String pair : pairs) {
            int colonIdx = pair.indexOf(':');
            if (colonIdx > 0) {
                String key = cleanValue(pair.substring(0, colonIdx));
                String val = pair.substring(colonIdx + 1).trim();
                // Strip surrounding quotes from scalar string values so callers
                // receive clean values (e.g. WELCOME50 instead of "WELCOME50").
                // Arrays/objects are preserved as-is for nested parsing.
                map.put(key, cleanValue(val));
            }
        }
        return map;
    }

    private static List<String> splitPairs(String body) {
        List<String> result = new ArrayList<>();
        boolean inQuotes = false;
        int depth = 0;
        StringBuilder sb = new StringBuilder();

        for (int i = 0; i < body.length(); i++) {
            char c = body.charAt(i);
            if (c == '"' && (i == 0 || body.charAt(i - 1) != '\\')) {
                inQuotes = !inQuotes;
            }
            if (!inQuotes) {
                if (c == '{' || c == '[') depth++;
                else if (c == '}' || c == ']') depth--;
                else if (c == ',' && depth == 0) {
                    result.add(sb.toString());
                    sb.setLength(0);
                    continue;
                }
            }
            sb.append(c);
        }
        if (sb.length() > 0) {
            result.add(sb.toString());
        }
        return result;
    }

    public static String cleanValue(String raw) {
        if (raw == null) return "";
        raw = raw.trim();
        if (raw.startsWith("\"") && raw.endsWith("\"") && raw.length() >= 2) {
            return raw.substring(1, raw.length() - 1)
                      .replace("\\\"", "\"")
                      .replace("\\\\", "\\");
        }
        return raw;
    }

    public static List<Map<String, String>> parseArrayOfObjects(String jsonArray) {
        List<Map<String, String>> list = new ArrayList<>();
        if (jsonArray == null || jsonArray.trim().isEmpty()) return list;
        
        String trimmed = jsonArray.trim();
        if (trimmed.startsWith("[")) trimmed = trimmed.substring(1);
        if (trimmed.endsWith("]")) trimmed = trimmed.substring(0, trimmed.length() - 1);
        
        int depth = 0;
        boolean inQuotes = false;
        StringBuilder currentObj = new StringBuilder();

        for (int i = 0; i < trimmed.length(); i++) {
            char c = trimmed.charAt(i);
            if (c == '"' && (i == 0 || trimmed.charAt(i - 1) != '\\')) {
                inQuotes = !inQuotes;
            }
            if (!inQuotes) {
                if (c == '{') {
                    if (depth > 0) currentObj.append(c);
                    depth++;
                } else if (c == '}') {
                    depth--;
                    if (depth > 0) currentObj.append(c);
                    else {
                        list.add(parseSimpleObject(currentObj.toString()));
                        currentObj.setLength(0);
                    }
                } else if (depth > 0) {
                    currentObj.append(c);
                }
            } else if (depth > 0) {
                currentObj.append(c);
            }
        }
        return list;
    }
}
