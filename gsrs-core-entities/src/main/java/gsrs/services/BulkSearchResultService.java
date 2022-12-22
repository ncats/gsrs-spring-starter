package gsrs.services;

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
	
	
	public enum Operation {
	    ADD,REMOVE 
	}
	
	public List<String> getUserSearchResultLists(Long userId){
		return 	userBulkSearchResultRepository.getUserSearchResultListsByUserId(userId);
	} 
	
	public void removeUserSearchResultList(Long userId, String listName) {
		userBulkSearchResultRepository.removeUserSearchResultList(userId, listName);
	}
	
	
	//todo: add pagination
	public List<String> getUserSavedBulkSearchResult(Long userId, String listName){
		List<String> keyList;
		String listString = userBulkSearchResultRepository.getUserSavedBulkSearchResult(userId, listName);
		keyList = Arrays.asList(listString.split(","));
		return keyList;
	}
	
	public void saveBulkSearchResult(String username, String listName, List<String> keyList ) {
		Principal user = principalRepository.findDistinctByUsernameIgnoreCase(username);
		String listString = keyList.stream()
				.filter(s->s.length()>0)
				.reduce("", (substring, key)-> substring.concat(","+key));
		listString = listString.substring(listString.indexOf(","));
		UserBulkSearchResult record = new UserBulkSearchResult(user, listName, listString);
		userBulkSearchResultRepository.saveAndFlush(record);
	}
	
	public void updateBulkSearchResultList(Long userId, String listName, List<String> keyList, Operation operation) {
		List<String> list;
		String listString = userBulkSearchResultRepository.getUserSavedBulkSearchResult(userId, listName);
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
					break;
				}				
			}		
		}
		
	    List<String> sortedList = new ArrayList<>(sortedSet);
	    String resultString = sortedList.stream().reduce("", (substring, key)-> substring.concat(","+key));
	    resultString = resultString.substring(listString.indexOf(","));
	    userBulkSearchResultRepository.updateUserSavedBulkSearchResult(userId, listName, listString);		
	}
	
}
