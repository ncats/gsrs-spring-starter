package gsrs.repository;

import java.util.List;

import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import ix.core.models.UserBulkSearchResult;

@Repository
@Transactional
public interface UserBulkSearchResultRepository extends GsrsRepository<UserBulkSearchResult, Long> {
	
	@Query("select name from UserBulkSearchResult where user_id = ?1 order by name")	
	public List<String> getUserSearchResultListsByUserId(Long userId);
	
	@Query("select name from UserBulkSearchResult order by name")	
	public List<String> getAllUserSearchResultLists();
			
	@Modifying
    @Transactional
    @Query("delete from UserBulkSearchResult ubsr where user_id = ?1 and name = ?2")
    public void removeUserSearchResultList(Long userId, String listName);	
	
	@Query("select list from UserBulkSearchResult where user_id = ?1 and name = ?2")
	public String getUserSavedBulkSearchResult(Long userId, String listName);
	
	@Modifying
    @Transactional
	@Query("update UserBulkSearchResult set list = ?3 where user_id = ?1 and name = ?2")
	public String updateUserSavedBulkSearchResult(Long userId, String listName, String listString);
	
}
