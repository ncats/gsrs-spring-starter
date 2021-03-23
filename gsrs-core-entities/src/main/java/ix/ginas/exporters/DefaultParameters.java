package ix.ginas.exporters;

import java.util.Objects;

public class DefaultParameters implements ExporterFactory.Parameters {
        private final OutputFormat format;

        private final boolean publicOnly;

        public DefaultParameters(OutputFormat format, boolean publicOnly) {
            Objects.requireNonNull(format);
            this.format = format;
            this.publicOnly = publicOnly;
        }

        @Override
        public OutputFormat getFormat() {
            return format;
        }

        @Override
        public boolean publicOnly() {
            return publicOnly;
        }
    }
