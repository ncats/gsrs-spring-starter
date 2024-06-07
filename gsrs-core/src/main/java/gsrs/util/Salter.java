package gsrs.util;

public interface Salter {

    void setHasher(Hasher hasher);

    String generateSalt() throws Exception;

    boolean mayBeOneOfMine(String testHash);
}
