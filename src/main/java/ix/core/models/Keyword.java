package ix.core.models;

import javax.persistence.*;
import java.util.Objects;

@Entity
@DiscriminatorValue("KEY")
@DynamicFacet(label="label", value="term")
@Indexable
public class Keyword extends Value {
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
}
