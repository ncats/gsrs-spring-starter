package gsrs.repository;

import java.util.List;

import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.transaction.annotation.Transactional;

import ix.core.models.BulkSearchResultKey;

public interface BulkSearchResultKeyRepository extends GsrsRepository<BulkSearchResultKey, Long> {
			
	@Query("select list_name from BulkSearchResultKey where key = ?1 and user_id = ?2 order by list_name")	
	public List<String> getAllListNamesFromKey(String key, Long userId);
			
	@Modifying
    @Transactional
    @Query("delete from BulkSearchResultKey ubsr where key = ?1 and user_id = ?2 and list_name = ?3")
    public void removeKey(String key, Long userId, String listName);
	
	@Modifying
    @Transactional
    @Query("delete from BulkSearchResultKey ubsr where user_id = ?1 and list_name = ?2")
    public void removeList(Long userId, String listName);
	

}
