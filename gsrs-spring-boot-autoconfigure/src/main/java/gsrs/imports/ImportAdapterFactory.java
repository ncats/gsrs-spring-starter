package gsrs.imports;

import com.fasterxml.jackson.databind.JsonNode;
import gsrs.controller.AbstractImportSupportingGsrsEntityController;

import java.io.InputStream;
import java.util.List;

/*
Creates a class that can process data from a file and convert it into domain objects
using Actions for individual parts of the data
 */
public interface ImportAdapterFactory<T> {
    /**
     * Returns the name of the adapter, to be used for lookups and registration
     * with a registry of {@link ImportAdapterFactory} possibilities.
     *
     * @return "Key" name of adapter factory.
     */
    String getAdapterName();

    /**
     * Returns list of supported file extensions this import adapter may support. This list
     * does not have to be exhaustive, but will serve as a hint when suggesting adapters from
     * a set of possible adapters.
     *
     * @return list of supported file extensions
     */
    List<String> getSupportedFileExtensions();

    /**
     * Creates an {@link ImportAdapter} based on the supplied {@link JsonNode}, which can
     * encode information about the initialization of the adapter. The adapterSettings typically
     * contain information like how individual fields of an input data stream map to fields in
     * an output record.
     *
     * @param adapterSettings initialization settings which can be used to configure an {@link ImportAdapter}
     * @return
     */
    ImportAdapter<T> createAdapter(JsonNode adapterSettings);

    /**
     * <p>
     * Partially or completely read a given {@link InputStream} and produce {@link ImportAdapterStatistics}
     * as hints for how to create proper adapterSettings to be used in {@link #createAdapter(JsonNode)}.
     * </p>
     *
     * <p>
     * {@link ImportAdapterStatistics} has 2 intended elements:
     *  <ol>
     *  <li>adapterSettings -- a "best guess" set of initialization parameters</li>
     *  <li>adapterSchema  -- a higher-level model for what input options are available</li>
     *  </ol>
     * </p>
     *
     * @param is the InputStream of real or example data to be analyzed
     * @return {@link ImportAdapterStatistics} object giving some statistics and suggestions for how to configure
     * the {@link ImportAdapter} with the {@link #createAdapter(JsonNode)} method.
     */
    public ImportAdapterStatistics predictSettings(InputStream is);

    public void setFileName(String fileName);
    public String getFileName();

    default void initialize() throws IllegalStateException {

    }

    /**
     * Returns the name of the holding service class to be called at the end of the import process
     *
     * @return "Key" name of holding service class (that implements interface HoldingService
     */
    String getHoldingServiceName();
}