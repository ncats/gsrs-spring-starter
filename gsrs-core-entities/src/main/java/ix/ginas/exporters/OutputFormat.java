package ix.ginas.exporters;

import com.fasterxml.jackson.databind.JsonNode;

import java.util.Objects;

/**
 * The OutputFormat the exporter should be.
 *
 */
public class OutputFormat {
    private final String extension;
    private final String displayname;
    private JsonNode parameterSchema;

    public JsonNode getParameterSchema() {
        return parameterSchema;
    }

    public void setParameterSchema(JsonNode parameterSchema) {
        this.parameterSchema = parameterSchema;
    }

    public OutputFormat(String extension, String displayname) {
        Objects.requireNonNull(extension);
        Objects.requireNonNull(displayname);

        this.extension = extension;
        this.displayname = displayname;
    }

    public String getExtension(){
        return extension;
    }

    public String getDisplayName(){
        return displayname;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        OutputFormat that = (OutputFormat) o;

        if (!extension.equals(that.extension)) return false;
        return displayname.equals(that.displayname);

    }

    @Override
    public int hashCode() {
        int result = extension.hashCode();
        result = 31 * result + displayname.hashCode();
        return result;
    }
}
