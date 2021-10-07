package gsrs.legacy;

import gsrs.indexer.IndexValueMakerFactory;
import gsrs.repository.GsrsRepository;
import ix.core.search.SearchOptions;
import ix.core.search.SearchResult;
import ix.core.search.text.TextIndexer;
import ix.core.search.text.TextIndexerFactory;
import ix.core.util.EntityUtils;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.search.Query;
import org.springframework.beans.factory.annotation.Autowired;
import org.apache.lucene.search.Filter;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.util.*;

public abstract class LegacyGsrsSearchService<T> implements GsrsSearchService<T>{

    @Autowired
    private TextIndexerFactory textIndexerFactory;

    private final GsrsRepository gsrsRepository;
    private final Class<T> entityClass;

    protected LegacyGsrsSearchService(Class<T> entityClass, GsrsRepository<T, ?> repository){
        gsrsRepository= repository;
        this.entityClass = entityClass;

    }

    @Override
    public Class<T> getEntityClass() {
        return entityClass;
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
    @Transactional( readOnly= true)
    public void reindexAndWait(boolean wipeIndexFirst){
        TextIndexer indexer = textIndexerFactory.getDefaultInstance();
        if(wipeIndexFirst) {
            indexer.deleteAll();
        }else{
            EntityUtils.getEntityInfoFor(entityClass).getTypeAndSubTypes()
                    .forEach(ei->{
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
                try {
                    indexer.add(EntityUtils.EntityWrapper.of(entity));
                } catch (IOException e) {
                    e.printStackTrace();
                }
            });

            onePage = gsrsRepository.findAll(pageRequest);
        }
    }
}
