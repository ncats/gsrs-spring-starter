package gsrs.repository;

import java.util.List;

import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.transaction.annotation.Transactional;

import ix.core.models.KeyUserList;

public interface KeyUserListRepository extends GsrsRepository<KeyUserList, Long> {
			
	@Query("select distinct listName from KeyUserList where entityKey = ?1 and user_id = ?2 order by listName")	
	public List<String> getAllListNamesFromKey(String key, Long userId);
	
	@Query("select list from KeyUserList list where list.entityKey = ?1 order by list.listName")	
	public List<KeyUserList> getAllListNamesFromKey(String key);
			
	@Modifying
	@Transactional
	@Query("delete from KeyUserList where entityKey = ?1 and user_id = ?2 and listName = ?3")
	public void removeKey(String key, Long userId, String listName);
	
	@Modifying
	@Transactional
	@Query("delete from KeyUserList where user_id = ?1 and listName = ?2")
	public void removeList(Long userId, String listName);
	
	

}
