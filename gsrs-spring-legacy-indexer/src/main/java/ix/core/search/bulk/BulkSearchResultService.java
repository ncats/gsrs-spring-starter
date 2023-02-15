package ix.core.search.bulk;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import gsrs.repository.BulkSearchResultKeyRepository;
import gsrs.repository.PrincipalRepository;
import gsrs.repository.UserBulkSearchResultRepository;
import ix.core.models.BulkSearchResultKey;
import ix.core.models.Principal;
import ix.core.models.UserBulkSearchResult;

@Service
public class BulkSearchResultService {
	
	@Autowired
	public UserBulkSearchResultRepository userBulkSearchResultRepository;
	
	@Autowired
	public BulkSearchResultKeyRepository bulkSearchResultKeyRepository;
	
	@Autowired
	public PrincipalRepository principalRepository;
	
	
	public static enum Operation {
	    ADD,REMOVE 
	}
	
	//All the validation checking of parameters are done at the controller
	public List<String> getUserSearchResultLists(String userName){
		Principal user = principalRepository.findDistinctByUsernameIgnoreCase(userName);
		if(user == null)
			return new ArrayList<String>();
		return 	getUserSearchResultLists(user.id);
	} 
	
	public List<String> getUserSearchResultLists(Long userId){
		return 	userBulkSearchResultRepository.getUserSearchResultListsByUserId(userId);
	} 
	
	public List<String> getAllUserSearchResultLists(){
		return 	userBulkSearchResultRepository.getAllUserSearchResultLists();
	}
	
	public List<String> getListNamesByKey(String key, Long userId){
		return bulkSearchResultKeyRepository.getAllListNamesFromKey(key, userId);
	}
	
	public void removeUserSearchResultList(Long userId, String listName) {
		userBulkSearchResultRepository.removeUserSearchResultList(userId, listName);
		bulkSearchResultKeyRepository.removeList(userId, listName);
	}
	
	
	public List<String> getUserSavedBulkSearchResultListContent(String userName, String listName, int top, int skip){
		Principal user = principalRepository.findDistinctByUsernameIgnoreCase(userName);
		if(user == null)
			return new ArrayList<String>();
		
		return getUserSavedBulkSearchResultListContent(user.id, listName, top, skip);		
	}
	
	
	public List<String> getUserSavedBulkSearchResultListContent(Long userId, String listName, int top, int skip){
		List<String> keyList = new ArrayList<String>();
		String listString = userBulkSearchResultRepository.getUserSavedBulkSearchResult(userId, listName);
		
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
	
	public List<String> getUserSavedBulkSearchResultListContent(String userName, String listName){
		Principal user = principalRepository.findDistinctByUsernameIgnoreCase(userName);
		List<String> keyList = new ArrayList<String>();
		if(user != null)
			keyList = getUserSavedBulkSearchResultListContent(user.id, listName);
		return keyList;		
	}
		
	public List<String> getUserSavedBulkSearchResultListContent(Long userId, String listName){
		List<String> keyList = new ArrayList<String>();
		String listString = userBulkSearchResultRepository.getUserSavedBulkSearchResult(userId, listName);
		if(listString == null || listString.trim().isEmpty())
			return keyList;
		
		keyList = Arrays.asList(listString.split(","));
		return keyList;
	}
	
	public void saveBulkSearchResultList(String userName, String listName, List<String> keyList ) {
		Principal user = principalRepository.findDistinctByUsernameIgnoreCase(userName);
		if(user == null)
			return;
		
		List<String> processedList = keyList.stream()
				.filter(s->s.length()>0)
				.map(s->s.trim())
				.collect(Collectors.toList());
		
		String listString = processedList.stream()				
				.reduce("", (substring, key)-> substring.concat(","+key));
		listString = listString.substring(listString.indexOf(",")+1);
		UserBulkSearchResult record = new UserBulkSearchResult(user, listName, listString);
		userBulkSearchResultRepository.saveAndFlush(record);
		
		//todo: use batch insert here
		for(String key: processedList)	{		
			bulkSearchResultKeyRepository.saveAndFlush(new BulkSearchResultKey(key, user, listName));
		}		
	}	
	
	public void deleteBulkSearchResultList(String userName, String listName) {
		
		Principal user = principalRepository.findDistinctByUsernameIgnoreCase(userName);
		if(user == null)
			return; 
		userBulkSearchResultRepository.removeUserSearchResultList(user.id, listName);
				
		bulkSearchResultKeyRepository.removeList(user.id, listName);
	}
	
	public List<String> updateBulkSearchResultList(Long userId, String listName, List<String> keyList, Operation operation) {
		List<String> list;
		List<String> changeSet = new ArrayList<>();
		String listString = userBulkSearchResultRepository.getUserSavedBulkSearchResult(userId, listName);
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
	    userBulkSearchResultRepository.updateUserSavedBulkSearchResult(userId, listName, resultString);	
	    
	    return changeSet; 
	    	    
	}
	
	public void updateBulkSearchResultKey(Principal user, String listName, Operation operation, List<String> changeSet) {
		
		switch(operation) {
		case ADD:
			for(String string: changeSet) {
				bulkSearchResultKeyRepository.saveAndFlush(new BulkSearchResultKey(string, user, listName));
			}
			break;
		case REMOVE:
			for(String string: changeSet) {
				bulkSearchResultKeyRepository.removeKey(string, user.id, listName);
			}
			break;	
		default:
			return ;					
		}			
		
	}
	
	public boolean updateBulkSearchResultList(String userName, String listName, List<String> keyList, Operation operation) {
		Principal user = principalRepository.findDistinctByUsernameIgnoreCase(userName);
		if(user == null)
			return false; 
		List<String> changeSet = updateBulkSearchResultList(user.id, listName, keyList, operation);	
		if(changeSet.size()>0) {
			updateBulkSearchResultKey(user, listName, operation, changeSet);
			return true;
		}else {
			return false;
		}
	}	
	
	public static String getIndexedValue(String userName, String listName) {
		return userName+":"+listName;	
	}
	
	
	public static String getUserListName(String indexedValue, String userName) {
		if(!indexedValue.contains(":"))
			return "";
		String [] values = indexedValue.split(":");
		if(values.length != 2)
			return "";
		if(values[1].equalsIgnoreCase(userName))
			return values[0];
		else 
			return "";
	}
}


