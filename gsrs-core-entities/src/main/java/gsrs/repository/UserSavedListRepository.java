package gsrs.repository;

import java.util.List;

import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import ix.core.models.UserSavedList;

@Repository
@Transactional
public interface UserSavedListRepository extends GsrsRepository<UserSavedList, Long> {
	
	@Query("select name from UserSavedList where user_id = ?1 order by name")	
	public List<String> getUserSearchResultListsByUserId(Long userId);
	
	@Query("select name from UserSavedList order by name")	
	public List<String> getAllUserSearchResultLists();
			
	@Modifying
	@Transactional
	@Query("delete from UserSavedList ubsr where user_id = ?1 and name = ?2")
	public void removeUserSearchResultList(Long userId, String listName);
	
	@Query("select list from UserSavedList where user_id = ?1 and name = ?2")
	public String getUserSavedBulkSearchResult(Long userId, String listName);
	
	@Query("select count(*) from UserSavedList where user_id = ?1 and name = ?2")
	public int userSavedBulkSearchResultExists(Long userId, String listName);
		
	@Modifying
	@Transactional
	@Query("update UserSavedList set list = ?3 where user_id = ?1 and name = ?2")
	public void updateUserSavedBulkSearchResult(Long userId, String listName, String listString);
	
}
