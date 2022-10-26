package gsrs.services;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import gsrs.repository.TextRepository;
import ix.core.models.Text;

@Service
public class TextServiceImpl implements TextService {
		
    @Autowired
    private TextRepository textRepository;
    private static Logger log = LoggerFactory.getLogger(TextServiceImpl.class);
    
    @Override
    public Long saveTextList(String label, List<String> textList) {
    	Text text = new Text();
    	text.label = label;
    	    	
    	ObjectMapper mapper = new ObjectMapper();        
        String jsonArray;
        
		try {
			jsonArray = mapper.writeValueAsString(textList);
			text.text = jsonArray.toString();  
		} catch (JsonProcessingException e) {			
			e.printStackTrace();
			log.error("Error in TextService writing to jsonarray string!");
		}    	 	
    	
    	Text saved = textRepository.saveAndFlush(text);
    	return saved.id;
    }
    
    @Override
    public Long updateTextString(String label, long id, String textString){
    	Text text = textRepository.findById(id).orElse(null);
    	text.setText(textString);
    	Text saved = textRepository.saveAndFlush(text);
    	return saved.id;
    }
    
    @Override
    public Long saveTextString(String label, String textString) {
    	Text text = new Text();
    	text.label = label;
    	text.text = textString;    	
    	Text saved = textRepository.saveAndFlush(text);
    	return saved.id;
    }
    
    @Override
    public String getText(String id){
    	Long recordId = Long.parseLong(id);
    	Text text = textRepository.findById(recordId).orElse(null);
    	if(text!=null) {    		
    		return text.text;
    		}
    	else 
    		return "";
    	
    }
    
    @Override
    public void deleteText(String id) {
    	Long recordId = Long.parseLong(id);
    	textRepository.deleteById(recordId);
    }	
}
