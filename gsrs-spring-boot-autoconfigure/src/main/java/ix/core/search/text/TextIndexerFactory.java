package ix.core.search.text;

import gsrs.indexer.IndexValueMakerFactory;
import ix.core.util.EntityUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class TextIndexerFactory {

    private Set<String> deepKinds;

    private ConcurrentMap<File, TextIndexer> indexers = new ConcurrentHashMap<>();

    private Map<String, Boolean> deepKindMap = new ConcurrentHashMap<>();
    @Value("${ix.home}")
    private String defaultDir;
    @Autowired
    private TextIndexerConfig textIndexerConfig;

    @Autowired
    private IndexerServiceFactory indexerServiceFactory;

    @Autowired
    public IndexValueMakerFactory indexValueMakerFactory;

    private TextIndexer defaultIndexer;

    @PostConstruct
    private void init(){
        // this logic was taken from the static init method of the Play G-SRS TextIndexer and moved to a new factory
        //so it could be used with dependency injection

        deepKinds = textIndexerConfig.getDeepFields()
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
                .collect(Collectors.toSet());

        defaultIndexer = getInstance(new File(defaultDir));
    }

    public TextIndexer getInstance(File baseDir){
        return indexers.computeIfAbsent(baseDir, dir -> {
            try {
                return new TextIndexer(dir, indexerServiceFactory, indexerServiceFactory.createForDir(dir), textIndexerConfig,indexValueMakerFactory,  this::isDeepKind);
            } catch (IOException ex) {
                ex.printStackTrace();
                return null;
            }
        });
    }


        private boolean isDeepKind(EntityUtils.EntityWrapper ew){
        return deepKindMap.computeIfAbsent(ew.getKind(), k->deepKinds.contains(k));
    }

    public TextIndexer getDefaultInstance(){
        return defaultIndexer;
    }
}
