package ix.ginas.exporters;

import java.util.List;

public interface ExporterFactoryConfig<T> {

    String getExporterName();

    List<String> getSupportedFileExtensions();

    void setFileName(String fileName);
    String getFileName();

    void setRecordScrubber( Class<RecordScrubber> scrubber);
    Class<RecordScrubber> getRecordScrubber();

    void setExportFactory(ExporterFactory exporterFactory);
    ExporterFactory getExportFactory();

    void setGeneralSettings(GeneralExportSettings settings);
    GeneralExportSettings getGeneralSettings();

    void setScrubberSettings(ScrubberExportSettings settings);
    ScrubberExportSettings getScrubberSettings();

    void setExporterSettings(ExporterSpecificExportSettings settings);
    ExporterSpecificExportSettings getExporterSettings();
}
