package gsrs.cv.api;

import java.io.IOException;
import java.util.Optional;

public interface ControlledVocabularyApi {
    <T extends AbstractGsrsControlledVocabularyDTO> Optional<T> findByDomain(String domain) throws IOException;

    long count() throws IOException;

    <T extends AbstractGsrsControlledVocabularyDTO> Optional<T> findByResolvedId(String anyKindOfId) throws IOException;

    <T extends AbstractGsrsControlledVocabularyDTO> Optional<T> findById(Long id) throws IOException;

    boolean existsById(Long id) throws IOException;

    <T extends AbstractGsrsControlledVocabularyDTO> T create(T dto) throws IOException;

    <T extends AbstractGsrsControlledVocabularyDTO> T update(T dto) throws IOException;
}
