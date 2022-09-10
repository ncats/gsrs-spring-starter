package gsrs.startertests.exporters;

import ix.ginas.exporters.*;
import ix.ginas.models.GinasCommonData;

import java.util.Arrays;
import java.util.List;
import java.util.Set;

public class BasicExporterConfig implements ExporterFactoryConfig<GinasCommonData> {

    private String exporterName="BasicExporter";
    private List<String> extensions = Arrays.asList("txt");
    private String fileName;
    private Set<Class<RecordScrubber<GinasCommonData>>> scrubbers;
    private ExporterFactory<GinasCommonData> factory;
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
    public void setSupportedFileExtensions(List<String> extensions) {
        this.extensions=extensions;
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
    public void setRecordScrubbers(Set<Class<RecordScrubber<GinasCommonData>>> scrubbers) {
        this.scrubbers=scrubbers;
    }

    @Override
    public Set<Class<RecordScrubber<GinasCommonData>>> getRecordScrubbers() {
        return this.scrubbers;
    }

    @Override
    public void setExportFactory(ExporterFactory<GinasCommonData> exporterFactory) {
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
