package gsrs.startertests.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import gsrs.controller.GsrsRestApiController;
import gsrs.controller.IdHelpers;
import gsrs.controller.OffsetBasedPageRequest;
import gsrs.service.AbstractGsrsEntityService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class MyEntityService extends AbstractGsrsEntityService<MyEntity, UUID> {
    public static final String CONTEXT = "myEntity";


    private ObjectMapper mapper = new ObjectMapper();
    @Autowired
    private MyEntityRepository repository;

    public MyEntityService(){
        super(CONTEXT, IdHelpers.UUID);
    }

    @Override
    protected MyEntity fromNewJson(JsonNode json) throws IOException {
        return mapper.convertValue(json, MyEntity.class);
    }

    @Override
    protected List<MyEntity> fromNewJsonList(JsonNode list) throws IOException {
        return null;
    }

    @Override
    protected MyEntity fromUpdatedJson(JsonNode json) throws IOException {
        return mapper.convertValue(json, MyEntity.class);
    }

    @Override
    protected List<MyEntity> fromUpdatedJsonList(JsonNode list) throws IOException {
        return null;
    }

    @Override
    protected JsonNode toJson(MyEntity myEntity) throws IOException {
        return mapper.valueToTree(myEntity);
    }

    @Override
    protected MyEntity create(MyEntity myEntity) {
         return repository.saveAndFlush(myEntity);
    }

    @Override
    public long count() {
        return repository.count();
    }

    @Override
    public Optional<MyEntity> get(UUID id) {
        return Optional.ofNullable(repository.getOne(id));
    }

    @Override
    public UUID parseIdFromString(String idAsString) {
        return UUID.fromString(idAsString);
    }

    @Override
    public Optional<MyEntity> flexLookup(String someKindOfId) {

        return Optional.ofNullable(repository.findDistinctByFoo(someKindOfId));
    }

    @Override
    public Class<MyEntity> getEntityClass() {
        return MyEntity.class;
    }

    @Override
    public Page page(long offset, long numOfRecords, Sort sort) {
        return repository.findAll(new OffsetBasedPageRequest(offset, numOfRecords, sort));
    }

    @Override
    public void delete(UUID id) {
        repository.deleteById(id);
    }

    @Override
    public UUID getIdFrom(MyEntity entity) {
        return entity.getUuid();
    }

    @Override
    protected MyEntity update(MyEntity myEntity) {
        return repository.saveAndFlush(myEntity);
    }
}
