package ix.core.search.text;

import java.util.function.Consumer;

public interface ReflectingIndexerAware {
    void index(PathStack currentPathStack, Consumer<IndexableValue> consumer);
    String getEmbeddedIndexFieldName();
}
