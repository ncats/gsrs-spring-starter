package ix.core.search;

import gsrs.legacy.GsrsSuggestResult;

public class SuggestResult implements GsrsSuggestResult {
    CharSequence key, highlight;
    long weight=0;
    
    
    public SuggestResult (CharSequence key, CharSequence highlight) {
        this.key = key;
        this.highlight = highlight;
    }
    
    public SuggestResult (CharSequence key, CharSequence highlight, long weight) {
        this.key = key;
        this.highlight = highlight;
        this.weight=weight;
    }

    
    @Override
    public CharSequence getKey() { return key; }
    @Override
    public CharSequence getHighlight() { return highlight; }
    @Override
    public Long getWeight() { 
    	return weight; 
   }
}
