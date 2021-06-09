package gsrs.service;

import ix.ginas.exporters.ExportDir;
import ix.ginas.exporters.ExportMetaData;
import ix.ginas.exporters.ExportProcess;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;
import java.util.stream.Stream;

public interface ExportService {

    Optional<ExportDir.ExportFile<ExportMetaData>> getFile(String username, String filename) throws IOException;

    List<ExportMetaData> getExplicitExportMetaData(String username);

    <T> ExportProcess createExport(ExportMetaData metaData, Supplier<Stream<T>> entitySupplier) throws Exception;

    Optional<ExportMetaData> getStatusFor(String username, String downloadID);

    void remove(ExportMetaData meta);
}
