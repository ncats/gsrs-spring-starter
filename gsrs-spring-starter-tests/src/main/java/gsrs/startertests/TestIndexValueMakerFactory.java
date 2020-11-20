package gsrs.startertests;

import gsrs.indexer.AbstractIndexValueMakerFactory;
import gsrs.indexer.IndexValueMakerFactory;
import ix.core.search.text.IndexValueMaker;
import ix.core.util.EntityUtils;

import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;

public class TestIndexValueMakerFactory extends AbstractIndexValueMakerFactory{
    private List<IndexValueMaker> indexValueMakers;

    public TestIndexValueMakerFactory(IndexValueMaker... indexValueMakers){
        this.indexValueMakers = Arrays.asList(indexValueMakers);
    }

    @Override
    protected void registerIndexValueMakers(Consumer<IndexValueMaker> registrar) {
        indexValueMakers.forEach(registrar::accept);
    }
}
