package gsrs.stagingarea.extractor;

import gsrs.stagingarea.model.MatchableKeyValueTupleExtractor;
import gsrs.stagingarea.service.StagingAreaEntityService;

public interface MatchableExtractorFactory {
        <T> MatchableKeyValueTupleExtractor<T> createExtractorFor(Class <T> cls);

        void setEntityService(StagingAreaEntityService entityService);
}
