package gsrs.services;

import java.util.List;

public interface TextService {
	
	Long saveTextList(String label, List<String> textList);	
	Long saveTextString(String label, String textJsonString);	
	String getText(String id);
	//todo might need change this to delete by id and label to be safe?  
	void deleteText(String id);	
}
