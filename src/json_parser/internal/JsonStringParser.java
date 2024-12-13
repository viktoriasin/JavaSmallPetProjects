package json_parser.internal;

import json_parser.exceptions.JsonParserException;

import java.util.*;

import static json_parser.internal.JsonElementCategory.NOT_VALID;
import static json_parser.internal.JsonElementCategory.VALID_OTHER;

public class JsonStringParser {
    CurrentParserCondition currentParserCondition;
    int currentParserIndex;
    char[] sourceJsonElements;

    JsonStringParser(String jsonString) {
        sourceJsonElements = jsonString.toCharArray();
        currentParserIndex = 0;
    }

    public Set<JsonKeyValuePair> parse(String jsonString) {
        return getJsonElementsMap(currentParserIndex, jsonString.length());
    }

    public Set<JsonKeyValuePair> getJsonElementsMap(int from, int to) {
        Set<JsonKeyValuePair> parsedJsonPairs = new HashSet<>();
        while (true) {

            Optional<JsonKeyValuePair> jsonPair = generateNextKeyValuePair();

            if (jsonPair.isEmpty()) {
                break;
            }

            parsedJsonPairs.add(jsonPair.get());
        }

        return parsedJsonPairs;
    }

    private Optional<JsonKeyValuePair> generateNextKeyValuePair() {
        Optional<JsonKey> key = generateNextKey();
        Optional<JsonValue> value = generateNextValue();
        if (key.isEmpty() || value.isEmpty()) {
            return Optional.empty(); // TODO если только один пустой - значит невалидный json, обработать
        }
        return Optional.of(new JsonKeyValuePair(key.get(), value.get()));
    }

    private Optional<JsonKey> generateNextKey() {
        String key = findNextJsonElement(new KeyCategorizer());
        return Optional.of(new JsonKey(key));
    }

    private Optional<JsonValue> generateNextValue() {
        String value = findNextJsonElement(new ValueValidator());
        return Optional.of(new JsonValue(value));
    }

    public String findNextJsonElement(JsonCategorizer characterValidator) {
        if (sourceJsonElements.length == currentParserIndex) {
            return null; // строка закончилась
        }
        StringBuilder result = new StringBuilder();

        for (int i = currentParserIndex; i < sourceJsonElements.length; i++) {
            char ch = sourceJsonElements[i];
            boolean isValid = characterValidator.categorize(ch, i);
            if (isValid) {
                result.append(ch);
            } else {
                throw new JsonParserException("Json not valid");
            }
        }

        return result.toString();
    }


    public static class CurrentParserCondition {

        boolean receiveOpenDoubleQuote;
        boolean receiveDotes;

        CurrentParserCondition(boolean receiveOpenDoubleQuote, boolean receiveDotes) {
            this.receiveOpenDoubleQuote = receiveOpenDoubleQuote;
            this.receiveDotes = receiveDotes;
        }

        public boolean isReceiveOpenDoubleQuote() {
            return receiveOpenDoubleQuote;
        }

        public boolean isReceiveDotes() {
            return receiveDotes;
        }
    }

    private static String prepareSourceStringToProcessing(String sourceString) {
        int stringLength = sourceString.length();
        if (stringLength < 2) {
            throw new IllegalArgumentException();
        }
        // избавляемся от {} на самом верхнем уровне
        return sourceString.substring(1, stringLength);
    }

    class ValueValidator implements JsonCategorizer {
        @Override
        public boolean categorize(char ch) {
            if (ch == ':' && !currentParserCondition.isReceiveDotes()) {
                currentParserCondition.receiveDotes = true;
            } else if (currentParserCondition.isReceiveDotes()) {
                if (ch == ' ' && currentParserCondition.isReceiveOpenDoubleQuote()) {
                    return true;
                }
                if (ch == '\"' && !currentParserCondition.isReceiveOpenDoubleQuote()) {
                    currentParserCondition.receiveOpenDoubleQuote = true;
                }
                boolean b = Character.isLetter(ch) || Character.isSpaceChar(ch);
                if (b && !currentParserCondition.isReceiveOpenDoubleQuote()) {
                    throw new IllegalArgumentException("Буквенные и пробельные символы в значениях допускаются только в двойных кавычках!");
                }
                if (b && currentParserCondition.isReceiveOpenDoubleQuote()) {
                    if (ch == '\"' && index-1 >= 0 && sourceJsonElements[index-1] == '\\') {
                        return false;
                    }
                    return true;
                }
                if ((Character.isDigit(ch) || ch == '.') && !currentParserCondition.isReceiveOpenDoubleQuote()) {
                    return true;
                }
            }
            return false;
        }
    }

    public class KeyCategorizer implements JsonCategorizer {
        @Override
        public JsonElementCategory categorize(char ch) {
            CurrentParserCondition currentParserCondition = new CurrentParserCondition(false, false);
            if (ch == '\"' && !currentParserCondition.isReceiveOpenDoubleQuote()) {
                currentParserCondition.receiveOpenDoubleQuote = true;
                return VALID_OTHER;
            } else if (ch == '\"') {
                if (currentParserIndex-1 >= 0 && sourceJsonElements[currentParserIndex-1] != '\\') {
                    return NOT_VALID;
                }
            }
            return NOT_VALID;
        }
    }

}
