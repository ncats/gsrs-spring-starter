package ix.ginas.models.v1;

//import com.example.demo.GsrsAnalyzers;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import ix.core.models.Backup;
import ix.core.models.Indexable;
import ix.core.models.IxModel;
import ix.core.models.Keyword;
import ix.ginas.models.serialization.KeywordDeserializer;
import ix.ginas.models.serialization.KeywordListSerializer;
import lombok.Getter;
import lombok.Setter;
//import org.hibernate.search.engine.backend.types.Aggregable;
//import org.hibernate.search.engine.backend.types.Searchable;
//import org.hibernate.search.mapper.pojo.mapping.definition.annotation.FullTextField;
//import org.hibernate.search.mapper.pojo.mapping.definition.annotation.GenericField;
//import org.hibernate.search.mapper.pojo.mapping.definition.annotation.Indexed;
//import org.hibernate.search.mapper.pojo.mapping.definition.annotation.KeywordField;

import javax.persistence.*;
import java.util.ArrayList;
import java.util.List;


@Entity
@Table(name = "ix_ginas_controlled_vocab")
@Inheritance
@DiscriminatorValue("CTLV")
@Backup

@Getter
@Setter
//@Indexed
public class ControlledVocabulary extends IxModel {

    private static final long serialVersionUID = 5455592961232451608L;

    //We need to keep this JsonIgnore
    //so when we generate the cv.json file
    //the ids aren't included.  This causes a problem
    //on import because then the ids get re-used by ebean
    //since it prefetches them before we load!!
    
    //The above is true, however, updates are broken without this,
    //as the JSON is used in the forms. Now that the ID is explicitly
    //ignored in the load, we should be fine. 
    
//    @JsonIgnore
//    public Long getId() {
//        return this.id;
//    }
	
    @Column(unique = true)
    @Indexable(name = "Domain", facet = true)
//    @KeywordField(name = "Domain", searchable = Searchable.YES)
    public String domain;


    public void setTerms(List<VocabularyTerm> terms) {

        this.terms = terms;
        if(terms !=null){
           terms.forEach(t -> t.setOwner(this));
        }
        setIsDirty("terms");
    }

    private String vocabularyTermType = ControlledVocabulary.class.getName();

    @ManyToMany(cascade = CascadeType.ALL)
    @JsonSerialize(using = KeywordListSerializer.class)
    @JsonDeserialize(contentUsing = KeywordDeserializer.class)
    @Indexable(name = "Field")
    @JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})

    public List<Keyword> fields = new ArrayList<Keyword>();

    public boolean editable = true;

    public boolean filterable = false;


    @OneToMany(mappedBy = "owner", cascade = CascadeType.ALL)
    //@JoinTable(name="ix_ginas_cv_terms")
    @JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
    @Basic(fetch= FetchType.EAGER)
    public List<VocabularyTerm> terms = new ArrayList<>();

    public VocabularyTerm getTermWithValue(String val) {
        for (VocabularyTerm vt : this.terms) {
            //System.out.println("Looking at value:" + vt.display);
            if (vt.value.equals(val)) {

                return vt;
            }
        }
        return null;
    }

    public List<VocabularyTerm> getTerms() {
        return terms;
    }

    public  void addTerms(VocabularyTerm term) {

        this.terms.add(term);
        term.setOwner(this);
        setIsDirty("terms");
    }

    public void addField(Keyword keyword) {

        this.fields.add(keyword);
        setIsDirty("fields");
    }

    public void setVocabularyTermType(String vocabularyTermType) {
        this.vocabularyTermType = vocabularyTermType;
    }

    public String getVocabularyTermType() {
        return vocabularyTermType;
    }

    public void setDomain(String domain) {
        this.domain = domain;
        setIsDirty("domain");
    }


}
