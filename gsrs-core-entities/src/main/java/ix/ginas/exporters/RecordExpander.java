package ix.ginas.exporters;

import java.util.stream.Stream;

/*
Take a record and return a set of related record
 */
public interface RecordExpander<T> {

    Stream<T> expandRecord(T record);
}
