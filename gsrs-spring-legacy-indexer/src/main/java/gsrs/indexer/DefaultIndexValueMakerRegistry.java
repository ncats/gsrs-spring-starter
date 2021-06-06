package gsrs.indexer;

import ix.core.search.text.IndexValueMaker;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;

@Service
public class DefaultIndexValueMakerRegistry {

    private List<IndexValueMaker> indexers = new ArrayList<>();


    public synchronized void addIndexer(IndexValueMaker indexValueMaker){
        Objects.requireNonNull(indexValueMaker);
        indexers.add(indexValueMaker);
    }

    public synchronized void consumeIndexers(Consumer<IndexValueMaker> consumer){
        if(consumer!=null) {
            indexers.forEach(i -> consumer.accept(i));
        }
    }
}
