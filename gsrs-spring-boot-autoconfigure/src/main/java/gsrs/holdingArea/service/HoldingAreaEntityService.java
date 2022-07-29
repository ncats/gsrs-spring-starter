package gsrs.holdingArea.service;

import com.fasterxml.jackson.databind.JsonNode;
import gsrs.holdingArea.model.MatchableKeyValueTuple;
import ix.core.search.text.IndexValueMaker;
import ix.core.validator.ValidationResponse;
import java.util.List;

public interface HoldingAreaEntityService<T> {
    Class<T> getEntityClass();

    T parse(JsonNode json);

    ValidationResponse<T> validate(T t);

    List<MatchableKeyValueTuple> extractKVM(T t);

    IndexValueMaker<T> createIVM(T t);

}
