package ix.utils.pojopatch;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.Builder;
import lombok.Data;

@Builder
@Data
public  class Change{
		private String path;
		private String op;
		private Object oldValue;
		private Object newValue;
		private Object oldContainer;
		private String from;
		
		public Change(String path, String op, Object oldValue, Object newValue, Object oldContainer, String from){
			this.path=path;
			this.op=op;
			this.oldValue=oldValue;
			this.newValue=newValue;
			this.oldContainer=oldContainer;
			this.from=from;
		}
		//TODO: somehow capture oldvalue
		public Change(JsonNode jsn){
			this(	jsn.at("/path").asText(),
					jsn.at("/op").asText(),
					null,
					jsn.at("/value"),
					jsn.at("/path"),
					(!jsn.at("/from").isMissingNode())?jsn.at("/from").asText():null
					);
		}
		public String toString(){
			return this.op + "\t" + this.path + "\t" + this.newValue + "\t" + this.oldValue + "\t" + this.from;
		}
	}