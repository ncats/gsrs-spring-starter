package ix.core.interfaces;

import tools.jackson.databind.JsonNode;
import tools.jackson.core.type.TypeReference;

public interface GsrsJsonMapper {
    String writeValueAsString(Object value);
    JsonNode toJsonNode(Object value);
    <T> T readValue(String json, Class<T> type);
    <T> T treeToValue(JsonNode node, Class<T> type);
    JsonNode readTree(String json);
    JsonNode valueToTree(Object value);
    <T> T convertValue(Object fromValue, TypeReference<T> toValueTypeRef);
    <T> T convertValue(Object fromValue, Class<T> toValueType);
}