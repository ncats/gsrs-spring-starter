package ix.ginas.models;

import ix.core.models.Keyword;
import ix.ginas.models.v1.VocabularyTerm;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class VocabularyTermBuilder {
    private VocabularyTerm term = new VocabularyTerm();

    public VocabularyTermBuilder(String value){
        term.value = value;
        term.display = value;
    }

    public VocabularyTermBuilder display(String display){
        term.display = display;
        return this;
    }
    public VocabularyTermBuilder filter(String label, String term){
        return filter(new Keyword(label, term));
    }
    public VocabularyTermBuilder filter(Keyword keyword){
        this.term.filters.add(Objects.requireNonNull(keyword));
        return this;
    }
    public VocabularyTermBuilder filter(String... labelTermPairs){
        List<Keyword> keywords = new ArrayList<>();
        for(int i=0; i<labelTermPairs.length; i+=2){
            keywords.add(new Keyword(labelTermPairs[i], labelTermPairs[i+1]));
        }
        this.term.filters.addAll(keywords);
        return this;
    }
    public VocabularyTerm build(){
        return term;
    }
}
