package ix.core.search.bulk;

public interface ResultListRecordGenerator {	
	
	default ResultListRecord generate(String keyString) {
		
		ResultListRecord.ResultListRecordBuilder builder = ResultListRecord.builder();
		if(keyString != null && !keyString.trim().isEmpty() ) {			
			builder.key(keyString);
		}
		return builder.build();
	}
}
