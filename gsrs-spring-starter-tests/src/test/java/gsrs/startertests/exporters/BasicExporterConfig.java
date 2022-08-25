package gsrs.startertests.exporters;

import ix.ginas.exporters.*;
import ix.ginas.models.GinasCommonData;

import java.util.Arrays;
import java.util.List;

public class BasicExporterConfig implements ExporterFactoryConfig<GinasCommonData> {

    private String exporterName="BasicExporter";
    private List<String> extensions = Arrays.asList("txt");
    private String fileName;
    private Class<RecordScrubber> scrubber;
    private ExporterFactory factory;
    private GeneralExportSettings generalExportSettings;
    private ScrubberExportSettings scrubberExportSettings;
    private ExporterSpecificExportSettings specificExportSettings;

    @Override
    public String getExporterName() {
        return exporterName;
    }

    @Override
    public List<String> getSupportedFileExtensions() {
        return extensions;
    }

    @Override
    public void setFileName(String fileName) {
        this.fileName= fileName;
    }

    @Override
    public String getFileName() {
        return this.fileName;
    }

    @Override
    public void setRecordScrubber(Class<RecordScrubber> scrubber) {
        this.scrubber=scrubber;
    }

    @Override
    public Class<RecordScrubber> getRecordScrubber() {
        return this.scrubber;
    }

    @Override
    public void setExportFactory(ExporterFactory exporterFactory) {
        factory=exporterFactory;
    }

    @Override
    public ExporterFactory getExportFactory() {
        return this.factory;
    }

    @Override
    public void setGeneralSettings(GeneralExportSettings settings) {
        this.generalExportSettings=settings;
    }

    @Override
    public GeneralExportSettings getGeneralSettings() {
        return this.generalExportSettings;
    }

    @Override
    public void setScrubberSettings(ScrubberExportSettings settings) {
        this.scrubberExportSettings=settings;
    }

    @Override
    public ScrubberExportSettings getScrubberSettings() {
        return this.scrubberExportSettings;
    }

    @Override
    public void setExporterSettings(ExporterSpecificExportSettings settings) {
        this.specificExportSettings=settings;
    }

    @Override
    public ExporterSpecificExportSettings getExporterSettings() {
        return this.specificExportSettings;
    }
}
