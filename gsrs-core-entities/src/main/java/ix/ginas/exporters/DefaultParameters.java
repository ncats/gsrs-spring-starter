package ix.ginas.exporters;

import com.fasterxml.jackson.databind.JsonNode;
import ix.core.models.Group;

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

public class DefaultParameters implements ExporterFactory.Parameters {
        private final OutputFormat format;

        private final boolean publicOnly;

        private JsonNode detailedParameters=null;

        private String username;

        public DefaultParameters(OutputFormat format, boolean publicOnly) {
            Objects.requireNonNull(format);
            this.format = format;
            this.publicOnly = publicOnly;
        }

    // publicOnly is deprecated; rely on scrubber instead.

    public DefaultParameters(OutputFormat format, boolean publicOnly, JsonNode detailedParameters) {
        Objects.requireNonNull(format);
        this.format = format;
        this.publicOnly = publicOnly;
        this.detailedParameters=detailedParameters;
    }

    @Override
        public OutputFormat getFormat() {
            return format;
        }

        @Override
        public boolean publicOnly() {
            return publicOnly;
        }

        @Override
        public JsonNode detailedParameters() {
            return detailedParameters;
        }

    @Override
    public String getUsername() {
        return this.username;
    }

    @Override
    public void setUsername(String username) {
            this.username=username;
    }
}
