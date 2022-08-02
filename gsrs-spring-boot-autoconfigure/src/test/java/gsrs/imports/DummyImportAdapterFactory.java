package gsrs.imports;

import com.fasterxml.jackson.databind.JsonNode;
import gsrs.controller.AbstractImportSupportingGsrsEntityController;

import java.io.InputStream;
import java.util.Arrays;
import java.util.List;

public class DummyImportAdapterFactory implements ImportAdapterFactory {

    private String fileName;
    private Class holdingAreaServiceClass;

    public String ADAPTER_NAME="Dummy Import Adapter";
    @Override
    public String getAdapterName() {
        return ADAPTER_NAME;
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
}
