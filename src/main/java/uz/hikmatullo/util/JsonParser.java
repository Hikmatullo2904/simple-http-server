package uz.hikmatullo.util;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.*;

public class JsonParser {
    private static final ObjectMapper myObjectMapper = defaultObjectMapper();
    private static ObjectMapper defaultObjectMapper() {
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        return objectMapper;
    }

    public static JsonNode parse(String str) throws JsonProcessingException {
        return myObjectMapper.readTree(str);
    }

    public static <T> T fromJson(JsonNode jsonNode, Class<T> clazz) throws JsonProcessingException {
        return myObjectMapper.treeToValue(jsonNode, clazz);
    }

    public static String toJson(Object object) throws JsonProcessingException {
        return myObjectMapper.writeValueAsString(object);
    }

    public static String stringify(JsonNode node) throws JsonProcessingException {
        return generateJson(node, false);
    }

    public static String stringifyPretty(JsonNode node) throws JsonProcessingException {
        return generateJson(node, true);
    }

    private static String generateJson(Object o, boolean pretty) throws JsonProcessingException {
        ObjectWriter writer = myObjectMapper.writer();

        if (pretty) {
            writer = writer.with(SerializationFeature.INDENT_OUTPUT);
        }
        return writer.writeValueAsString(o);
    }
}
