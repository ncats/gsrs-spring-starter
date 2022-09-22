package gsrs.legacy;

import java.io.IOException;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;

import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.search.Filter;
import org.apache.lucene.search.Query;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.transaction.annotation.Transactional;

import gsrs.repository.GsrsRepository;
import gsrs.security.hasAdminRole;
import gsrs.services.TextService;
import ix.core.EntityFetcher;
import ix.core.search.SearchOptions;
import ix.core.search.SearchResult;
import ix.core.search.text.TextIndexer;
import ix.core.search.text.TextIndexerFactory;
import ix.core.util.EntityUtils;
import ix.core.util.EntityUtils.EntityWrapper;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public abstract class LegacyGsrsSearchService<T> implements GsrsSearchService<T>{

    @Autowired
    private TextIndexerFactory textIndexerFactory;
    
    @Autowired
    private TextService textService;

    private final GsrsRepository gsrsRepository;
    private final Class<T> entityClass;

    private AtomicBoolean reindexing = new AtomicBoolean(false);

    protected LegacyGsrsSearchService(Class<T> entityClass, GsrsRepository<T, ?> repository){
        gsrsRepository= repository;
        this.entityClass = entityClass;

    }

    @Override
    public long getLastModified() {
        return textIndexerFactory.getDefaultInstance().lastModified();
    }

    @Override
    public Collection<String> getSuggestFields() throws IOException {
        return textIndexerFactory.getDefaultInstance().getSuggestFields();
    }

    @Override
    public List<? extends GsrsSuggestResult> suggestField(String field, String query, int max) throws IOException {
        TextIndexer defaultInstance = textIndexerFactory.getDefaultInstance();
        return defaultInstance.suggest(field, query, max);

    }
    @Override
    public Map<String, List<? extends GsrsSuggestResult>> suggest(String query, int max) throws IOException {
        TextIndexer defaultInstance = textIndexerFactory.getDefaultInstance();
        Map<String, List<? extends GsrsSuggestResult>> map = new LinkedHashMap<>();
        for(String field: defaultInstance.getSuggestFields()){
            List<? extends GsrsSuggestResult> suggest = defaultInstance.suggest(field, query, max);
            if(suggest !=null && !suggest.isEmpty()) {
                map.put(field, suggest);
            }
        }
        return map;

    }
    @Override
    public SearchResult search(String query, SearchOptions options) throws IOException {
        return textIndexerFactory.getDefaultInstance().search(gsrsRepository, options, query);
    }
    
    public SearchResult bulkSearch(String queryID, String bulkQuery, String query, SearchOptions options) throws IOException {
    	return textIndexerFactory.getDefaultInstance().bulkSearch(gsrsRepository, queryID, options, bulkQuery, query);
    }

    @Override
    public SearchResult search(String query, SearchOptions options, Collection<?> subset) throws IOException {
        return textIndexerFactory.getDefaultInstance().search(gsrsRepository, options, query, subset);
    }

    @Override
    public TextIndexer.TermVectors getTermVectors(Optional<String> field) throws IOException {
        try {
            return textIndexerFactory.getDefaultInstance().getTermVectors(entityClass, field.orElse(null));
        } catch (Exception e) {
            throw new IOException("error generating term vectors", e);
        }
    }

    @Override
    public TextIndexer.TermVectors getTermVectorsFromQuery(String query, SearchOptions options, String field) throws IOException {
        TextIndexer indexer = textIndexerFactory.getDefaultInstance();
        try {
            Query q= indexer.extractFullFacetQuery(query, options, field);
            return indexer.getTermVectors(entityClass, field, (Filter)null, q);
        } catch (ParseException e) {
            throw new IOException("error parsing lucene query '" + query + "'", e);
        }catch(Exception e){
            throw new IOException("error getting term vectors ", e);
        }
    }
    

    @hasAdminRole
    @Transactional( readOnly= true)
    public void reindexAndWait(boolean wipeIndexFirst){
        //only run if we are not reindexing
        if(reindexing.compareAndSet(false, true)) {

            try {
                TextIndexer indexer = textIndexerFactory.getDefaultInstance();
                if (wipeIndexFirst) {
                    indexer.clearAllIndexes(false);
                } else {
                    EntityUtils.getEntityInfoFor(entityClass).getTypeAndSubTypes()
                            .forEach(ei -> {
                                try {
                                    indexer.removeAllType(ei.getEntityClass());
                                } catch (Exception e) {
                                    e.printStackTrace();
                                }
                            });

                }
                Pageable pageRequest = PageRequest.of(0, 200);
                Page<T> onePage = gsrsRepository.findAll(pageRequest);

                while (!onePage.isEmpty()) {
                    pageRequest = pageRequest.next();
                    //DO SOMETHING WITH ENTITIES
                    onePage.forEach(entity -> {
                        EntityWrapper ew = EntityUtils.EntityWrapper.of(entity);
                        try {
                            Object v=ew.getValue();
                            //If it can't fetch for some reason, like an invalid
                            //transaction, this should fix that by fetching
                            //fully with the EF
                            try {
                                ew.toInternalJsonNode();
                            }catch(Exception e) {
                                EntityFetcher ef = EntityFetcher.of(ew.getKey());
                                v = ef.call();
                            }
                            indexer.add(EntityWrapper.of(v));
                        } catch (Exception e) {
                            log.warn("Error reindexing:" + ew.getOptionalKey(),e);
                        }
                    });

                    onePage = gsrsRepository.findAll(pageRequest);
                }
            }finally {
                reindexing.set(false);
            }
        }
    }
}
