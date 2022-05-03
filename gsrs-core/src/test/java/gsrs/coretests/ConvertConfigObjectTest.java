package gsrs.coretests;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import ix.core.util.EntityUtils;
import org.aspectj.weaver.TypeFactory;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class ConvertConfigObjectTest {

    @Test
    public void testConvertConfigObject() throws JsonProcessingException {
        ObjectNode action = JsonNodeFactory.instance.objectNode();
        action.put("actionName", "cas_import");
        action.put("actionClass", "gsrs.module.substance.importers.importActionFactories.NSRSCustomCodeExtractorActionFactory");

        ArrayNode arrayNode = JsonNodeFactory.instance.arrayNode();
        ObjectNode objectNode = JsonNodeFactory.instance.objectNode();
        objectNode.put("fieldName", "code");
        objectNode.put("required",true);
        objectNode.put("fieldType", "java.lang.String");
        objectNode.put("expectedToChange", true);
        objectNode.put("fieldLabel", "CAS Number");
        arrayNode.add(objectNode);
        objectNode = JsonNodeFactory.instance.objectNode();
        objectNode.put("fieldName", "codeType");
        objectNode.put("required",false);
        objectNode.put("fieldType", "java.lang.String");
        objectNode.put("expectedToChange", true);
        objectNode.put("defaultValue", "PRIMARY");
        objectNode.put("fieldLabel", "Primary or Alternative");
        arrayNode.add(objectNode);

        action.set("fields", arrayNode);
        ObjectNode parameterNode = JsonNodeFactory.instance.objectNode();
        parameterNode.put("codeSystem", "CAS");
        action.set("parameters", parameterNode);

        String nodeData = action.toString();
        System.out.println("nodeData: " + nodeData);

        JsonNode node= (JsonNode) EntityUtils.convertConfigObject(nodeData);
        System.out.println("returned node: " + node.toPrettyString());
        Assertions.assertNotNull(node);
    }

    @Test
    public void testConvertConfigComplexObject() throws JsonProcessingException {
        Map<String, Object> action = new LinkedHashMap<>();
        action.put("actionName", "cas_import");
        action.put("actionClass", "gsrs.module.substance.importers.importActionFactories.NSRSCustomCodeExtractorActionFactory");

        Map<String, Object> arrayMap = new LinkedHashMap<>();
        Map<String, Object> objectMap = new LinkedHashMap<>();
        objectMap.put("fieldName", "code");
        objectMap.put("required",true);
        objectMap.put("fieldType", "java.lang.String");
        objectMap.put("expectedToChange", true);
        objectMap.put("fieldLabel", "CAS Number");
        arrayMap.put("0", objectMap);
        objectMap = new LinkedHashMap<>();
        objectMap.put("fieldName", "codeType");
        objectMap.put("required",false);
        objectMap.put("fieldType", "java.lang.String");
        objectMap.put("expectedToChange", true);
        objectMap.put("defaultValue", "PRIMARY");
        objectMap.put("fieldLabel", "Primary or Alternative");
        arrayMap.put("1", objectMap);
        action.put("parameters", arrayMap);

        List arrayMapObject = EntityUtils.convertClean(arrayMap, new TypeReference<List>() { });
        Assertions.assertNotNull(arrayMapObject);
        Assertions.assertEquals(ArrayList.class, arrayMapObject.getClass());
        
        Map newMapObject = EntityUtils.convertClean(action, new TypeReference<Map>() { });
        
        Assertions.assertNotNull(newMapObject);
        Object o = newMapObject.get("parameters");
        Assertions.assertEquals(ArrayList.class, o.getClass());
        
    }
}
