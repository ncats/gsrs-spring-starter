package gsrs.holdingarea.extractor;

import gsrs.holdingarea.model.MatchableKeyValueTupleExtractor;
import gsrs.holdingarea.service.HoldingAreaEntityService;

public interface MatchableExtractorFactory {
        <T> MatchableKeyValueTupleExtractor<T> createExtractorFor(Class <T> cls);

        void setEntityService(HoldingAreaEntityService entityService);
}
