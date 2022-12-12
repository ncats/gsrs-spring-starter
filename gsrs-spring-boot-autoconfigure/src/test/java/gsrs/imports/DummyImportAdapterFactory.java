package gsrs.imports;

import com.fasterxml.jackson.databind.JsonNode;

import java.io.InputStream;
import java.util.Arrays;
import java.util.List;

public class DummyImportAdapterFactory implements ImportAdapterFactory {

    private String fileName;
    private Class holdingAreaServiceClass;
    private List<Class> services;
    private Class serviceClass;

    public String ADAPTER_NAME="Dummy Import Adapter";
    @Override
    public String getAdapterName() {
        return ADAPTER_NAME;
    }

    @Override
    public String getAdapterKey() {
        return "dummy";
    }

    public static String[] extensions = {"txt", "sdf"};

    @Override
    public List<String> getSupportedFileExtensions() {
        return Arrays.asList(extensions);
    }

    @Override
    public ImportAdapter createAdapter(JsonNode adapterSettings) {
        return null;
    }

    @Override
    public ImportAdapterStatistics predictSettings(InputStream is) {
        return null;
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
    public void setEntityServices(List services) {
        this.services=services;
    }

}
