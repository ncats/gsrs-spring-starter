package ix.core.search.bulk;

import java.util.ArrayList;
import java.util.List;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class SearchResultSummaryRecord {

	private String searchTerm;
        private String modifiedSearchTerm;
	private List<MatchView> records;

	public SearchResultSummaryRecord(String searchTerm, String modifiedSearchTerm) {
		this.searchTerm = searchTerm;
		this.modifiedSearchTerm=modifiedSearchTerm;
		records = new ArrayList<MatchView>();
	}
}
