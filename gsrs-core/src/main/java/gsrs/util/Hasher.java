package gsrs.util;

public interface Hasher {

    String getHashType();

    String hash(String... values);
}
