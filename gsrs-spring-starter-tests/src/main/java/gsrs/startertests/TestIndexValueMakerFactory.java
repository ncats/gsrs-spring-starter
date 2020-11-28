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
    private List<IndexValueMaker> indexValueMakers;

    public TestIndexValueMakerFactory(IndexValueMaker... indexValueMakers){
        setIndexValueMakerListTo(indexValueMakers);
    }

    private void setIndexValueMakerListTo(IndexValueMaker[] indexValueMakers) {
        List<IndexValueMaker> list = new ArrayList<>(Arrays.asList(indexValueMakers));
        list.forEach(Objects::nonNull);
        this.indexValueMakers = list;
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
        resetCache();
        return this;
    }

    /**
     * Remove all index value makers.
     */
    public void clearAll() {
        indexValueMakers.clear();
        resetCache();
    }
    public TestIndexValueMakerFactory setIndexValueMakers(IndexValueMaker... indexValueMakers){
        setIndexValueMakerListTo(indexValueMakers);
        resetCache();
        return this;
    }
}
