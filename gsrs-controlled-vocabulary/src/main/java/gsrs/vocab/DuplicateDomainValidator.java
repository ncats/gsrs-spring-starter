package gsrs.vocab;

import gsrs.repository.ControlledVocabularyRepository;
import gsrs.validator.ValidatorConfig;
import ix.core.validator.GinasProcessingMessage;
import ix.core.validator.ValidatorCallback;
import ix.ginas.models.v1.ControlledVocabulary;
import ix.ginas.utils.validation.ValidatorPlugin;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;

public class DuplicateDomainValidator implements ValidatorPlugin<ControlledVocabulary> {
    @Autowired
    private ControlledVocabularyRepository repository;

    @Override
    public void validate(ControlledVocabulary objnew, ControlledVocabulary objold, ValidatorCallback callback) {
        String domain = objnew.getDomain();
        if(domain ==null){
            return;
        }

        List<ControlledVocabularyRepository.ControlledVocabularySummary> list = repository.findSummaryByDomain(domain);
        if(list.isEmpty()){
            //new domain that's fine
            return;
        }
        Long duplicateId = list.get(0).getId();
        if(!duplicateId.equals(objnew.getId())){
            //id doesn't match so it's either a new record or a different record
            callback.addMessage(GinasProcessingMessage.ERROR_MESSAGE("Duplicate Domain '"+domain + "' in vocabulary id = "+ duplicateId ));
            //TODO add link to the other record ?
        }
    }

    @Override
    public boolean supports(ControlledVocabulary newValue, ControlledVocabulary oldValue, ValidatorConfig.METHOD_TYPE methodType) {
        switch(methodType){
            case BATCH:
            case IGNORE:
                return false;
            default: return true;
        }
    }
}
