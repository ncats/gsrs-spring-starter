package gsrs.stagingarea.repository;

import gsrs.stagingarea.model.RawImportData;
import gsrs.repository.GsrsVersionedRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Repository
public interface RawImportDataRepository extends GsrsVersionedRepository<RawImportData, UUID> {

    @Modifying
    @Transactional
    @Query("delete from RawImportData i where i.recordId = ?1")
    void deleteByRecordId(UUID recordId);

}
