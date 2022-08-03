package gsrs.holdingarea.repository;

import gsrs.holdingarea.model.RawImportData;
import gsrs.repository.GsrsVersionedRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface RawImportDataRepository extends GsrsVersionedRepository<RawImportData, UUID> {
}
