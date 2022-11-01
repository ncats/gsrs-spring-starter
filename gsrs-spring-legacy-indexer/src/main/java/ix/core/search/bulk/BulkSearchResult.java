package ix.core.search.bulk;

import ix.core.util.EntityUtils.Key;
import lombok.Data;

@Data
public class BulkSearchResult {
	
	private Key key;
	private String query;
	
}
