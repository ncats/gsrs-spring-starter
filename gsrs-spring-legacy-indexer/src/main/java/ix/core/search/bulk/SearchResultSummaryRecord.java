package ix.core.search.bulk;

import java.util.ArrayList;
import java.util.List;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class SearchResultSummaryRecord {

	private String searchTerm;
	private List<MatchView> records;

	public SearchResultSummaryRecord(String term) {
		searchTerm = term;
		records = new ArrayList<MatchView>();
	}
}
