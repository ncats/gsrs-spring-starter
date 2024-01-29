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

    @Query("select name from UserSavedList where principal.id = ?1 and kind = ?2 order by name")
    public List<String> getUserSearchResultListsByUserId(Long userId, String kind);

    @Query("select name from UserSavedList where kind = ?1 order by name")
    public List<String> getAllUserSearchResultLists(String kind);

    @Modifying
    @Transactional
    @Query("delete from UserSavedList ubsr where principal.id = ?1 and name = ?2 and kind = ?3")
    public void removeUserSearchResultList(Long userId, String listName, String kind);

    @Query("select list from UserSavedList where principal.id = ?1 and name = ?2 and kind = ?3")
    public String getUserSavedBulkSearchResult(Long userId, String listName, String kind);

    @Query("select count(*) from UserSavedList where principal.id = ?1 and name = ?2 and kind = ?3")
    public int userSavedBulkSearchResultExists(Long userId, String listName, String kind);

    @Modifying
    @Transactional
    @Query("update UserSavedList set list = ?3 where principal.id = ?1 and name = ?2 and kind = ?4")
    public void updateUserSavedBulkSearchResult(Long userId, String listName, String listString, String kind);
}
