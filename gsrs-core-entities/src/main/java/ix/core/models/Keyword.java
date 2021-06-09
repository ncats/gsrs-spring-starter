package ix.core.models;

import ix.core.search.text.IndexableValue;
import ix.core.search.text.IndexableValueFromRaw;
import ix.core.search.text.ReflectingIndexerAware;
import ix.utils.PathStack;

import javax.persistence.*;
import java.util.Objects;
import java.util.function.Consumer;

@Entity
@DiscriminatorValue("KEY")
@DynamicFacet(label="label", value="term")
@Indexable
public class Keyword extends Value implements ReflectingIndexerAware {
    @Column(length=255)

    public String term;
    @Lob
    @Basic(fetch=FetchType.EAGER)
    public String href;

    public Keyword () {}
    public Keyword (String term) {
        this.term = term;
    }
    public Keyword (String label, String term) {
        super (label);
        this.term = term;
    }

    @Override
    public String getValue () { return term; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Keyword)) return false;
        Keyword keyword = (Keyword) o;
        return Objects.equals(term, keyword.term) &&
                Objects.equals(href, keyword.href);
    }

    @Override
    public int hashCode() {
        return Objects.hash(term, href);
    }

    public String toString(){
    	String ret="Keyword id = " + id + " ";
    	if(this.label==null){
    		return ret +  "NO_LABEL:" + this.term;
    	}else{
    		return ret +  this.label  + ":" + this.term;
    	}
    }

    //katzelda Nov 2020: these methods for new ReflectingIndexerAware interface were added so we dont hardcode Keyword into indexer
    @Override
    public void index(PathStack currentPathStack, Consumer<IndexableValue> consumer) {
        if(label ==null){
            return;
        }
        consumer.accept(new IndexableValueFromRaw( this.label, this.getValue(), currentPathStack.toPath()).dynamic().suggestable());

    }

    @Override
    public String getEmbeddedIndexFieldName() {
        return this.label;
    }
}
