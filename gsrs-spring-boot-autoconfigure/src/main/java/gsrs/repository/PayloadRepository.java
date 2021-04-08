package gsrs.repository;

import ix.core.models.Payload;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface PayloadRepository extends JpaRepository<Payload, UUID> {

    Optional<Payload> findBySha1(String sha1);
}
