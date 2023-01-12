package ix.core.search.bulk;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.SortedSet;
import java.util.TreeSet;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import gsrs.repository.PrincipalRepository;
import gsrs.repository.UserBulkSearchResultRepository;
import ix.core.models.Principal;
import ix.core.models.UserBulkSearchResult;

@Service
public class BulkSearchResultService {
	
	@Autowired
	public UserBulkSearchResultRepository userBulkSearchResultRepository;
	
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
		return 	userBulkSearchResultRepository.getUserSearchResultListsByUserId(user.id);
	} 
	
	public List<String> getUserSearchResultLists(Long userId){
		return 	userBulkSearchResultRepository.getUserSearchResultListsByUserId(userId);
	} 
	
	public List<String> getAllUserSearchResultLists(){
		return 	userBulkSearchResultRepository.getAllUserSearchResultLists();
	}
	
	public void removeUserSearchResultList(Long userId, String listName) {
		userBulkSearchResultRepository.removeUserSearchResultList(userId, listName);
	}
	
	
	//todo: add pagination
	public List<String> getUserSavedBulkSearchResultLists(Long userId, String listName){
		List<String> keyList = new ArrayList<String>();
		String listString = userBulkSearchResultRepository.getUserSavedBulkSearchResult(userId, listName);
		keyList = Arrays.asList(listString.split(","));
		return keyList;
	}
	
	public void saveBulkSearchResultList(String userName, String listName, List<String> keyList ) {
		Principal user = principalRepository.findDistinctByUsernameIgnoreCase(userName);
		if(user == null)
			return;
		String listString = keyList.stream()
				.filter(s->s.length()>0)
				.reduce("", (substring, key)-> substring.concat(","+key));
		listString = listString.substring(listString.indexOf(","));
		UserBulkSearchResult record = new UserBulkSearchResult(user, listName, listString);
		userBulkSearchResultRepository.saveAndFlush(record);
	}
	
	public void deleteBulkSearchResultList(String userName, String listName) {
		if(userName == null || listName == null || userName.trim().isEmpty() || listName.trim().isEmpty())
			return;
		Principal user = principalRepository.findDistinctByUsernameIgnoreCase(userName);
		if(user == null)
			return;
		userBulkSearchResultRepository.removeUserSearchResultList(user.id, listName);
	}
	
	
	public void deleteBulkSearchResultLists(String userName, String listName) {
		
		Principal user = principalRepository.findDistinctByUsernameIgnoreCase(userName);
		if(user == null)
			return; 
		userBulkSearchResultRepository.removeUserSearchResultList(user.id, listName);
	}
	
	public void updateBulkSearchResultList(Long userId, String listName, List<String> keyList, Operation operation) {
		List<String> list;
		String listString = userBulkSearchResultRepository.getUserSavedBulkSearchResult(userId, listName);
		if(listString == null || listString.trim().isEmpty())
			return;
		list = Arrays.asList(listString.split(","));
		SortedSet<String> sortedSet = new TreeSet<>(list);
		for(String string: keyList) {
			if(!sortedSet.contains(string)) {
				switch(operation) {
				case ADD: 
					sortedSet.add(string);
					break;
				case REMOVE:
					sortedSet.remove(string);
					break;	
				default:
					return;					
				}				
			}		
		}
		
	    List<String> sortedList = new ArrayList<>(sortedSet);
	    String resultString = sortedList.stream().reduce("", (substring, key)-> substring.concat(","+key));
	    resultString = resultString.substring(listString.indexOf(","));
	    userBulkSearchResultRepository.updateUserSavedBulkSearchResult(userId, listName, listString);	
	}
	
	public void updateBulkSearchResultList(String userName, String listName, List<String> keyList, Operation operation) {
		Principal user = principalRepository.findDistinctByUsernameIgnoreCase(userName);
		if(user == null)
			return; 
		updateBulkSearchResultList(user.id, listName, keyList, operation);	
	}	
}


