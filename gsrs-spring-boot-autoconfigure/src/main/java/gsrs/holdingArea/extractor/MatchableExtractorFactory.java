package gsrs.holdingArea.extractor;

import gsrs.holdingArea.model.MatchableKeyValueTupleExtractor;
import gsrs.holdingArea.service.HoldingAreaEntityService;

public interface MatchableExtractorFactory {
        <T> MatchableKeyValueTupleExtractor<T> createExtractorFor(Class <T> cls);

        void setEntityService(HoldingAreaEntityService entityService);
}
