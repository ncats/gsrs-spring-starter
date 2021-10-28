package ix.core.search.text;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.HashSet;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import gov.nih.ncats.common.util.CachedSupplier;
import gsrs.cache.GsrsCache;
import gsrs.indexer.IndexValueMakerFactory;
import ix.core.util.EntityUtils;

@Service
public class TextIndexerFactory {

    private Set<String> deepKinds;

    private ConcurrentMap<File, TextIndexer> indexers = new ConcurrentHashMap<>();

    @Value("${ix.home:ginas.ix}")
    private String defaultDir;
    @Autowired
    private TextIndexerConfig textIndexerConfig;

    @Autowired
    private IndexerServiceFactory indexerServiceFactory;
    

    @Autowired
    private GsrsCache gsrsCache;

    @Autowired
    public IndexValueMakerFactory indexValueMakerFactory;

    private TextIndexer defaultIndexer;

    private final CachedSupplier<Void> initializer = IndexValueMakerFactory.INDEX_VALUE_MAKER_INTIALIZATION_GROUP.add(CachedSupplier.ofInitializer(()->{
        defaultIndexer = getInstanceWithoutSync(new File(defaultDir));
        // this logic was taken from the static init method of the Play G-SRS TextIndexer and moved to a new factory
        //so it could be used with dependency injection

        deepKinds = Optional.ofNullable(textIndexerConfig)
                .map(cf->cf.getDeepFields()
                .stream()
                .map(s->{
                    try{
                        return EntityUtils.getEntityInfoFor(s).getTypeAndSubTypes();
                    }catch(Exception e){
                        e.printStackTrace();
                        return null;
                    }
                })
                .filter(Objects::nonNull)
                .flatMap(Collection::stream)
                .map(ei->ei.getName())
                .collect(Collectors.toSet())
                ).orElse(new HashSet<>());


    }));
//    @PostConstruct
//    private void init(){
//        initializer.getSync();
//    }

    public TextIndexer getInstance(File baseDir){
        initializer.getSync();
        return getInstanceWithoutSync(baseDir);
    }
    private TextIndexer getInstanceWithoutSync(File baseDir){

        return indexers.computeIfAbsent(baseDir, dir -> {
            try {
                return new TextIndexer(dir, indexerServiceFactory, indexerServiceFactory.createForDir(dir), textIndexerConfig,indexValueMakerFactory, gsrsCache,  this::isDeepKind);
            } catch (IOException ex) {
                ex.printStackTrace();
                return null;
            }
        });
    }


    private boolean isDeepKind(EntityUtils.EntityWrapper ew){
        return deepKinds.contains(ew.getKind());
    }

    public TextIndexer getDefaultInstance(){
        initializer.getSync();
        return defaultIndexer;
    }
}
