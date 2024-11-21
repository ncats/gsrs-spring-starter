package gsrs.indexer;

import com.fasterxml.jackson.databind.ObjectMapper;
import gov.nih.ncats.common.util.CachedSupplier;
import gsrs.springUtils.AutowireHelper;
import ix.core.search.text.CombinedIndexValueMaker;
import ix.core.search.text.IndexValueMaker;
import ix.core.search.text.ReflectingIndexValueMaker;
import ix.core.util.EntityUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

public class ConfigBasedIndexValueMakerFactory implements IndexValueMakerFactory{
    private ReflectingIndexValueMaker reflectingIndexValueMaker = new ReflectingIndexValueMaker();

    private List<ConfigBasedIndexValueMakerConfiguration.IndexValueMakerConf> confList;

    private CachedSupplier<List<IndexValueMaker>> indexers = CachedSupplier.runOnce(()->{
        ObjectMapper mapper = new ObjectMapper();
        List<IndexValueMaker> ivms = confList.stream()
        		.map(c ->{
        			try {
        				IndexValueMaker indexer = (IndexValueMaker)c.newIndexValueMaker(mapper, AutowireHelper.getInstance().getClassLoader());
        				if(indexer !=null) {
        					indexer = AutowireHelper.getInstance().autowireAndProxy(indexer);
        				}

        				return (IndexValueMaker)indexer;
        			} catch (Exception e) {
        				throw new IllegalStateException("error creating indexer for " + c, e);
        			}

        		})
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
        return ivms;
    });

    private CachedSupplier<Map<Class, List<IndexValueMaker>>> map;
    public ConfigBasedIndexValueMakerFactory( List<ConfigBasedIndexValueMakerConfiguration.IndexValueMakerConf> indexers ) {
        this(indexers, null);
    }
    public ConfigBasedIndexValueMakerFactory(List<ConfigBasedIndexValueMakerConfiguration.IndexValueMakerConf> confs, DefaultIndexValueMakerRegistry defaultIndexValueMakerRegistry){
        this.confList = new ArrayList<>(confs);
        map = CachedSupplier.of( ()->{
            Map<Class, List<IndexValueMaker>> valueMakersMap = new ConcurrentHashMap<>();

            for(IndexValueMaker i : indexers.get()) {
                valueMakersMap.computeIfAbsent(i.getIndexedEntityClass(), c -> new ArrayList<>()).add(i);
            }

            if(defaultIndexValueMakerRegistry !=null){
                defaultIndexValueMakerRegistry.consumeIndexers(i ->
                    valueMakersMap.computeIfAbsent(i.getIndexedEntityClass(), c -> new ArrayList<>()).add(i)
                );
            }
            return valueMakersMap;
        });


    }
    @Override
    public IndexValueMaker createIndexValueMakerFor(EntityUtils.EntityWrapper<?> ew) {
        Class<?> clazz = ew.getEntityClass();
        List<IndexValueMaker> acceptedList = new ArrayList<>();
        //always add reflecting indexvaluemaker
        acceptedList.add(reflectingIndexValueMaker);

        for (Map.Entry<Class, List<IndexValueMaker>> entry : map.get().entrySet()) {
            if (entry.getKey().isAssignableFrom(clazz)) {
                acceptedList.addAll(entry.getValue());
            }
        }

        return new CombinedIndexValueMaker(clazz, acceptedList);
    }
}
