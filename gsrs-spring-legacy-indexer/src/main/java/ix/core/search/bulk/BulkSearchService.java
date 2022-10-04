package ix.core.search.bulk;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import gsrs.cache.GsrsCache;
import gsrs.repository.GsrsRepository;
import gsrs.springUtils.AutowireHelper;
import ix.core.models.BaseModel;
import ix.core.search.SearchOptions;
import ix.core.search.SearchResult;
import ix.core.search.SearchResultContext;
import ix.core.search.text.TextIndexer;
import ix.core.search.text.TextIndexerFactory;
import ix.core.util.EntityUtils;
import ix.core.util.EntityUtils.EntityWrapper;
import ix.core.util.EntityUtils.Key;
import ix.utils.Util;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;


@Slf4j
@Service
public class BulkSearchService {
	    
	
	@Autowired
	private GsrsCache ixCache;	
		
	private final int MAX_BULK_SUB_QUERY_COUNT = 1000;
	private ExecutorService threadPool;
    

	public BulkSearchService() {		
		this(ForkJoinPool.commonPool());		
	}	
	
	public BulkSearchService(ExecutorService tp) {		
		threadPool = tp;		
	}
		
	public SearchResultContext search(GsrsRepository gsrsRepository, SanitizedBulkSearchRequest request, 
			SearchOptions options, TextIndexer textIndexer, MatchViewGenerator generator) throws IOException {
		
		String hashKey = request.computeKey();		
		
        try {
        	     	
           return ixCache.getOrElse(textIndexer.lastModified() , hashKey, ()-> {
            	
            	SearchOptions optionsCopy = new SearchOptions();
        		optionsCopy.parse(options.asQueryParams());
        		optionsCopy.setSimpleSearchOnly(true);
        		optionsCopy.setTop(100);		
        		optionsCopy.setSkip(0);
        		optionsCopy.setFetchAll();       		
        		       		
        		BulkSearchResultProcessor processor = new BulkSearchResultProcessor(ixCache);              
        		processor = AutowireHelper.getInstance().autowireAndProxy(processor);
            	
        		processor.setResults(1, rawSearch(gsrsRepository, request, optionsCopy, textIndexer, generator));        			
        		SearchResultContext ctx = processor.getContext();
                ctx.setKey(hashKey);

                return ctx;  
            });             
                
        } catch (Exception e) {
            throw new IOException("error performing search ", e);
        }      
	}	
	
	private ResultEnumeration rawSearch(GsrsRepository gsrsRepository, SanitizedBulkSearchRequest request, 
			SearchOptions optionsCopy, TextIndexer textIndexer, MatchViewGenerator generator) {
		BlockingQueue<BulkSearchResult> bq = new LinkedBlockingQueue<BulkSearchResult>();		
		List<SearchResultSummaryRecord> summary = new ArrayList<>();
		
		threadPool.submit(() -> {

			try {
				request.getQueries().forEach(q -> {

					List<Key> keys = new ArrayList<>();					
					
					SearchResult result;
					try {
						result = textIndexer.search(gsrsRepository, optionsCopy, q);
						result.copyKeysTo(keys, 0, MAX_BULK_SUB_QUERY_COUNT, true);
																		
						SearchResultSummaryRecord singleQuerySummary = new SearchResultSummaryRecord(q);
						List<MatchView> list = new ArrayList<>();						
						
						
						keys.forEach((k) -> {
							BulkSearchResult bsr = new BulkSearchResult();
							bsr.setQuery(q);
							bsr.setKey(k);
							bq.add(bsr);
							MatchView mv = generator.generate(bsr);
							list.add(mv);
						});
						singleQuerySummary.setRecords(list);
						summary.add(singleQuerySummary);						
					} catch (IOException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
					
				});
				

			} catch (Throwable e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} finally {
				bq.add(POISON_RESULT);
			}			

		});
		
		ixCache.setRaw("BulkSearchSummary/"+request.computeKey(), summary);		
		
		return new ResultEnumeration(bq);

	}
	
	
	@Data
	public static class SanitizedBulkSearchRequest{
			
		private String hash; 
		private List<String> queries;
		
		private String computeHash() {
			if(hash != null) {
				return hash;
			}else {
				// maybe come back later
				hash = queries.stream().sorted()
						.map(q->q.hashCode())
						.reduce((a,b)->a^b)
						.map(r->r+"")
						.orElse("Empty");
				return hash;
			}
		}
		
		public String computeKey(){
	            return Util.sha1("bulk/" + computeHash());
	        }
	}
	
	public static final BulkSearchResult POISON_RESULT = new BulkSearchResult();
	
	
	public static class ResultEnumeration implements Enumeration<BulkSearchResult> {
        final BlockingQueue<BulkSearchResult> queue;
        BulkSearchResult next;

        public ResultEnumeration (BlockingQueue<BulkSearchResult> queue) {
            this.queue = queue;
            if(queue==null){
                next=POISON_RESULT;
            }else{
                next ();
            }
        }

        void next () {
            try {
                next = queue.take();
            }
            catch (Exception ex) {
                log.error(ex.getMessage(), ex);
                next = POISON_RESULT; // terminate
            }
        }           

        public boolean hasMoreElements () {
            return next != POISON_RESULT;
        }

        public BulkSearchResult nextElement () {
            if(!hasMoreElements()){
                throw new NoSuchElementException();
            }
            BulkSearchResult current = next;
            next ();            
            return current;
        }
    }	
}
