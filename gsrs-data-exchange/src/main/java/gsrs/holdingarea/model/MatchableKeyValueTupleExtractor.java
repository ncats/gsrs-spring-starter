package gsrs.holdingarea.model;

import java.util.function.Consumer;

public interface MatchableKeyValueTupleExtractor<T> {
    void extract(T t, Consumer<gsrs.holdingarea.model.MatchableKeyValueTuple> c);

    default MatchableKeyValueTupleExtractor<T> combine(MatchableKeyValueTupleExtractor<T> matchableKeyValueTupleExtractor) {
        MatchableKeyValueTupleExtractor<T> _this = this;
        return (t,c) -> {
            _this.extract(t,c);
            matchableKeyValueTupleExtractor.extract(t,c);
        };
    }
}
