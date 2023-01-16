package gsrs.dataexchange.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import gsrs.imports.ImportAdapter;
import gsrs.imports.ImportAdapterFactory;
import gsrs.imports.ImportAdapterStatistics;
import ix.ginas.importers.InputFieldStatistics;
import ix.ginas.importers.TextFileReader;
import ix.ginas.models.GinasCommonData;

import java.io.InputStream;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class DummyImportAdapterFactory implements ImportAdapterFactory<GinasCommonData> {

    private String fileName;
    private Class holdingAreaServiceClass;
    private List<Class> services;
    private Class serviceClass;

    private String description = "Simplest Import Adapter used for testing";

    public static String ADAPTER_NAME="Dummy Import Adapter";
    @Override
    public String getAdapterName() {
        return ADAPTER_NAME;
    }

    @Override
    public String getAdapterKey() {
        return "dummy";
    }

    public String[] extensions = {"txt", "sdf"};

    @Override
    public List<String> getSupportedFileExtensions() {
        return Arrays.asList(extensions);
    }

    @Override
    public void setSupportedFileExtensions(List extensions) {
        this.extensions = (String[]) extensions.toArray();
    }

    @Override
    public ImportAdapter createAdapter(JsonNode adapterSettings) {
        return new DummyImportAdapter();
    }

    @Override
    public ImportAdapterStatistics predictSettings(InputStream is, ObjectNode settings) {
        ImportAdapterStatistics statistics = new ImportAdapterStatistics();
        ObjectNode node = JsonNodeFactory.instance.objectNode();
        TextFileReader reader = new TextFileReader();
        Map<String, InputFieldStatistics> map=reader.getFileStatistics(is, ",", false, null, 100, 0);
        node.putPOJO("Fields", map.keySet().stream().collect(Collectors.toList()));
        node.put("fileName", getFileName());
        statistics.setAdapterSchema(node);
        ObjectNode adapterSettings = JsonNodeFactory.instance.objectNode();
        ArrayNode result = JsonNodeFactory.instance.arrayNode();
        result.add(createDefaultReferenceNode());
        adapterSettings.set("actions", result);
        statistics.setAdapterSettings(adapterSettings);
        return statistics;
    }

    @Override
    public void setFileName(String fileName) {
        this.fileName=fileName;
    }

    @Override
    public String getFileName() {
        return this.fileName;
    }

    @Override
    public void initialize() throws IllegalStateException {
        ImportAdapterFactory.super.initialize();
    }

    @Override
    public Class getHoldingAreaService() {
        return this.holdingAreaServiceClass;
    }

    @Override
    public void setHoldingAreaService(Class holdingAreaService){
        this.holdingAreaServiceClass = holdingAreaService;
    }

    @Override
    public Class getHoldingAreaEntityService() {
        return null;
    }

    @Override
    public void setHoldingAreaEntityService(Class holdingAreaEntityService) {

    }

    @Override
    public List<Class> getEntityServices() {
        return this.services;
    }

    @Override
    public Class getEntityServiceClass() {
        return serviceClass;
    }

    @Override
    public void setEntityServiceClass(Class newClass) {
        serviceClass=newClass;
    }

    @Override
    public void setInputParameters(JsonNode parameters) {

    }

    @Override
    public String getDescription() {
        return this.description;
    }

    @Override
    public void setDescription(String description) {
        this.description=description;
    }

    @Override
    public void setEntityServices(List services) {
        this.services=services;
    }


    protected JsonNode createDefaultReferenceNode() {
        ObjectNode referenceNode = JsonNodeFactory.instance.objectNode();
        referenceNode.put("actionName", "public_reference");

        ObjectNode parameters = JsonNodeFactory.instance.objectNode();
        parameters.put("docType", "CATALOG");
        parameters.put("citation", "INSERT REFERENCE CITATION HERE");
        parameters.put("referenceID", "INSERT REFERENCE ID HERE");
        parameters.put("uuid", String.format("[[%s]]", "UUID_1"));
        referenceNode.set("actionParameters", parameters);

        return referenceNode;
    }
}
