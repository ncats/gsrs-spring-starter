package gsrs.startertests.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import gsrs.controller.AbstractGsrsEntityController;
import gsrs.controller.GsrsRestApiController;
import gsrs.controller.IdHelpers;
import gsrs.controller.OffsetBasedPageRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Sort;

import java.io.IOException;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@GsrsRestApiController(context= MyController.CONTEXT)
public class MyController extends AbstractGsrsEntityController<MyEntity, UUID> {
    public static final String CONTEXT = "myEntity";


    private ObjectMapper mapper = new ObjectMapper();
    @Autowired
    private MyEntityRepository repository;

    public MyController(){
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
    protected long count() {
        return repository.count();
    }

    @Override
    protected Optional<MyEntity> get(UUID id) {
        return Optional.ofNullable(repository.getOne(id));
    }

    @Override
    protected UUID parseIdFromString(String idAsString) {
        return UUID.fromString(idAsString);
    }

    @Override
    protected Optional<MyEntity> flexLookup(String someKindOfId) {

        return Optional.ofNullable(repository.findDistinctByFoo(someKindOfId));
    }

    @Override
    protected Class<MyEntity> getEntityClass() {
        return MyEntity.class;
    }

    @Override
    protected Page page(long offset, long numOfRecords, Sort sort) {
        return repository.findAll(new OffsetBasedPageRequest(offset, numOfRecords, sort));
    }

    @Override
    protected void delete(UUID id) {
        repository.deleteById(id);
    }

    @Override
    protected UUID getIdFrom(MyEntity entity) {
        return entity.getUuid();
    }

    @Override
    protected MyEntity update(MyEntity myEntity) {
        return repository.saveAndFlush(myEntity);
    }
}
