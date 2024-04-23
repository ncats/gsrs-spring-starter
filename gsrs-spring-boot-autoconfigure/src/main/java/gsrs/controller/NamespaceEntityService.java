package gsrs.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import gsrs.events.AbstractEntityCreatedEvent;
import gsrs.events.AbstractEntityUpdatedEvent;
import gsrs.repository.NamespaceRepository;
import gsrs.service.AbstractGsrsEntityService;
import ix.core.models.Namespace;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
@Service
public class NamespaceEntityService extends AbstractGsrsEntityService<Namespace, Long> {

    public static final String CONTEXT = "namespace";
    @Autowired
    private NamespaceRepository namespaceRepository;
    private ObjectMapper mapper = new ObjectMapper();

    public NamespaceEntityService() {
        super(CONTEXT, IdHelpers.NUMBER, null, null, null);
    }

    @Override
    protected Namespace fromNewJson(JsonNode json) throws IOException {
        Namespace n = fromUpdatedJson(json);
        n.id = null; //blank out for new id
        return n;
    }


    @Override
    protected Namespace fromUpdatedJson(JsonNode json) throws IOException {
        return mapper.convertValue(json, Namespace.class);
    }

    @Override
    protected JsonNode toJson(Namespace namespace) throws IOException {
        return mapper.valueToTree(namespace);
    }

    @Override
    protected Namespace create(Namespace namespace) {
        return namespaceRepository.save(namespace);
    }

    @Override
    public Optional<Namespace> get(Long id) {
        return namespaceRepository.findById(id);
    }

    @Override
    public Long parseIdFromString(String idAsString) {
        return Long.parseLong(idAsString);
    }

    @Override
    public Long getIdFrom(Namespace entity) {
        return entity.id;
    }

    @Override
    protected Namespace update(Namespace namespace) {
        return namespaceRepository.save(namespace);
    }

    @Override
    protected AbstractEntityUpdatedEvent<Namespace> newUpdateEvent(Namespace updatedEntity) {
        return null;
    }

    @Override
    protected AbstractEntityCreatedEvent<Namespace> newCreationEvent(Namespace createdEntity) {
        return null;
    }

    @Override
    public Optional<Namespace> flexLookup(String someKindOfId) {
        return Optional.ofNullable(namespaceRepository.findByName(someKindOfId));
    }

    @Override
    public long count() {
        return namespaceRepository.count();
    }

    @Override
    public void delete(Long id) {
        namespaceRepository.deleteById(id);
    }

    @Override
    public Class<Namespace> getEntityClass() {
        return Namespace.class;
    }

    @Override
    public Page page(Pageable pageable) {
        return namespaceRepository.findAll(pageable);
    }

	@Override
	public List<Long> getIDs() {
		return namespaceRepository.getAllIDs();
	}
}
