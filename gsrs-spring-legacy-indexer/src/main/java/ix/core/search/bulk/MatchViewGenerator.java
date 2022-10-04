package ix.core.search.bulk;

public interface MatchViewGenerator  {
	
	default MatchView generate(BulkSearchResult bsr) {
		
		MatchView.MatchViewBuilder builder = MatchView.builder();
		if(bsr != null && bsr.getKey() != null) {			
			builder.id(bsr.getKey().getIdString());
		}
		return builder.build();
	}
}
