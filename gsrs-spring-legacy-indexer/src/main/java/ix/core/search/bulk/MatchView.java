package ix.core.search.bulk;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class MatchView {
	
	private String id;
	private String displayName;
	private String displayCode;
	private String displayCodeName;
	
	public static MatchViewBuilder builder() {return new MatchViewBuilder();}
}
