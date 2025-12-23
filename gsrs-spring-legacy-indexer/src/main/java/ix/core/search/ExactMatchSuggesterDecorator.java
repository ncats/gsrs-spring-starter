package ix.core.search;

import java.io.Closeable;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Supplier;

import org.apache.lucene.index.BinaryDocValues;
import org.apache.lucene.index.MultiDocValues;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.Collector;
import org.apache.lucene.search.EarlyTerminatingSortingCollector;
import org.apache.lucene.search.FieldDoc;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.SearcherManager;
import org.apache.lucene.search.Sort;
import org.apache.lucene.search.SortField;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.search.TopFieldCollector;
import org.apache.lucene.search.TopFieldDocs;
import org.apache.lucene.search.suggest.InputIterator;
import org.apache.lucene.search.suggest.Lookup;
import org.apache.lucene.search.suggest.analyzing.AnalyzingInfixSuggester;
import org.apache.lucene.store.DataInput;
import org.apache.lucene.store.DataOutput;
import org.apache.lucene.util.BytesRef;

import lombok.extern.slf4j.Slf4j;

/**
 * Created by katzelda on 2/27/17.
 * 
 * This wraps a {@link AnalyzingInfixSuggester} and allows it to promote exact matches to the top
 * which is something the existing suggester doesn't do. To do this it uses reflection to find the
 * index searcher attributes and do a pre-screen search before the standard suggest search.
 */
@Slf4j
public class ExactMatchSuggesterDecorator extends Lookup implements Closeable {

	// This is used as a hack to get the search manager used internally to the
    // AnalyzingInfixSuggester, which hides it. This is used because we need
    // to adjust exact match results
    private static <T>  T getFieldValue(Object obj, String fieldName){
        try {
            java.lang.reflect.Field f = obj.getClass().getDeclaredField(fieldName);
            try {
            	f.setAccessible(true);
            }catch(Exception e) {
            	log.warn("Unable to mark field as accessible:" +fieldName ,e);
            }
            return (T) f.get(obj);
        }catch(NoSuchFieldException  | IllegalAccessException e){
            throw new RuntimeException(e);
        }
    }
	
    private static final String TEXT_FIELD_NAME = "text";
    private final static String EXACT_TEXT_FIELD_NAME = "exacttext";

    private static final Sort SORT2 = new Sort(new SortField("weight", SortField.Type.LONG, true));


    private static final Function<String, String> DEFAULT_KEY_TRANSFORMATION_FUNCTION = r2Key -> {
        //for some reason the super lookup's key is the highlight!!
        //which adds bold tag around the match
        return r2Key.replaceAll("<b>(.+?)</b>", "$1");
    };

    private final AnalyzingInfixSuggester delegate;

    private final Supplier<SearcherManager> searcherMgr;

    private final Function<String, String> keyTransformationFunction;
/*
public InxightInfixSuggester(Version matchVersion, Directory dir,
			Analyzer analyzer) throws IOException {
		super(matchVersion, dir, analyzer);
	}
 */
    public ExactMatchSuggesterDecorator(AnalyzingInfixSuggester delegate) {
        this(delegate, ()->{
        	return getFieldValue(delegate, "searcherMgr");
        }, DEFAULT_KEY_TRANSFORMATION_FUNCTION);
    }
    public ExactMatchSuggesterDecorator(AnalyzingInfixSuggester delegate, Supplier<SearcherManager> searchMgr) {
        this(delegate, searchMgr, DEFAULT_KEY_TRANSFORMATION_FUNCTION);
    }
    public ExactMatchSuggesterDecorator(AnalyzingInfixSuggester delegate, Supplier<SearcherManager> searchMgr, Function<String, String> keyTransformationFunction) {
        this.delegate = Objects.requireNonNull(delegate);
        this.searcherMgr = Objects.requireNonNull(searchMgr);
        this.keyTransformationFunction = Objects.requireNonNull(keyTransformationFunction);
    }

   public AnalyzingInfixSuggester getDelegate(){
       return delegate;
   }
    @Override
    public long getCount() throws IOException {
        return delegate.getCount();
    }

    @Override
    public void build(InputIterator inputIterator) throws IOException {
        delegate.build(inputIterator);
    }

    @Override
    public List<LookupResult> lookup(CharSequence key, Set<BytesRef> contexts, boolean onlyMorePopular, int num) throws IOException {
        //katzelda 2/2017
        // this is the real lookup method
        //the problem is our weights are bad and we can get tons of hits with the same weight
        //so we want to re-order it so exact matches are first.
        //
        //My first attempt was to just reorder the returned list but that doesn't work
        //because we might miss an exact match in the limited number of results returned
        //
        //so the solution is to do an exact search query first and then
        //append the normal suggest results after dealing with duplicate hits.

        log.trace("in lookup with key: {}", key);
        if(key== null || key.length()==0) {
            return Collections.emptyList();
        }
        List<LookupResult> exactMatches = getExactHitsFor(key, num);

        // return lookup(key, contexts, num, true, true);
        List<LookupResult>  superMatches =  delegate.lookup(key, contexts,onlyMorePopular,  num + exactMatches.size());
        if(exactMatches.isEmpty()){
            return superMatches;
        }

        //remove any duplicate exact matches
        exactMatches.forEach( r -> {
            Iterator<LookupResult> iter = superMatches.iterator();
            String rKey = r.key.toString();

            while(iter.hasNext()){
                LookupResult r2 = iter.next();
                String r2Key = keyTransformationFunction.apply(r2.key.toString());

                if(rKey.equalsIgnoreCase(r2Key)){
                    //duplicate
                    iter.remove();

                }
            }

        });


        List<LookupResult> combinedList = new ArrayList<>(exactMatches.size() + superMatches.size());
        combinedList.addAll(exactMatches);
        combinedList.addAll(superMatches);

        //in case we get an exact match that isn't included in the super call limit the size of the return to num
        if(combinedList.size() > num) {
            return combinedList.subList(0, num);
        }
        return combinedList;
    }

    @Override
    public boolean store(DataOutput output) throws IOException {
        return delegate.store(output);
    }

    @Override
    public boolean load(DataInput input) throws IOException {
        return delegate.load(input);
    }

    @Override
    public long ramBytesUsed() {
        return delegate.ramBytesUsed();
    }

    private List<LookupResult> getExactHitsFor(CharSequence query, int num){
        Term t=new Term(EXACT_TEXT_FIELD_NAME, new BytesRef(query).utf8ToString());
        TermQuery tq=new TermQuery(t);

        IndexSearcher searcher=null;
        SearcherManager manager=null;
        try{
            // Sort by weight, descending:
            TopFieldCollector c = TopFieldCollector.create(SORT2, num, true, false, false);

            // We sorted postings by weight during indexing, so we
            // only retrieve the first num hits now:
            Collector c2 = new EarlyTerminatingSortingCollector(c, SORT2, num,SORT2);
            manager = searcherMgr.get();
            searcher = manager.acquire();
            searcher.search(tq, c2);

            TopFieldDocs hits = (TopFieldDocs) c.topDocs();

            if(hits.totalHits ==0){
                return Collections.emptyList();
            }
            BinaryDocValues textDV = MultiDocValues.getBinaryValues(searcher.getIndexReader(), TEXT_FIELD_NAME);
            String queryStr = query.toString();
            List<LookupResult> exactMatches = new ArrayList<>();
            for (int i=0;i<hits.scoreDocs.length;i++) {
                FieldDoc fd = (FieldDoc) hits.scoreDocs[i];
                BytesRef term = textDV.get(fd.doc);
                String text = term.utf8ToString();
                long score = (Long) fd.fields[0];
                //don't return negative scores
                if(text.equalsIgnoreCase(queryStr) && score>=0){
                    //used by TextIndexer like this
                    //.map(r -> new SuggestResult(r.payload.utf8ToString(), r.key, r.value))
                    //key, highlight, weight
                    exactMatches.add(new LookupResult(query, "<b>"+queryStr + "</b>", Integer.MAX_VALUE, new BytesRef(query)));
                }
            }
            return exactMatches;
        }catch(Exception e){
            throw new RuntimeException(e);
        } finally{
            if(manager!=null){
                try{
                    manager.release(searcher);
                }catch(Exception e){
                    e.printStackTrace();
                }
            }
        }
    }

    /**
     *
     * This method will get the assigned weight for the exact match text provided
     *
     * @param text
     * @return
     */
    public long getWeightFor(BytesRef text){
        IndexSearcher searcher=null;
        SearcherManager manager=null;
        try{
            Term t=new Term(EXACT_TEXT_FIELD_NAME, text.utf8ToString());
            TermQuery tq=new TermQuery(t);

            // Sort by weight, descending:
            TopFieldCollector c = TopFieldCollector.create(SORT2, 2, true, false, false);

            // We sorted postings by weight during indexing, so we
            // only retrieve the first num hits now:
            Collector c2 = new EarlyTerminatingSortingCollector(c, SORT2, 2, SORT2);
            manager = searcherMgr.get();
            searcher = manager.acquire();
            searcher.search(tq, c2);

            TopFieldDocs hits = (TopFieldDocs) c.topDocs();
            if(hits.totalHits>=1){
                int i=0;
                FieldDoc fd = (FieldDoc) hits.scoreDocs[i];
                long score = (Long) fd.fields[0];
                return score;
            }
        }catch(Exception e){
            e.printStackTrace();
        } finally{
            if(manager!=null){
                try{
                    manager.release(searcher);
                }catch(Exception e){
                    e.printStackTrace();
                }
            }
        }

        return 0;
    }
    
	@Override
	public void close() throws IOException {
		if(this.delegate instanceof Closeable){
			((Closeable)this.delegate).close();
		}
	}
	public void update(BytesRef ref, Set<BytesRef> context, long p, BytesRef ref2) throws IOException {
		delegate.update(ref,context, p, ref2);
	}
	public void refresh() throws IOException {
		delegate.refresh();
	}
}
