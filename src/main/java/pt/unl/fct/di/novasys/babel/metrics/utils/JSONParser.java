package pt.unl.fct.di.novasys.babel.metrics.utils;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.*;

/**
 * Minimal hand-written JSON parser that converts a JSON string or file into a tree of
 * {@link java.util.Map}, {@link java.util.List}, {@link String}, {@link Boolean}, {@link Double},
 * and {@link Integer} values without any external dependency.
 */
public class JSONParser {

    /**
     * Parses a JSON string and returns the root value as a Java object.
     * Objects become {@code Map<String, Object>}, arrays become {@code List<Object>}, strings become
     * {@code String}, booleans become {@code Boolean}, and numbers become {@code Double} or {@code Integer}.
     *
     * @param json the JSON string to parse
     * @return the parsed value
     * @throws RuntimeException if the input is not valid JSON
     */
    public static Object parseJson(String json) {
        Tokenizer tokenizer = new Tokenizer(json);
        tokenizer.skipInitialWhitespace();  // Skip leading whitespace
        return parseValue(tokenizer); // Parse the root element, which can be either an object or an array
    }

    /**
     * Reads a JSON file and parses its contents, trimming each line before parsing.
     *
     * @param filePath path to the JSON file
     * @return the parsed value (same types as {@link #parseJson(String)})
     * @throws RuntimeException if the file cannot be read or its contents are not valid JSON
     */
    public static Object parseJsonFile(String filePath) {
        // Use BufferedReader to read the file content
        StringBuilder jsonBuilder = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new FileReader(filePath))) {
            String line;
            while ((line = reader.readLine()) != null) {
                jsonBuilder.append(line.trim()); // Trim each line to remove leading/trailing whitespace
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        String json = jsonBuilder.toString();
        return parseJson(json);
    }



    private static Map<String, Object> parseObject(Tokenizer tokenizer) {
        Map<String, Object> map = new HashMap<>();
        tokenizer.consume('{');

        while (tokenizer.peek() != '}') {
            String key = tokenizer.nextString();
            tokenizer.consume(':');
            Object value = parseValue(tokenizer);
            map.put(key, value);

            if (tokenizer.peek() == ',') {
                tokenizer.consume(',');
            }
        }
        tokenizer.consume('}');
        return map;
    }

    private static List<Object> parseArray(Tokenizer tokenizer) {
        List<Object> list = new ArrayList<>();
        tokenizer.consume('[');

        while (tokenizer.peek() != ']') {
            list.add(parseValue(tokenizer));

            if (tokenizer.peek() == ',') {
                tokenizer.consume(',');
            }
        }
        tokenizer.consume(']');
        return list;
    }

    private static Object parseValue(Tokenizer tokenizer) {
        char nextChar = tokenizer.peek();

        if (nextChar == '{') {
            return parseObject(tokenizer);
        } else if (nextChar == '[') {
            return parseArray(tokenizer);
        } else if (nextChar == '"') {
            return tokenizer.nextString();
        } else {
            return tokenizer.nextPrimitive();
        }
    }

    private static class Tokenizer {
        private final String json;
        private int pos;

        public Tokenizer(String json) {
            this.json = json;
            this.pos = 0;
        }

        // Skip any leading whitespace or tabs
        public void skipInitialWhitespace() {
            while (pos < json.length() && (json.charAt(pos) == ' ' || json.charAt(pos) == '\t' || json.charAt(pos) == '\n')) {
                pos++;
            }
        }

        public char peek() {
            skipWhitespace();
            return json.charAt(pos);
        }

        public void consume(char expected) {
            skipWhitespace();
            if (json.charAt(pos) != expected) {
                throw new RuntimeException("Expected '" + expected + "' but found '" + json.charAt(pos) + "'");
            }
            pos++;
        }

        public String nextString() {
            consume('"');
            StringBuilder sb = new StringBuilder();
            while (json.charAt(pos) != '"') {
                sb.append(json.charAt(pos));
                pos++;
            }
            consume('"');
            return sb.toString();
        }

        public Object nextPrimitive() {
            StringBuilder sb = new StringBuilder();
            while (pos < json.length() && !Character.isWhitespace(json.charAt(pos)) && json.charAt(pos) != ',' && json.charAt(pos) != ']' && json.charAt(pos) != '}') {
                sb.append(json.charAt(pos));
                pos++;
            }
            String value = sb.toString();
            if (value.equals("true") || value.equals("false")) {
                return Boolean.parseBoolean(value);
            }
            if (value.contains(".")) {
                return Double.parseDouble(value);
            }
            return Integer.parseInt(value);
        }

        private void skipWhitespace() {
            while (pos < json.length() && Character.isWhitespace(json.charAt(pos))) {
                pos++;
            }
        }
    }


    public static void main(String[] args) {
        String jsonString = "[{\"type\":\"TimedLogExporter\",\"name\":false,\"exporterConfigs\":\"./exporter.Properties\",\"exporterCollectOptions\":\"./exporter_collectoptions.json\"}]";
        Object parsedJson = parseJson(jsonString);
        System.out.println(parsedJson);
    }
}

