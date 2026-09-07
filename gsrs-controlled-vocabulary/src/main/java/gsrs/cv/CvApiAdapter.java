package gsrs.cv;

import org.springframework.beans.factory.annotation.Qualifier;
import tools.jackson.databind.JsonNode;

import gsrs.controller.GsrsControllerUtil;
import gsrs.cv.api.*;
import gsrs.service.GsrsEntityService;
import ix.ginas.models.v1.ControlledVocabulary;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.json.JsonMapper;

import java.io.IOException;
import java.util.Collections;
import java.util.Optional;

/**
 * A {@link ControlledVocabularyApi} implementation
 * that directly calls the service methods without making REST calls
 * this is useful to use if you are including the ControlledVocabulary
 * in your spring boot app itself instead of having it make a call
 * to an external micro-service.
 */
public class CvApiAdapter implements ControlledVocabularyApi {

    private final ControlledVocabularyEntityService service;

    @Autowired
    public CvApiAdapter(ControlledVocabularyEntityService service, @Qualifier("legacyMapper") JsonMapper jsonMapper) {
        this.service = service;
        this.mapper=jsonMapper;
    }

    private final JsonMapper mapper;

    private AbstractGsrsControlledVocabularyDTO toDto(ControlledVocabulary cv){
        //for now rather than do instance of checks and call setters we do the lazy
        // not at all efficient way of serializing and then deserializing to json

        JsonNode node = mapper.valueToTree(GsrsControllerUtil.enhanceWithView(cv, Collections.emptyMap()));
        return mapper.convertValue(node, AbstractGsrsControlledVocabularyDTO.class);
    }



    @Override
    @Transactional(readOnly = true)
    public <T extends AbstractGsrsControlledVocabularyDTO> Optional<T> findByDomain(String domain)  {
        Optional<ControlledVocabulary> opt= service.getEntityBySomeIdentifier(domain);
        return (Optional<T>) opt.map(this::toDto);
    }

    @Override
    public long count(){
        return service.count();
    }

    @Override
    @Transactional(readOnly = true)
    public <T extends AbstractGsrsControlledVocabularyDTO> Optional<T> findByResolvedId(String anyKindOfId) {
        return (Optional<T>) service.getEntityBySomeIdentifier(anyKindOfId).map(this::toDto);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<AbstractGsrsControlledVocabularyDTO> findById(Long id) {
        return service.get(id).map(this::toDto);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean existsById(Long id) throws IOException {
        return service.get(id).isPresent();
    }

    @Override
    @Transactional
    public <T extends AbstractGsrsControlledVocabularyDTO> T create(T dto) throws IOException {
        GsrsEntityService.CreationResult<ControlledVocabulary> result= service.createEntity(mapper.valueToTree(dto));
        if(!result.isCreated()){
            throw new IOException(result.getValidationResponse().toString());
        }
        return (T) toDto(result.getCreatedEntity());
    }

    @Override
    @Transactional
    public <T extends AbstractGsrsControlledVocabularyDTO> T update(T dto) throws IOException {
        GsrsEntityService.UpdateResult<ControlledVocabulary> result= null;
        try {
            result = service.updateEntity(mapper.valueToTree(dto));
        } catch (Exception e) {
            throw new IOException(e.getMessage(), e);
        }
        GsrsEntityService.UpdateResult.STATUS status = result.getStatus();
        switch (status){
            case NOT_FOUND: throw new IOException("not found : " + dto.getId());
            case ERROR: new IOException(result.getValidationResponse().toString());
            default: break;
        }

        return (T) toDto(result.getUpdatedEntity());
    }
}
