package ix.ginas.exporters;

public interface RecordScrubberFactory<T> {

    RecordScrubber<T> createScrubber(ScrubberExportSettings settings);
}
