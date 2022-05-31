package gsrs.imports;

import java.io.InputStream;
import java.util.stream.Stream;

/*
Creates a set of domain entity from data in an InputStream
 */
public interface ImportAdapter<T> {
    Stream<T> parse(InputStream is, String fileEncoding);
}
