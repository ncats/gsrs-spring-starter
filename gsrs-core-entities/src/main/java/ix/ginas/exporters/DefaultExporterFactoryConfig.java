package ix.ginas.exporters;

import java.util.List;
import java.util.Set;

/*
Default implementation
 */
public class DefaultExporterFactoryConfig<T> implements ExporterFactoryConfig<T>{

    private String exporterName;
    private List<String> supportedFileExtensions;
    private Set<Class<RecordScrubber<T>>> recordScrubbers;
    private ExporterFactory<T> exporterFactory;
    private ScrubberExportSettings  scrubberExportSettings;
    private ExporterSpecificExportSettings<T> exporterSpecificExportSettings;
    private GeneralExportSettings generalExportSettings;
    private String fileName;

    @Override
    public String getExporterName() {
        return exporterName;
    }

    @Override
    public List<String> getSupportedFileExtensions() {
        return supportedFileExtensions;
    }

    @Override
    public void setSupportedFileExtensions(List<String> extensions) {
        this.supportedFileExtensions= extensions;
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
    public Set<Class<RecordScrubber<T>>> getRecordScrubbers() {
        return this.recordScrubbers;
    }

    @Override
    public void setExportFactory(ExporterFactory<T> exporterFactory) {
        this.exporterFactory=exporterFactory;
    }

    @Override
    public ExporterFactory<T> getExportFactory() {
        return this.exporterFactory;
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
        exporterSpecificExportSettings=settings;
    }

    @Override
    public ExporterSpecificExportSettings<T> getExporterSettings() {
        return exporterSpecificExportSettings;
    }

    @Override
    public void setRecordScrubbers(Set<Class<RecordScrubber<T>>> scrubbers) {
        this.recordScrubbers=scrubbers;
    }
}
