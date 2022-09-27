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

import gsrs.cache.GsrsCache;
import gsrs.repository.GsrsRepository;
import gsrs.springUtils.AutowireHelper;
import gsrs.util.SanitizerUtil;
import ix.core.search.SearchOptions;
import ix.core.search.SearchRequest;
import ix.core.search.SearchResult;
import ix.core.search.SearchResultContext;
import ix.core.search.text.TextIndexer;
import ix.core.search.text.TextIndexerFactory;
import ix.core.util.EntityUtils.Key;
import ix.utils.Util;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

@Service
@Data
@Slf4j
public class BulkSearchService {
		
    @Autowired
    private TextIndexerFactory textIndexerFactory;
	
	private final GsrsRepository gsrsRepository;
	
	private TextIndexer textIndexer;
	
	@Autowired
	private GsrsCache ixCache;
	
	private final int MAX_BULK_SUB_QUERY_COUNT = 1000;
	private ExecutorService threadPool;
	
	public BulkSearchService(GsrsRepository gsrsRepository) {		
		this(gsrsRepository, ForkJoinPool.commonPool());
	}
	
	public BulkSearchService(GsrsRepository gsrsRepository, ExecutorService tp) {
		this.gsrsRepository = gsrsRepository;
		textIndexer = textIndexerFactory.getDefaultInstance();
		threadPool = tp;
	}
	
	
//	SearchResultContext bulkSearch(String queryID, SearchOptions options, String queryText) {
//				
//		SearchOptions optionsCopy = new SearchOptions();
//		optionsCopy.parse(options.asQueryParams());
//		optionsCopy.setSimpleSearchOnly(true);
//		optionsCopy.setTop(100);		
//		optionsCopy.setSkip(0);
//		optionsCopy.setFetchAll();
//		
//		//todo: ixcache getOrElse()		
//		List<String> queries = Arrays.asList(queryText.split("\\s*,\\s*"));	
//		queries = queries.stream().distinct().collect(Collectors.toList());
//		Map<Key, List<String>> matchContexts = new ConcurrentHashMap<>();		
//		List<Tuple<String,List<Key>>> statistics = queries.stream().map(q->{
//			Tuple<String,List<Key>> keys = Tuple.of(q, new ArrayList<>());			
//			SearchResult r;			
//			try {			
//				//todo: we can optimize based on identifiers or complex searches
//				r = textIndexerFactory.getDefaultInstance().search(gsrsRepository, optionsCopy, q);
//				r.copyKeysTo(keys.v(), 0, 100, true);				
//			} catch (IOException e) {
//				// TODO Auto-generated catch block
//				e.printStackTrace();
//			}			
//			return keys;
//		}).collect(Collectors.toList());
//				
//		List<Key> subset = statistics.stream()
//		          .flatMap(t->t.v().stream().map(v->Tuple.of(t.k(), v)))
//                  .peek(t->{
//                	  matchContexts.computeIfAbsent(t.v(),(k)->new ArrayList<>());
//                	  List<String> values = matchContexts.get(t.v());
//                	  if(!values.contains(t.k()))
//                		  values.add(t.k());})
//                  .map(t->t.v())
//		          .distinct()
//		          .peek(System.out::println)
//		          .collect(Collectors.toList());
//		// todo: look into matchContexts to set up cache
//				
//		matchContexts.forEach((k,v)->{
//			Map<String,Object> map = new HashMap<>();
//			map.put("queries", v);
//			ixCache.setMatchingContext("matchBulkSearchQueries" + queryID, k, map);});
//						
//		ixCache.setRaw("matchBulkSearchStatistics" + queryID, statistics);
//		
//		return textIndexerFactory.getDefaultInstance().search(gsrsRepository, options, null, subset);	
//		
//	}	
	
	
//	public ResultEnumeration search (String query, SearchOptions optionsCopy) {
//		
//		if (query == null || query.length() == 0) {
//            return new ResultEnumeration(null);
//        }
//        final BlockingQueue<BulkSearchResult> out = new LinkedBlockingQueue<>();
//        threadPool.submit(()->{
//            //TODO katzelda turn off stopwatch for now
////            ix.core.util.StopWatch.timeElapsed(()->{
//                try {
//                    search (out, query, identity, gap, rt, seqType);
//                }catch (Exception ex) {
//
//                    log.warn("trouble searching sequence", ex);
//                }finally{
//                    try {
//                        out.put(POISON_RESULT);// finish
//                    }catch (InterruptedException e) {
//                        log.error(e.getMessage(), e);
//                    } 
//                }
////            });
//
//        });
//
//        return new ResultEnumeration (out);
//		
//		SearchResult = textIndexer.search(gsrsRepository, optionsCopy, query);
//		
//	}
	
	
	public SearchResultContext search(SanitizedBulkSearchRequest request, SearchOptions options) throws IOException {
		
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
            	
        		processor.setResults(1, rawSearch(request, optionsCopy));        			
        		SearchResultContext ctx = processor.getContext();
                ctx.setKey(hashKey);

                return ctx;  
            });             
                
        } catch (Exception e) {
            throw new IOException("error performing search ", e);
        } 
        
      
	}
	
	
	private ResultEnumeration rawSearch(SanitizedBulkSearchRequest request, SearchOptions optionsCopy) {
		BlockingQueue<BulkSearchResult> bq = new LinkedBlockingQueue<BulkSearchResult>();
		
		threadPool.submit(()->{
			
			try {
				request.getQueries().forEach(q->{
			
				List<Key> keys = new ArrayList<>();
				SearchResult result;
				try {
					result = textIndexer.search(gsrsRepository, optionsCopy, q);
					result.copyKeysTo(keys, 0, MAX_BULK_SUB_QUERY_COUNT, true);	     			
					
					keys.forEach((k)->{
						BulkSearchResult bsr = new BulkSearchResult();
						bsr.setQuery(q);
						bsr.setKey(k);
						bq.add(bsr);
					});
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}			
				 
	        });
			}catch(Throwable e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}finally {
				bq.add(POISON_RESULT);
			}
			
		});
		
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
