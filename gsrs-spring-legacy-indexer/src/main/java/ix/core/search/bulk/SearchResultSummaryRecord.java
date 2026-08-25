package ix.core.search.bulk;

import java.util.Collections;
import java.util.List;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class SearchResultSummaryRecord {

	private String searchTerm;
    private String modifiedSearchTerm;
	private List<MatchView> records;
    private int recordCount;

	public SearchResultSummaryRecord(String searchTerm, String modifiedSearchTerm) {
		this.searchTerm = searchTerm;
		this.modifiedSearchTerm=modifiedSearchTerm;
		setRecords(Collections.emptyList());
	}

	public void setRecords(List<MatchView> records) {
		this.records = records == null ? Collections.emptyList() : records;
		this.recordCount = this.records.size();
	}
}
