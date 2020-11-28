package gsrs.startertests;

import gsrs.indexer.AbstractIndexValueMakerFactory;
import gsrs.indexer.IndexValueMakerFactory;
import ix.core.search.text.IndexValueMaker;
import ix.core.util.EntityUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;

/**
 * Test IndexValueMakerFactory that let's test code directly
 * control which {@link IndexValueMaker}s to add.
 */
public class TestIndexValueMakerFactory extends AbstractIndexValueMakerFactory{
    private List<IndexValueMaker> indexValueMakers = new ArrayList<>();

    public TestIndexValueMakerFactory(IndexValueMaker... indexValueMakers){
        for(IndexValueMaker i : indexValueMakers) {
            addIndexValueMaker(i);
        }
    }

    @Override
    protected void registerIndexValueMakers(Consumer<IndexValueMaker> registrar) {
        indexValueMakers.forEach(registrar::accept);
    }

    /**
     * Add the given IndexValueMaker to this Factory.
     * @param indexValueMaker the indexValueMaker to add can not be null.
     * @return this
     * @throws NullPointerException if parameter is null.
     */
    public TestIndexValueMakerFactory addIndexValueMaker(IndexValueMaker indexValueMaker){
        indexValueMakers.add(Objects.requireNonNull(indexValueMaker));
        return this;
    }

    /**
     * Remove all index value makers.
     */
    public void clearAll() {
        indexValueMakers.clear();
    }
}
