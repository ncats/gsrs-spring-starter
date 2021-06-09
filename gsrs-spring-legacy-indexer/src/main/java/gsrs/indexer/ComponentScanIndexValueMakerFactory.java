package gsrs.indexer;

import ix.core.search.text.CombinedIndexValueMaker;
import ix.core.search.text.IndexValueMaker;
import ix.core.search.text.ReflectingIndexValueMaker;
import ix.core.util.EntityUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * Factory class that finds all the {@link IndexValueMaker}
 * components in the component scan and maps figures out which
 * ones should be called for each entity.
 *
 * There is currently one default indexValueMaker, the {@link ReflectingIndexValueMaker}
 * that is included on all objects.
 *
 * If more than one indexvalue maker is found to apply to a given entity,
 * then they are merged into a composite {@link IndexValueMaker} instance
 * to hide it from the caller.
 */
@Service
public class ComponentScanIndexValueMakerFactory extends AbstractIndexValueMakerFactory {


    @Autowired(required = false)
    private List<IndexValueMaker> indexValueMakers;

    @Override
    protected void registerIndexValueMakers(Consumer<IndexValueMaker> registrar) {
        //indexValueMakers field is null if no components found
        if(indexValueMakers !=null) {
           indexValueMakers.forEach(registrar::accept);
        }
    }
}
