package gsrs.dataexchange.imports;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.TextNode;
import gsrs.dataexchange.model.MappingAction;
import gsrs.imports.ImportAdapter;
import ix.ginas.models.GinasCommonData;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;

import java.io.FileInputStream;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;

public class BasicTextImportTest {

    @Test
    public void actionParmsProcessingText1() throws Exception {
        ObjectNode settings=createSimpleObjectNode();
        List<MappingAction<GinasCommonData, TextRecordContext>> mappings= BasicImportFactory.getMappingActions(settings);
        Assertions.assertTrue(mappings.size() >0);
    }

    @Test
    public void testBasicImport() throws IOException {
        Resource dataFile = new ClassPathResource("text/basicvalues.txt");
        FileInputStream fis = new FileInputStream(dataFile.getFile().getAbsolutePath());

        BasicImportFactory basicImportFactory = new BasicImportFactory();

        ObjectNode settings=createSimpleObjectNode();
        ImportAdapter<GinasCommonData> adapter = basicImportFactory.createAdapter(settings);
        ObjectNode settingsNode = JsonNodeFactory.instance.objectNode();
        settingsNode.put("Charset", Charset.defaultCharset().name());
        Stream<GinasCommonData> objects= adapter.parse(fis, settingsNode,null);
        AtomicInteger counter = new AtomicInteger(0);
        objects.forEach(o-> {
            System.out.println("object: ");
            o.getMatchContextProperties().keySet().forEach(k -> System.out.printf("key: %s; value: %s\n", k, o.getMatchContextProperties().get(k)));
            counter.incrementAndGet();
        });
        Assertions.assertEquals(2, counter.get());
        fis.close();
    }

    @Test
    public void testBasicImportComplex() throws IOException {
        Resource dataFile = new ClassPathResource("text/basicvalues.txt");
        FileInputStream fis = new FileInputStream(dataFile.getFile().getAbsolutePath());

        BasicImportFactory basicImportFactory = new BasicImportFactory();

        ObjectNode settings=createComplexObjectNode();
        ImportAdapter<GinasCommonData> adapter = basicImportFactory.createAdapter(settings);
        ObjectNode settingsNode = JsonNodeFactory.instance.objectNode();
        settingsNode.put("Charset", Charset.defaultCharset().name());
        Stream<GinasCommonData> objects= adapter.parse(fis, settingsNode, null);
        AtomicInteger counter = new AtomicInteger(0);
        objects.forEach(o-> {
            System.out.println("object: ");
            o.getMatchContextProperties().keySet().forEach(k -> System.out.printf("key: %s; value: %s\n", k, o.getMatchContextProperties().get(k)));
            counter.incrementAndGet();
        });
        Assertions.assertEquals(2, counter.get());
        fis.close();
    }

    @Test
    public void testParameterPassing() {
        ObjectNode parameters = JsonNodeFactory.instance.objectNode();
        parameters.put("A", "a");
        parameters.put("delimiter", "\t");
        parameters.put("originality", "some original phrase");
        BasicImportFactory basicImportFactory = new BasicImportFactory();

        basicImportFactory.setInputParameters(parameters);

        JsonNode echoedSettings = basicImportFactory.getOriginalParameters();
        Iterator<String> keys =parameters.fieldNames();
        while( keys.hasNext()) {
            String field = keys.next();
            System.out.printf("field: %s; input: %s; echo: %s\n", field, parameters.get(field).asText(), echoedSettings.get(field).asText());
            Assertions.assertEquals(parameters.get(field).asText(), echoedSettings.get(field).asText());
        }
    }

    private ObjectNode createSimpleObjectNode() {
        ObjectNode settings = JsonNodeFactory.instance.objectNode();
        ObjectNode actionNode = JsonNodeFactory.instance.objectNode();

        TextNode textNode = JsonNodeFactory.instance.textNode("import text");
        actionNode.set("actionName", textNode);

        ObjectNode parameters = JsonNodeFactory.instance.objectNode();
        parameters.put("parameterName1", "value1");
        parameters.put("parameterName2", "value2");
        actionNode.set("actionParameters", parameters);
        ArrayNode actionsNode = JsonNodeFactory.instance.arrayNode();
        actionsNode.add(actionNode);

        settings.set("actions", actionsNode);
        return settings;
    }

    private ObjectNode createComplexObjectNode() {
        ObjectNode settings = JsonNodeFactory.instance.objectNode();

        ObjectNode actionNode = JsonNodeFactory.instance.objectNode();

        TextNode textNode = JsonNodeFactory.instance.textNode("import text 1");
        actionNode.set("actionName", textNode);
        TextNode textNode2 = JsonNodeFactory.instance.textNode("gsrs.dataexchange.imports.Item2MappingAction");
        actionNode.set("actionClassName", textNode2);

        ObjectNode parameters = JsonNodeFactory.instance.objectNode();
        parameters.put("parameterName1", "value1");
        parameters.put("parameterName2", "value2");
        actionNode.set("actionParameters", parameters);

        ArrayNode actionsNode = JsonNodeFactory.instance.arrayNode();
        actionsNode.add(actionNode);

        ObjectNode actionNode2 = JsonNodeFactory.instance.objectNode();

        textNode = JsonNodeFactory.instance.textNode("import text 2");
        actionNode2.set("actionName", textNode);
        TextNode textNode3 = JsonNodeFactory.instance.textNode("gsrs.dataexchange.imports.Item1MappingAction");
        actionNode2.set("actionClassName", textNode3);

        ObjectNode parameters2 = JsonNodeFactory.instance.objectNode();
        parameters2.put("parameterName1", "value1");
        parameters2.put("parameterName2", "value2");
        actionNode2.set("actionParameters", parameters2);

        actionsNode.add(actionNode2);

        settings.set("actions", actionsNode);
        return settings;
    }

}
