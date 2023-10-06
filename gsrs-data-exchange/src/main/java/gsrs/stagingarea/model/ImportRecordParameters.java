package gsrs.stagingarea.model;

import com.fasterxml.jackson.databind.JsonNode;
import ix.core.models.Principal;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.InputStream;
import java.util.UUID;

/**
 * ImportRecordParameters packages a number of parameters for use in creating a record in the staging area.
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class ImportRecordParameters {
    /**
     * serialized domain object
     */
    private String jsonData;
    /**
     * file name (or other source) from which the entity was extracted
     */
    private String source;

    private byte[] rawData;

    /**
     * Data in its original format
     */
    private InputStream rawDataSource;

    /**
     * qualified name of the class of domain object being imported
     */
    private String entityClassName;

    /**
     * mime type of imported data
     */
    private String formatType;
    /**
     * does not appear to be used, as of June 2023
     */
    private UUID recordId;

    /**
     * Name of import adapter used
     */
    private String adapterName;

    /**
     * A way of passing additional parameters
     * We use an ObjectNode called rarelyUsedSettings that may contain text nodes:
     *  skipValidation - to allow turning off data validation
     *  skipIndexing- to turn off indexing (for faster searching)
     *  skipMatching - to turn off matching processing
     */
    private JsonNode settings;

    /**
     * The user performing the data load
     */
    private Principal importingUser;
}
