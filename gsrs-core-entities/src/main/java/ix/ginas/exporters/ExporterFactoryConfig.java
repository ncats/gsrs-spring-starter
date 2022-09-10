package ix.ginas.exporters;

import java.util.List;
import java.util.Set;

public interface ExporterFactoryConfig<T> {

    String getExporterName();

    List<String> getSupportedFileExtensions();
    void setSupportedFileExtensions(List<String> extensions);

    void setFileName(String fileName);
    String getFileName();

    void setRecordScrubbers( Set<Class<RecordScrubber<T>>> scrubbers);
    Set<Class<RecordScrubber<T>>> getRecordScrubbers();

    void setExportFactory(ExporterFactory<T> exporterFactory);
    ExporterFactory<T> getExportFactory();

    void setGeneralSettings(GeneralExportSettings settings);
    GeneralExportSettings getGeneralSettings();

    void setScrubberSettings(ScrubberExportSettings settings);
    ScrubberExportSettings getScrubberSettings();

    void setExporterSettings(ExporterSpecificExportSettings settings);
    ExporterSpecificExportSettings getExporterSettings();
}
