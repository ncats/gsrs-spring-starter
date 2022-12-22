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
	
	@Query("select listName from UserBulkSearchResult ubsr where ubsr.user_id=?1")	
	public List<String> getUserSearchResultListsByUserId(Long userId);
	
	@Query("select listName from UserBulkSearchResult ubsr where ubsr.username=?1")	
	public List<String> getUserSearchResultListsByUserName(String name);
		
	@Modifying
    @Transactional
    @Query("delete from UserBulkSearchResult ubsr where ubsr.user_id = ?1 and ubsr.name = ?2")
    public void removeUserSearchResultList(Long userId, String listName);

	@Query("select list from UserBulkSearchResult ubsr where ubsr.user_id = ?1 and ubsr.name = ?2")
	public String getUserSavedBulkSearchResult(Long userId, String listName);
	
	@Modifying
    @Transactional
	@Query("update UserBulkSearchResult ubsr set list = ?3 where ubsr.user_id = ?1 and ubsr.name = ?2")
	public String updateUserSavedBulkSearchResult(Long userId, String listName, String listString);
	
	
}
