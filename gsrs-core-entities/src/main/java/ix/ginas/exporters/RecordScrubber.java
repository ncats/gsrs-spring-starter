package ix.ginas.exporters;

import java.util.Optional;

/*
As of 19 August, this is a shot in the dark
 */
public interface RecordScrubber<T> {
    Optional<T> scrub(T object);

}
