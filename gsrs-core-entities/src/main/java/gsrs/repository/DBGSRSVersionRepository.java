package gsrs.repository;

import org.springframework.data.jpa.repository.Query;

import ix.core.models.DBGSRSVersion;

public interface DBGSRSVersionRepository extends GsrsRepository<DBGSRSVersion, Long>{
	
	@Query("select max(versionInfo) from DBGSRSVersion where entity = ?1")
	String findMaxVersions(String entity);
}
