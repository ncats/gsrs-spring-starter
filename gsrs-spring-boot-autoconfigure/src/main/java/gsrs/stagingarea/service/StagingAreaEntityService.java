package gsrs.stagingarea.service;

import com.fasterxml.jackson.databind.JsonNode;
import gsrs.stagingarea.model.MatchableKeyValueTuple;
import gsrs.service.GsrsEntityService;
import ix.core.search.text.IndexValueMaker;
import ix.core.search.text.TextIndexer;
import ix.core.validator.ValidationResponse;
import java.util.List;
import java.util.function.Consumer;

public interface StagingAreaEntityService<T> {
    Class<T> getEntityClass();

    T parse(JsonNode json);

    ValidationResponse<T> validate(T t);

    List<MatchableKeyValueTuple> extractKVM(T t);

    IndexValueMaker<T> createIVM(T t);

    GsrsEntityService.ProcessResult<T> persistEntity(T t, boolean isNew);

    T retrieveEntity(String entityId);

    void IndexEntity(TextIndexer indexer, Object object);

    void synchronizeEntity(T t, Consumer<String> recorder, JsonNode options);
}
