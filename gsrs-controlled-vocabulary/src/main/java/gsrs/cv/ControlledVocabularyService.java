package gsrs.cv;

import gsrs.repository.ControlledVocabularyRepository;
import gsrs.security.canManageCVs;
import gsrs.security.hasAdminRole;
import ix.ginas.models.v1.ControlledVocabulary;
import ix.ginas.models.v1.VocabularyTerm;
import org.springframework.beans.factory.annotation.Autowired;

public class ControlledVocabularyService {

    @Autowired
    private ControlledVocabularyRepository repository;

    //@hasAdminRole
    @canManageCVs
    public void addTerm(ControlledVocabulary cv, VocabularyTerm term){
        cv.addTerms(term);
        //business here
        repository.save(cv);
    }

    public void foo(){
        addTerm(null, null);
    }
}
