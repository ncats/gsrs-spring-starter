package gsrs.util;

public interface Salter {

    void setHasher(Hasher hasher);

    String generateSalt();

    boolean mayBeOneOfMine(String testHash);
}
