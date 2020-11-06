package ix.core.search.text;

import java.util.List;
import java.util.function.Consumer;

public class CombinedIndexValueMaker<T> implements IndexValueMaker<T> {
    private final List<IndexValueMaker<? super T>> list;
    private final Class<T> clazz;
    public CombinedIndexValueMaker(Class<T> clazz, List<IndexValueMaker<? super T>> list) {
        this.list = list;
        this.clazz = clazz;
    }

    @Override
    public Class<T> getIndexedEntityClass() {
        return clazz;
    }

    @Override
    public void createIndexableValues(T t, Consumer<IndexableValue> c) {
        list.forEach(i -> {
            try {
                i.createIndexableValues(t, c);
            } catch (Exception e) {
                e.printStackTrace();
//		        Logger.error("Trouble creating index for:" + EntityWrapper.of(t).getKey(), e);
            }
        });

    }
}
