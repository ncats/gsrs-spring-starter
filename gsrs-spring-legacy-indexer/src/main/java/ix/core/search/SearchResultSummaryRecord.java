package ix.core.search;

import java.util.ArrayList;
import java.util.List;

import ix.core.util.EntityUtils.Key;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class SearchResultSummaryRecord {	
		
		private String searchTerm;		
		private List<Key> recordUNIIs;
		private List<String> recordNames;
		
		public SearchResultSummaryRecord(String term){
			searchTerm = term;
			recordUNIIs = new ArrayList<Key>();
			recordNames = new ArrayList<String>();
		}

}
