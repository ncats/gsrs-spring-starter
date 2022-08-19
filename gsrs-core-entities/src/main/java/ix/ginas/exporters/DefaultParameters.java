package ix.ginas.exporters;

import ix.core.models.Group;

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

public class DefaultParameters implements ExporterFactory.Parameters {
        private final OutputFormat format;

        private final boolean publicOnly;

        private RecordScrubber recordScrubber;

        private Set<String> scrubberGroups = new HashSet<>();

        public DefaultParameters(OutputFormat format, boolean publicOnly) {
            Objects.requireNonNull(format);
            this.format = format;
            this.publicOnly = publicOnly;
        }

    public DefaultParameters(OutputFormat format, boolean publicOnly, RecordScrubber scrubber, Set<String> scrubberGroups) {
        Objects.requireNonNull(format);
        this.format = format;
        this.publicOnly = publicOnly;
        this.recordScrubber=scrubber;
        this.scrubberGroups=scrubberGroups;
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
        public RecordScrubber getScrubber() {
            return recordScrubber;
        }

        @Override
        public Set<String> getScrubberGroups(){
            return scrubberGroups;
        }


        public void setRecordScrubber(RecordScrubber recordScrubber) {
            this.recordScrubber = recordScrubber;
        }

        public void setScrubberGroups(Set<String> scrubberGroups) {
            this.scrubberGroups = scrubberGroups;
        }
}
