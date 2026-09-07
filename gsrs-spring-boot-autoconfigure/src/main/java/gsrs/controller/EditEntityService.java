package gsrs.controller;

import org.springframework.beans.factory.annotation.Qualifier;
import tools.jackson.databind.JsonNode;
import gsrs.events.AbstractEntityCreatedEvent;
import gsrs.events.AbstractEntityUpdatedEvent;
import gsrs.repository.EditRepository;
import gsrs.service.AbstractGsrsEntityService;
import ix.core.models.Edit;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import tools.jackson.databind.json.JsonMapper;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class EditEntityService extends AbstractGsrsEntityService<Edit, UUID> {
    @Autowired
    private EditRepository editRepository;

    @Autowired
    @Qualifier("legacyJsonMapper")
    private JsonMapper mapper;

    public EditEntityService() {
        super("edits", IdHelpers.UUID, null,null,null);
    }

    @Override
    protected Edit fromNewJson(JsonNode json) throws IOException {
        Edit edit = fromUpdatedJson(json);
        //blank out the id so we generate a new one
        edit.id = null;
        return edit;
    }

    @Override
    protected List<Edit> fromNewJsonList(JsonNode list) throws IOException {
        List<Edit> entityList = new ArrayList<>(list.size());
        for(JsonNode editValue: list){
            entityList.add(fromNewJson(editValue));
        }
        return entityList;
    }

    @Override
    protected Edit fromUpdatedJson(JsonNode json)  {
        return mapper.treeToValue(json, Edit.class);
    }

    @Override
    protected List<Edit> fromUpdatedJsonList(JsonNode list)  {
        List<Edit> entityList = new ArrayList<>(list.size());
        for(JsonNode editValue: list){
            entityList.add(fromUpdatedJson(editValue));
        }
        return entityList;
    }

    @Override
    protected JsonNode toJson(Edit edit) {
        return mapper.valueToTree(edit);
    }

    @Override
    protected Edit create(Edit edit) {
        return editRepository.save(edit);
    }

    @Override
    public Optional<Edit> get(UUID id) {
        return editRepository.findById(id);
    }

    @Override
    public UUID parseIdFromString(String idAsString) {
        return UUID.fromString(idAsString);
    }

    @Override
    public UUID getIdFrom(Edit entity) {
        return entity.id;
    }

    @Override
    protected Edit update(Edit edit) {
        return editRepository.save(edit);
    }

    @Override
    protected AbstractEntityUpdatedEvent<Edit> newUpdateEvent(Edit updatedEntity) {
        return null;
    }

    @Override
    protected AbstractEntityCreatedEvent<Edit> newCreationEvent(Edit createdEntity) {
        return null;
    }

    @Override
    public Optional<Edit> flexLookup(String someKindOfId) {
        return get(parseIdFromString(someKindOfId));
    }

    @Override
    public long count() {
        return editRepository.count();
    }

    @Override
    public void delete(UUID id) {
        editRepository.deleteById(id);
    }

    @Override
    public Class<Edit> getEntityClass() {
        return Edit.class;
    }

    @Override
    public Page<Edit> page(Pageable pageable) {
        return editRepository.findAll(pageable);
    }

	@Override
	public List<UUID> getIDs() {
		return editRepository.getAllIDs();
	}
}
