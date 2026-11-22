package com.studentexpensetracker;

import java.util.HashMap;
import java.util.Map;

/** Reads the flat JSON objects sent by the React form fields. */
public final class SimpleJson {
    private SimpleJson() { }

    public static Map<String, String> readObject(String text) {
        if (text == null) throw new IllegalArgumentException("Request body is empty.");
        return new ObjectReader(text).readObject();
    }

    /** Stores the text and current character number while reading the request. */
    private static final class ObjectReader {
        private final String text;
        private int position;

        ObjectReader(String text) { this.text = text; }

        Map<String, String> readObject() {
            Map<String, String> fields = new HashMap<>();
            skipSpaces();
            expect('{');
            skipSpaces();
            if (hasText() && text.charAt(position) == '}') {
                position++;
                skipSpaces();
                if (position != text.length()) throw new IllegalArgumentException("Unexpected text after JSON object.");
                return fields;
            }
            while (hasText()) {
                skipSpaces();
                String key = readString();
                skipSpaces();
                expect(':');
                skipSpaces();
                String value = readValue();
                fields.put(key, value);
                skipSpaces();
                if (hasText() && text.charAt(position) == '}') {
                    position++;
                    break;
                }
                expect(',');
            }
            skipSpaces();
            if (position != text.length()) throw new IllegalArgumentException("Unexpected text after JSON object.");
            return fields;
        }

        private String readValue() {
            if (hasText() && text.charAt(position) == '"') return readString();
            int start = position;
            while (hasText()) {
                char current = text.charAt(position);
                if (current == ',' || current == '}' || Character.isWhitespace(current)) break;
                position++;
            }
            if (start == position) throw new IllegalArgumentException("Expected a JSON value.");
            String value = text.substring(start, position);
            if ("null".equals(value)) return "";
            if ("true".equals(value) || "false".equals(value) || isJsonNumber(value)) return value;
            throw new IllegalArgumentException("Only text, numbers, and true/false values are accepted.");
        }

        private String readString() {
            expect('"');
            StringBuilder value = new StringBuilder();
            while (hasText()) {
                char current = text.charAt(position++);
                if (current == '"') return value.toString();
                if (current < 32) throw new IllegalArgumentException("A JSON string contains an invalid control character.");
                if (current != '\\') {
                    value.append(current);
                    continue;
                }
                if (!hasText()) throw new IllegalArgumentException("Unfinished escape in JSON string.");
                appendEscapedCharacter(value, text.charAt(position++));
            }
            throw new IllegalArgumentException("JSON string is missing its closing quote.");
        }

        private void appendEscapedCharacter(StringBuilder value, char escaped) {
            if (escaped == '"' || escaped == '\\' || escaped == '/') value.append(escaped);
            else if (escaped == 'n') value.append('\n');
            else if (escaped == 'r') value.append('\r');
            else if (escaped == 't') value.append('\t');
            else if (escaped == 'b') value.append('\b');
            else if (escaped == 'f') value.append('\f');
            else if (escaped == 'u') value.append(readUnicodeCharacter());
            else throw new IllegalArgumentException("Invalid escape in JSON string.");
        }

        private char readUnicodeCharacter() {
            if (position + 4 > text.length()) throw new IllegalArgumentException("Invalid Unicode escape.");
            int number = 0;
            for (int i = 0; i < 4; i++) {
                int digit = Character.digit(text.charAt(position++), 16);
                if (digit < 0) throw new IllegalArgumentException("Invalid Unicode escape.");
                number = number * 16 + digit;
            }
            return (char) number;
        }

        private void skipSpaces() {
            while (hasText() && Character.isWhitespace(text.charAt(position))) position++;
        }
        private boolean hasText() { return position < text.length(); }
        private void expect(char wanted) {
            if (!hasText() || text.charAt(position) != wanted) throw new IllegalArgumentException("Expected '" + wanted + "' in JSON.");
            position++;
        }
    }

    private static boolean isJsonNumber(String value) {
        int index = 0;
        if (value.length() == 0) return false;
        if (value.charAt(index) == '-') index++;
        if (index >= value.length()) return false;
        if (value.charAt(index) == '0') index++;
        else {
            if (!isDigit(value.charAt(index)) || value.charAt(index) == '0') return false;
            while (index < value.length() && isDigit(value.charAt(index))) index++;
        }
        if (index < value.length() && value.charAt(index) == '.') {
            index++;
            int firstFractionDigit = index;
            while (index < value.length() && isDigit(value.charAt(index))) index++;
            if (index == firstFractionDigit) return false;
        }
        if (index < value.length() && (value.charAt(index) == 'e' || value.charAt(index) == 'E')) {
            index++;
            if (index < value.length() && (value.charAt(index) == '+' || value.charAt(index) == '-')) index++;
            int firstExponentDigit = index;
            while (index < value.length() && isDigit(value.charAt(index))) index++;
            if (index == firstExponentDigit) return false;
        }
        if (index != value.length()) return false;
        try {
            double number = Double.parseDouble(value);
            return !Double.isInfinite(number) && !Double.isNaN(number);
        } catch (NumberFormatException e) { return false; }
    }

    private static boolean isDigit(char value) { return value >= '0' && value <= '9'; }
}
