package ix.core.search.text;


import gsrs.repository.GsrsRepository;
import ix.core.search.LazyList;
import ix.core.search.SearchOptions;
import ix.core.search.SearchResult;
import ix.core.util.EntityUtils.Key;
import lombok.extern.slf4j.Slf4j;
import org.apache.lucene.document.Document;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.TopDocs;

import java.io.IOException;

/**
 * Takes the TopDocs and IndexSearcher from a lucene index, 
 * and uses them to populate a given SearchResult with
 * the expected results. In the current instantiation,
 * the objects returned are deferred rather than direct.
 * 
 * @author peryeata
 *
 */
@Slf4j
class LuceneSearchResultPopulator {
	SearchResult result;
	TopDocs hits;
	IndexSearcher searcher;
	SearchOptions options;
	int total, offset, last=0;
	GsrsRepository gsrsRepository;
	LuceneSearchResultPopulator(GsrsRepository gsrsRepository, SearchResult result, TopDocs hits, IndexSearcher searcher) {
		this.result = result;
		this.hits = hits;
		this.searcher = searcher;
		this.options = result.getOptions();
		result.setCount(hits.totalHits);
		total  = Math.max(0, Math.min(options.max(), result.getCount()));
		offset = Math.min(options.getSkip(), total);
		this.gsrsRepository = gsrsRepository;
	}
	
	
	public void setSearcher(IndexSearcher searcher){
		this.searcher=searcher;
	}

	void fetch() throws IOException, InterruptedException {
		try {
			fetch(total);
		} finally {
			result.done();
		}
	}

	void fetch(int size) throws IOException, InterruptedException {
		size = Math.min(options.getTop(), Math.min(total - offset, size));
		
		int i=last;
		try{
			for (i = last; (i < size) && (i + offset<hits.scoreDocs.length); ++i) {
				if (Thread.interrupted()) {
					throw new InterruptedException();
				}
				
				//This is probably a bad idea
				//because the document id integer may not always remain the same between loads
				//and that can cause a problem
				Document doc = searcher.doc(hits.scoreDocs[i + offset].doc);
				try {
					Key k = Key.of(doc);
					result.addNamedCallable(new EntityFetcher(k, gsrsRepository));
				} catch (Exception e) {
					System.out.println("Record:" + i + " of " + hits.scoreDocs.length);
					e.printStackTrace();
					log.error(e.getMessage());
				}
			}
		}finally{
			this.last=i;
		}
	}

	private static class EntityFetcher implements LazyList.NamedCallable<Key, Object>{
		private Key key;
		private GsrsRepository gsrsRepository;

		public EntityFetcher(Key key, GsrsRepository gsrsRepository) {
			this.key = key;
			this.gsrsRepository = gsrsRepository;
		}

		@Override
		public Object call() {
			return gsrsRepository.findByKey(key).get();
		}

		@Override
		public Key getName() {
			return key;
		}
	}
}