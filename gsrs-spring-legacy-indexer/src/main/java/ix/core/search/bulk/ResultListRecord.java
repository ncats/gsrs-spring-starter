package ix.core.search.bulk;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ResultListRecord {
	String key;
	String displayName;
	String displayCode;
	String displayCodeSystem;
	
	public static ResultListRecordBuilder builder() {return new ResultListRecordBuilder();}
}
