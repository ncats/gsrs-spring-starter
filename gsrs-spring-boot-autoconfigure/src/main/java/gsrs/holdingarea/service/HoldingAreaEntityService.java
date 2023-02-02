package gsrs.holdingarea.service;

import com.fasterxml.jackson.databind.JsonNode;
import gsrs.holdingarea.model.MatchableKeyValueTuple;
import gsrs.service.GsrsEntityService;
import ix.core.search.text.IndexValueMaker;
import ix.core.search.text.TextIndexer;
import ix.core.validator.ValidationResponse;
import java.util.List;

public interface HoldingAreaEntityService<T> {
    Class<T> getEntityClass();

    T parse(JsonNode json);

    ValidationResponse<T> validate(T t);

    List<MatchableKeyValueTuple> extractKVM(T t);

    IndexValueMaker<T> createIVM(T t);

    GsrsEntityService.ProcessResult<T> persistEntity(T t);

    T retrieveEntity(String entityId);

    void IndexEntity(TextIndexer indexer, Object object);
}
