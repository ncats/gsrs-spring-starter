package ix.ginas.models.v1;

import ix.core.SingleParent;

import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import jakarta.persistence.Inheritance;

/**
 * Created by peryeata on 2/17/16.
 */

@Entity
@Inheritance
@DiscriminatorValue("SYS")
@SingleParent
public class CodeSystemVocabularyTerm extends VocabularyTerm {
    
    String systemCategory;
    String regex;
    
		
	
	public String getRegex() {
		return regex;
	}
	public void setRegex(String regex) {
		if(regex==null || regex.equals("")){
			this.regex=null;
		}else{
			this.regex = regex;
		}
	}
	public String getSystemCategory() {
		return systemCategory;
	}
	public void setSystemCategory(String systemCategory) {
		this.systemCategory = systemCategory;
	}
	
	
	public CodeSystemVocabularyTerm(){};
    
}
