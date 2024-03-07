package gsrs.legacy;

import java.io.IOException;
import java.util.ArrayList;
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

import com.fasterxml.jackson.databind.JsonNode;

import gsrs.indexer.IndexerEntityListener;
import gsrs.repository.GsrsRepository;
import gsrs.security.hasAdminRole;
import ix.core.EntityFetcher;
import ix.core.search.SearchOptions;
import ix.core.search.SearchResult;
import ix.core.search.SearchResultContext;
import ix.core.search.bulk.BulkSearchService;
import ix.core.search.bulk.BulkSearchService.SanitizedBulkSearchRequest;
import ix.core.search.bulk.MatchViewGenerator;
import ix.core.search.text.TextIndexer;
import ix.core.search.text.TextIndexerFactory;
import ix.core.util.EntityUtils;
import ix.core.util.EntityUtils.EntityWrapper;
import ix.core.util.EntityUtils.Key;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public abstract class LegacyGsrsSearchService<T> implements GsrsSearchService<T>{

    @Autowired
    private TextIndexerFactory textIndexerFactory;
    
    private IndexerEntityListener indexerEntityListener;

    private final GsrsRepository gsrsRepository;
    
    private final Class<T> entityClass;    
   
    @Autowired
    private BulkSearchService bulkSearchService;
    
    private MatchViewGenerator matchViewGenerator;

    private AtomicBoolean reindexing = new AtomicBoolean(false);

    protected LegacyGsrsSearchService(Class<T> entityClass, GsrsRepository<T, ?> repository){
        this(entityClass, repository, new MatchViewGenerator() {});
    }

    protected LegacyGsrsSearchService(Class<T> entityClass, GsrsRepository<T, ?> repository, MatchViewGenerator generator){
        gsrsRepository= repository;
        this.entityClass = entityClass;
        matchViewGenerator = generator;
        indexerEntityListener = new IndexerEntityListener();
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

    @Override
    public SearchResult search(String query, SearchOptions options, Collection<?> subset) throws IOException {
        return textIndexerFactory.getDefaultInstance().search(gsrsRepository, options, query, subset);
    }
    
    public SearchResultContext bulkSearch(SanitizedBulkSearchRequest request, SearchOptions options) throws IOException {    		
    	return bulkSearchService.search(gsrsRepository, request, options, textIndexerFactory.getDefaultInstance(), matchViewGenerator);
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
            log.trace("getTermVectorsFromQuery using kind {}", options.getKind());
            return indexer.getTermVectors(entityClass, field, (Filter)null, q);
        } catch (ParseException e) {
            throw new IOException("error parsing lucene query '" + query + "'", e);
        }catch(Exception e){
            throw new IOException("error getting term vectors ", e);
        }
    }

    public TextIndexer.TermVectors getTermVectorsFromQueryNew(String query, SearchOptions options, String field) throws IOException {
        TextIndexer indexer = textIndexerFactory.getDefaultInstance();
        try {
            Query q= indexer.extractFullFacetQuery(query, options, field);
            log.trace("getTermVectorsFromQuery using kind {}", options.getKind());
            return indexer.getTermVectors(options.getKind()!=null ? options.getKind() : entityClass, field, (Filter)null, q);
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
                            indexer.add(EntityWrapper.of(v), false); //reindex event: including external IVMs
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
    
    public void reindex(Object entity, boolean deleteFirst){
    	reindex(entity, deleteFirst, true);    	
    }
    
    public void reindex(Object entity, boolean deleteFirst, boolean excludeExternal){
    	//this ensures that the reindexing is done recursively
    	//TODO: technically this will not handle the cases where 
    	// a child element which is indexed at root had been deleted via the database
    	// and a reindexing is triggered. For example, a name of a substance is deleted
    	// in the database and the substance is marked for reindexing. While the reindexing
    	// of the substance fields will work, the reindexing of the NAME will not delete
    	// the now orphaned index. To do this, we would need to add some field to the name
    	// which specifies its parent substance and have another step to delete all such elements
    	// 
    	// In practice this case is rare, but it may become more common. This must be addressed
    	// eventually
    	
    	EntityWrapper wrapper =EntityWrapper.of(entity);
    	wrapper.traverse().execute((p, child) -> {
            EntityUtils.EntityWrapper<EntityUtils.EntityWrapper> wrapped = EntityUtils.EntityWrapper.of(child);
            //this should speed up indexing so that we only index
            //things that are roots.  the actual indexing process of the root should handle any
            //child objects of that root.
            boolean isEntity = wrapped.isEntity();
            boolean isRootIndexed = wrapped.isRootIndex();

            if (isEntity && isRootIndexed) {
                try {
                    indexerEntityListener.reindexEntity(wrapped.getRawValue(), deleteFirst, excludeExternal);
                } catch (Throwable t) {
                    log.warn("indexing error handling:" + wrapped, t);
                }
            }
        });    	
    }
    
    
    public Key toKey(String id) {
    	return Key.ofStringId(entityClass, id);
    }
    
    
    public TextIndexer.IndexRecord getIndexData(Key k) throws IOException {
    	TextIndexer indexer = textIndexerFactory.getDefaultInstance();
    	
    	try {
			return indexer.getIndexRecord(k);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
    	return null;
    }
        
    
}
