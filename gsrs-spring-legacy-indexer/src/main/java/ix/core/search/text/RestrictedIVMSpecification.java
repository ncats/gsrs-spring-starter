package ix.core.search.text;

import java.util.Collections;
import java.util.Set;

import ix.utils.Util;
import lombok.Data;

@Data
public class RestrictedIVMSpecification {
	
	boolean include;
	Set<String> tags;
	
	public enum RestrictedType{
        EXCLUDE_EXTERNAL,
        INCLUDE_USER_LIST        
    };
    
    public RestrictedIVMSpecification(boolean include, Set<String> tags) {
		this.include = include;
		this.tags = tags;		
	}
	
	public static RestrictedIVMSpecification getRestrictedIVMSpecs(RestrictedType type) {
		switch(type) {
		case EXCLUDE_EXTERNAL:
			return new RestrictedIVMSpecification(false, Util.toSet("external"));	
		case INCLUDE_USER_LIST:
			return new RestrictedIVMSpecification(true, Util.toSet("user_list"));	
		default:
			return new RestrictedIVMSpecification(false, Collections.<String>emptySet());		
		}		
	}
	
}
