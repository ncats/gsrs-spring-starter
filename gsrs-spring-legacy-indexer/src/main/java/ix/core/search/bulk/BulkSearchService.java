package ix.core.search.bulk;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.LinkedBlockingQueue;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;

import com.fasterxml.jackson.annotation.JsonAutoDetect;

import gsrs.cache.GsrsCache;
import gsrs.repository.GsrsRepository;
import gsrs.springUtils.AutowireHelper;
import ix.core.search.SearchOptions;
import ix.core.search.SearchResult;
import ix.core.search.SearchResultContext;
import ix.core.search.text.TextIndexer;
import ix.core.util.EntityUtils.Key;
import ix.utils.Util;
import lombok.Builder;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;


@Slf4j
@Service
public class BulkSearchService {
	    
	
	@Autowired
	private GsrsCache ixCache;	
	
	@Autowired
	protected PlatformTransactionManager transactionManager;
		
	private final int MAX_BULK_SUB_QUERY_COUNT = 1000;
	private ExecutorService threadPool;
    

	public BulkSearchService() {		
		this(ForkJoinPool.commonPool());		
	}	
	
	public BulkSearchService(ExecutorService tp) {		
		threadPool = tp;		
	}
	
	public void setTransactionManager(PlatformTransactionManager transactionManager) {
		this.transactionManager = transactionManager;
	}
		
	public SearchResultContext search(GsrsRepository gsrsRepository, SanitizedBulkSearchRequest request, 
			SearchOptions options, TextIndexer textIndexer, MatchViewGenerator generator) throws IOException {
		
		String hashKey = request.computeKey(options.getBulkSearchOnIdentifiers());		
		
		
        try {
        	     	
           return ixCache.getOrElse(textIndexer.lastModified() , hashKey, ()-> {
            	
            	SearchOptions optionsCopy = new SearchOptions();
        		optionsCopy.parse(options.asQueryParams());
        		optionsCopy.setSimpleSearchOnly(true);
        		optionsCopy.setTop(options.getTop());		
        		optionsCopy.setSkip(options.getSkip());
        		optionsCopy.setQTop(options.getQTop());		
        		optionsCopy.setQSkip(options.getQSkip());  
        		optionsCopy.setBulkSearchOnIdentifiers(options.getBulkSearchOnIdentifiers());
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
		List<SearchResultSummaryRecord> summaryList = new ArrayList<>();
				
		BulkQuerySummary querySummary = new BulkQuerySummary.BulkQuerySummaryBuilder()
				.qUnMatchTotal(0)
				.searchOnIdentifiers(optionsCopy.getBulkSearchOnIdentifiers())
				.build();
				
		boolean searchOnIdentifiers = optionsCopy.getBulkSearchOnIdentifiers();
		threadPool.submit(() -> {

			try {
				request.getQueries().forEach(q -> {
					
					String query = preProcessQuery(q, searchOnIdentifiers);
					List<Key> keys = new ArrayList<>();					
					
					SearchResult result;
					try {
						result = textIndexer.search(gsrsRepository, optionsCopy, query);
						result.copyKeysTo(keys, 0, MAX_BULK_SUB_QUERY_COUNT, true);
																					
						if(keys.size()==0)
							querySummary.setQUnMatchTotal(1+ querySummary.getQUnMatchTotal());
						
						
						SearchResultSummaryRecord singleQuerySummary = new SearchResultSummaryRecord(q, query);
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
						summaryList.add(singleQuerySummary);						
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
		
		int total = request.getQueries().size();		
		querySummary.setQTotal(total);		
		querySummary.setQueries(summaryList);				
		
		ixCache.setRaw("BulkSearchSummary/"+request.computeKey(optionsCopy.getBulkSearchOnIdentifiers()), querySummary);		
		
		return new ResultEnumeration(bq);

	}
	
	
	private String preProcessQuery(String query, boolean identifiers) {

		query = query.trim();

		// 1. check to see that no field specified
		if (query.matches("^[A-Z0-9a-z_]+[:].+")) { // looks for things like root_names_name: or text:
			return query; // don't try to change any part that has an explicit field. Assume user meant it
							// as-is
		}
		// 2. remove existing quotes
		if (query.startsWith("\"")) {
			query = query.substring(1);
		}
		if (query.endsWith("\"")) {
			query = query.substring(0, query.length() - 1);
		}

		
		if(identifiers) {
			// 3. remove any explicit signifiers if present
			if (query.startsWith("^")) {
				query = query.substring(1);
			}
			if (query.endsWith("$")) {
				query = query.substring(0, query.length() - 1);
			}
			return "\"^" + query + "$\"";			
		}else {
			return "\"" + query + "\"";
		}
	}
	
	@Data
	public static class SanitizedBulkSearchRequest{
			
		private String hash; 
		private List<String> queries;
		
		private String computeHash(boolean identifers) {
			if(hash != null) {
				return hash;
			}else {
				// maybe come back later	
				String flag = (identifers==true?"1":"0");
				hash = queries.stream().sorted()
						.map(q->q.hashCode())
						.reduce((a,b)->a^b)
						.map(r->r+flag)
						.orElse("Empty");	
			
				return hash;				
			}
		}
		
		public String computeKey(boolean identifers){
	            return Util.sha1("bulk/" + computeHash(identifers));
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
	
	@Data
	@Builder
	@JsonAutoDetect(getterVisibility = JsonAutoDetect.Visibility.NONE, fieldVisibility = JsonAutoDetect.Visibility.ANY)
	public static class BulkQuerySummary{
		int qTotal;
		int qTop;
		int qSkip;
		int qMatchTotal;
		int qUnMatchTotal;
		boolean searchOnIdentifiers;
		List<SearchResultSummaryRecord> queries;
		
		public static BulkQuerySummaryBuilder builder() {return new BulkQuerySummaryBuilder();}
		
	}
}
