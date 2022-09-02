package gsrs.holdingarea.repository;

import gsrs.holdingarea.model.ImportValidation;
import gsrs.repository.GsrsVersionedRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Repository
public interface ImportValidationRepository extends GsrsVersionedRepository<ImportValidation, UUID> {

    @Query("select i from ImportValidation i where i.instanceId = ?1")
    List<ImportValidation> retrieveValidationsByRecordId(UUID instanceId);

    @Modifying
    @Transactional
    @Query("delete from ImportValidation i where i.instanceId = ?1")
    void deleteByInstanceId(UUID instanceId);

}
