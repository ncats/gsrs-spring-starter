package ix.core.search.text;

import ix.utils.PathStack;

import java.util.function.Consumer;

public interface ReflectingIndexerAware {
    void index(PathStack currentPathStack, Consumer<IndexableValue> consumer);
    String getEmbeddedIndexFieldName();
}
