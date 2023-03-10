package ix.core.search.text;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import lombok.Data;

/**
 * Configuration properties for the Legacy TextIndexer
 */
@Component
@Data
public class TextIndexerConfig {
    @Value("#{new Boolean('${ix.textindex.enabled:true}')}")
    private boolean enabled = true;
    @Value("#{new Boolean('${ix.textindex.fieldsuggest:true}')}")
    private boolean fieldsuggest;
    @Value("#{new Boolean('${ix.textindex.shouldLog:false}')}")
    private boolean shouldLog;

//    private static final boolean USE_ANALYSIS =    ConfigHelper.getBoolean("ix.textindex.fieldsuggest",true);

    @Value("#{new Integer('${ix.fetchWorkerCount:4}')}")
    private int fetchWorkerCount = 4;
    
//    @Value("${ix.index.deepfields:}")
//    private List<String> deepFields = new ArrayList<>();

    
    @Value("#{new String('${ix.index.deepfieldsraw:}').split(';')}")
    private String[] deepFieldsRaw;
    
    @Value("#{new Boolean('${ix.index.rootIndexOnly:false}')}")
    private boolean rootIndexOnly;


    public List<String> getDeepFields(){
        if(deepFieldsRaw==null || deepFieldsRaw.length == 0 || deepFieldsRaw[0]==null || "".equals(deepFieldsRaw[0])){
            return new ArrayList<String>();
        }
        return Arrays.stream(deepFieldsRaw).collect(Collectors.toList());
    }

}
