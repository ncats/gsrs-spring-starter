package gsrs.repository;

import ix.core.models.FileData;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface FileDataRepository extends JpaRepository<FileData, UUID> {

    Optional<FileData> findBySha1(String sha1);
}
