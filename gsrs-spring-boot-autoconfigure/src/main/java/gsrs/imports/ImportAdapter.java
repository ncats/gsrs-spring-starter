package gsrs.imports;

import java.io.InputStream;
import java.util.stream.Stream;

public interface ImportAdapter<T> {
    Stream<T> parse(InputStream is);
}
