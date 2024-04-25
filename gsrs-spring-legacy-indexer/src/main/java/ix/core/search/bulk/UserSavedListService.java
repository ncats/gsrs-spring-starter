package ix.core.search.bulk;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import gsrs.repository.KeyUserListRepository;
import gsrs.repository.PrincipalRepository;
import gsrs.repository.UserSavedListRepository;
import ix.core.models.KeyUserList;
import ix.core.models.Principal;
import ix.core.models.UserSavedList;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class UserSavedListService {
	
	@Autowired
	public UserSavedListRepository userSavedListRepository;
	
	@Autowired
	public KeyUserListRepository keyUserListRepository;
	
	@Autowired
	public PrincipalRepository principalRepository;
	
	
	public static enum Operation {
	    ADD,REMOVE 
	}
	
	private final String anonymousUser="anonymousUser";
	
	//All the validation checking of parameters are done at the controller
	public List<String> getUserSearchResultLists(String userName, String kind){
		Principal user = principalRepository.findDistinctByUsernameIgnoreCase(userName);
		if(user == null) {
			if(!userName.equalsIgnoreCase(anonymousUser)) {
				log.info("User saved list service: cannot find user " + userName);
			}
			return new ArrayList<String>();
		}
		return 	getUserSearchResultLists(user.id, kind);
	} 
	
	public List<String> getUserSearchResultLists(Long userId, String kind){
		
		return 	userSavedListRepository.getUserSearchResultListsByUserId(userId, kind);
	} 
	
	public List<String> getAllUserSearchResultLists(String kind){
		return 	userSavedListRepository.getAllUserSearchResultLists(kind);
	}
	
	public List<String> getListNamesByKey(String key, Long userId, String kind){
		return keyUserListRepository.getAllListNamesFromKey(key, userId, kind);
	}
	
	public void removeUserSearchResultList(Long userId, String listName, String kind) {
		userSavedListRepository.removeUserSearchResultList(userId, listName, kind);
		keyUserListRepository.removeList(userId, listName, kind);
	}
	
	
	public List<String> getUserSavedBulkSearchResultListContent(String userName, String listName, int top, int skip, String kind){
		Principal user = principalRepository.findDistinctByUsernameIgnoreCase(userName);
		if(user == null) {
			if(!userName.equalsIgnoreCase(anonymousUser)) {
				log.info("User saved list service: cannot find user " + userName);
			}
			return new ArrayList<String>();
		}
		
		return getUserSavedBulkSearchResultListContent(user.id, listName, top, skip, kind);		
	}
	
	
	public List<String> getUserSavedBulkSearchResultListContent(Long userId, String listName, int top, int skip, String kind){
		List<String> keyList = new ArrayList<String>();
		String listString = userSavedListRepository.getUserSavedBulkSearchResult(userId, listName, kind);
		
		if(listString == null || listString.trim().isEmpty())
			return keyList;
		
		keyList = Arrays.asList(listString.split(","));
		
		if(skip >= keyList.size())
			return new ArrayList<String>();
		
		int endIndex = keyList.size();
		if(top+skip < endIndex)
			endIndex = top+skip;
		
		return keyList.subList(skip, endIndex);
	}
	
	public List<String> getUserSavedBulkSearchResultListContent(String userName, String listName, String kind){
		List<String> keyList = new ArrayList<String>();
		Principal user = principalRepository.findDistinctByUsernameIgnoreCase(userName);
		if(user == null) {
			if(!userName.equalsIgnoreCase(anonymousUser)) {
				log.info("User saved list service: cannot find user " + userName);
			}
			return keyList;
		}
		
		keyList = getUserSavedBulkSearchResultListContent(user.id, listName, kind);
		return keyList;		
	}
		
	public List<String> getUserSavedBulkSearchResultListContent(Long userId, String listName, String kind){
		List<String> keyList = new ArrayList<String>();
		String listString = userSavedListRepository.getUserSavedBulkSearchResult(userId, listName, kind);
		if(listString == null || listString.trim().isEmpty())
			return keyList;
		
		keyList = Arrays.asList(listString.split(","));
		return keyList;
	}
	
	public boolean userListExists(String userName, String listName, String kind) {
		Principal user = principalRepository.findDistinctByUsernameIgnoreCase(userName);
		if(user == null) {
			if(!userName.equalsIgnoreCase(anonymousUser)) {
				log.info("User saved list service: cannot find user " + userName);
			}
			return false;
		}		
		if(userSavedListRepository.userSavedBulkSearchResultExists(user.id, listName, kind) > 0) {			
			return true;
		}else 
			return false;		
	} 
	
	// Error message or empty string to indicate no error
	public String validateUsernameAndListname(String userName, String listName, String kind) {
		Principal user = principalRepository.findDistinctByUsernameIgnoreCase(userName);
		if(user == null) {
			if(!userName.equalsIgnoreCase(anonymousUser)) {
				log.info("User saved list service: cannot find user " + userName);
			}
			return "Cannot find user " + userName;
		}
		
		if(userSavedListRepository.userSavedBulkSearchResultExists(user.id, listName, kind) > 0) {			
			return "User list with name as " + userName + " already exists.";
		}
		else 
			return "";		
	} 
	
	
	public void createBulkSearchResultList(String userName, String listName, List<String> keyList, String kind ) {		
		
		Principal user = principalRepository.findDistinctByUsernameIgnoreCase(userName);		
		
		List<String> processedList = keyList.stream()
				.filter(s->s.length()>0)
				.map(s->s.trim())
				.collect(Collectors.toList());
		
		String listString = processedList.stream()				
				.reduce("", (substring, key)-> substring.concat(","+key));
		listString = listString.substring(listString.indexOf(",")+1);
		UserSavedList record = new UserSavedList(user, listName, listString, kind);
		userSavedListRepository.saveAndFlush(record);
		
		//todo: use batch insert here
		for(String key: processedList)	{		
			keyUserListRepository.saveAndFlush(new KeyUserList(key, user, listName, kind));
		}		
	}	
		
	public void deleteBulkSearchResultList(String userName, String listName, String kind) {
		
		Principal user = principalRepository.findDistinctByUsernameIgnoreCase(userName);
		if(user == null) {
			if(!userName.equalsIgnoreCase(anonymousUser)) {
				log.info("User saved list service: cannot find user " + userName);
			}
			return; 
		}
		userSavedListRepository.removeUserSearchResultList(user.id, listName, kind);
				
		keyUserListRepository.removeList(user.id, listName, kind);
	}
	
	public List<String> updateBulkSearchResultList(Long userId, String listName, List<String> keyList, 
			Operation operation, String kind) {
		List<String> list;
		List<String> changeSet = new ArrayList<>();
		String listString = userSavedListRepository.getUserSavedBulkSearchResult(userId, listName, kind);
		if(listString == null || listString.trim().isEmpty())
			return changeSet;
		list = Arrays.asList(listString.split(","));
		SortedSet<String> sortedSet = new TreeSet<>(list);
		
		
		switch(operation) {
			case ADD:
				for(String string: keyList) {
					if(!sortedSet.contains(string)) {
						sortedSet.add(string);
						changeSet.add(string);
					}
				}
				break;
			case REMOVE:
				for(String string: keyList) {
					if(sortedSet.contains(string)) {
						sortedSet.remove(string);
						changeSet.add(string);
					}
				}
				break;	
			default:
				return changeSet;					
			}				
					
		
		List<String> sortedList = new ArrayList<>(sortedSet);
	    String resultString = sortedList.stream().reduce("", (substring, key)-> substring.concat(","+key));
	    resultString = resultString.substring(resultString.indexOf(",")+1);
	    userSavedListRepository.updateUserSavedBulkSearchResult(userId, listName, resultString, kind);	
	    
	    return changeSet; 
	    	    
	}
	
	public void updateBulkSearchResultKey(Principal user, String listName, Operation operation, List<String> changeSet, String kind) {
		
		switch(operation) {
		case ADD:
			for(String string: changeSet) {
				keyUserListRepository.saveAndFlush(new KeyUserList(string, user, listName, kind));
			}
			break;
		case REMOVE:
			for(String string: changeSet) {
				keyUserListRepository.removeKey(string, user.id, listName, kind);
			}
			break;	
		default:
			return ;					
		}			
		
	}
	
	public boolean updateBulkSearchResultList(String userName, String listName, List<String> keyList, Operation operation, String kind) {
		Principal user = principalRepository.findDistinctByUsernameIgnoreCase(userName);
		if(user == null) {
			if(!userName.equalsIgnoreCase(anonymousUser)) {
				log.info("User saved list service: cannot find user " + userName);
			}
			return false; 
		}
		List<String> changeSet = updateBulkSearchResultList(user.id, listName, keyList, operation, kind);	
		if(changeSet.size()>0) {
			updateBulkSearchResultKey(user, listName, operation, changeSet, kind);
			return true;
		}else {
			return false;
		}
	}	
	
	public static String getIndexedValue(String userName, String listName) {
		return userName+":"+listName;	
	}
	
	@Data
	public static class UserListIndexedValue{
		
		String userName;
		String listName;
		
		public UserListIndexedValue(String userNameString, String listNameString) {
			userName = userNameString;
			listName = listNameString;			
		}
	}
	
	public static UserListIndexedValue getUserNameAndListNameFromIndexedValue(String value) {
		List<String> resultList = Arrays.asList(value.split(":"));
		if(resultList.size()!=2)
			return new UserListIndexedValue("","");		
		return new UserListIndexedValue(resultList.get(0), resultList.get(1));
	}	
	
	public static String getUserNameFromIndexedValue(String value) {
		List<String> resultList = Arrays.asList(value.split(":"));
		if(resultList.size()!=2)
			return "";
		return resultList.get(0);			
	}	
}


