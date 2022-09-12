package gsrs.services;

import java.util.List;

public interface TextService {
	
	Long saveTextList(String label, List<String> textList);	
	Long saveTextString(String label, String textJsonString);	
	String getText(String id);
}
