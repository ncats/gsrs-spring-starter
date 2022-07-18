package ix.core.search.text;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.util.concurrent.Striped;
import gov.nih.ncats.common.Tuple;
import gov.nih.ncats.common.functions.ThrowableFunction;
import gov.nih.ncats.common.io.IOUtil;
import gov.nih.ncats.common.sneak.Sneak;
import gov.nih.ncats.common.stream.StreamUtil;
import gov.nih.ncats.common.util.CachedSupplier;
import gov.nih.ncats.common.util.TimeUtil;
import gsrs.cache.GsrsCache;
import gsrs.indexer.IndexValueMakerFactory;
import gsrs.legacy.GsrsSuggestResult;
import gsrs.repository.GsrsRepository;
import ix.core.EntityFetcher;
import ix.core.FieldNameDecorator;
import ix.core.models.*;
import ix.core.search.*;
import ix.core.models.FieldedQueryFacet.MATCH_TYPE;
import ix.core.search.SearchOptions.DrillAndPath;
import ix.core.util.EntityUtils;
import ix.core.util.EntityUtils.EntityInfo;
import ix.core.util.EntityUtils.EntityWrapper;
import ix.core.util.EntityUtils.Key;
import ix.core.util.LogUtil;
import ix.core.utils.executor.ProcessListener;
import ix.utils.Util;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.core.KeywordAnalyzer;
import org.apache.lucene.analysis.miscellaneous.PerFieldAnalyzerWrapper;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.analysis.util.CharArraySet;
import org.apache.lucene.document.*;
import org.apache.lucene.facet.*;
import org.apache.lucene.facet.range.LongRange;
import org.apache.lucene.facet.range.LongRangeFacetCounts;
import org.apache.lucene.facet.taxonomy.FastTaxonomyFacetCounts;
import org.apache.lucene.facet.taxonomy.TaxonomyReader;
import org.apache.lucene.facet.taxonomy.directory.DirectoryTaxonomyReader;
import org.apache.lucene.facet.taxonomy.directory.DirectoryTaxonomyWriter;
import org.apache.lucene.index.*;
import org.apache.lucene.index.FieldInfo.IndexOptions;
import org.apache.lucene.queries.BooleanFilter;
import org.apache.lucene.queries.ChainedFilter;
import org.apache.lucene.queries.FilterClause;
import org.apache.lucene.queries.TermsFilter;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.queryparser.complexPhrase.ComplexPhraseQueryParser;
import org.apache.lucene.search.*;
import org.apache.lucene.search.BooleanClause.Occur;
import org.apache.lucene.search.suggest.DocumentDictionary;
import org.apache.lucene.search.suggest.analyzing.AnalyzingInfixSuggester;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.NIOFSDirectory;
import org.apache.lucene.store.NoLockFactory;
import org.apache.lucene.store.RAMDirectory;
import org.apache.lucene.util.BytesRef;
import org.apache.lucene.util.Version;
import org.springframework.beans.factory.annotation.Autowired;
import java.io.*;
import java.nio.file.Files;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.apache.lucene.document.Field.Store.NO;
import static org.apache.lucene.document.Field.Store.YES;

/**
 * Singleton class that responsible for all entity indexing
 */
@Slf4j
public class TextIndexer implements Closeable, ProcessListener {
	public static final String TERM_VEC_PREFIX = "F";

    public static final String IX_BASE_PACKAGE = "ix";


    private TextIndexerConfig textIndexerConfig;


//	public static final boolean INDEXING_ENABLED = ConfigHelper.getBoolean("ix.textindex.enabled",true);
//	private static final boolean USE_ANALYSIS =    ConfigHelper.getBoolean("ix.textindex.fieldsuggest",true);
//    private static final CachedSupplier<Boolean> SHOULD_LOG_INDEXING =    CachedSupplier.of(new Supplier<Boolean>() {
//        @Override
//        public Boolean get() {
//            boolean value= Play.application().configuration().getBoolean("ix.textindex.shouldLog", false);
//            return value;
//        }
//    });

    private static final String ANALYZER_FIELD = "M_FIELD";
	private static final String ANALYZER_MARKER_FIELD = "ANALYZER_MARKER";
	private static final String ANALYZER_VAL_PREFIX = "ANALYZER_";
	private static final int DEFAULT_ANALYZER_MATCH_FIELD_LIMIT = 25; // number of narrowing fields to show
	
	
	private static final char SORT_DESCENDING_CHAR = '$';
	private static final char SORT_ASCENDING_CHAR = '^';
	private static final int EXTRA_PADDING = 2;
	private static final String FULL_TEXT_FIELD = "text";
	private static final String SORT_PREFIX = "SORT_";
	protected static final String STOP_WORD = " THE_STOP";
	protected static final String START_WORD = "THE_START ";
	public static final String GIVEN_STOP_WORD = "$";
	public static final String GIVEN_START_WORD = "^";
	static final String ROOT = "root";
	static final String ENTITY_PREFIX = "entity";	
	private static final String SPACE_WORD = "XSPACEX";	

    private static final Pattern COMPLEX_QUERY_REGEX = Pattern.compile("_.*:");

    private List<IndexListener> listeners = new ArrayList<>();

	private Set<String> alreadySeenDuringReindexingMode;

	@Autowired
	GsrsCache gsrscache;

    /**
     * DO NOT CALL UNLESS YOU KNOW WHAT YOU ARE DOING.
     * This is exposed for dependency injection from
     * another module.
     * @param indexServiceCreator Function that creates the indexService
     *                            from the directory to use.
     */
    public void setIndexerService(ThrowableFunction<File, IndexerService, IOException> indexServiceCreator) {
        this.indexerService = indexerService;
    }

    public void addListender(IndexListener l){
	    listeners.add(Objects.requireNonNull(l));
    }

    public void removeListener(IndexListener l){
        listeners.remove(Objects.requireNonNull(l));
    }

    //Listener notifications. Listeners get notified on
    //1. adding a document
    //2. deleting (based on a query, not a document)
    //3. when wiping the index (delete all)
    private void notifyListenersAddDocument(Document d){
        listeners.forEach(l -> l.addDocument(d));
    }
    private void notifyListenersDeleteDocuments(Query q){
        listeners.forEach(l-> l.deleteDocuments(q));
    }
    private void notifyListenersRemoveAll(){
        listeners.forEach(IndexListener::removeAll);
    }

    
	public static String FULL_TEXT_FIELD(){
		return FULL_TEXT_FIELD;
	}

	

	private void deleteFileIfExists(File f){
        if(f.exists()){
            f.delete();
        }
    }
	
	/**
	 * well known fields
	 */
	public static final String FIELD_KIND = "__kind";
	public static final String FIELD_ID = "id";

	/**
	 * these default parameters should be configurable!
	 */
	public static final int CACHE_TIMEOUT = 60 * 60 * 24; // 24 hours

	/**
	 * Make sure to properly update the code when upgrading version
	 */
	
	//Version 4.10.0
	static final Version LUCENE_VERSION = Version.LATEST;
	static final String FACETS_CONFIG_FILE = "facet_conf.json";
	static final String SUGGEST_CONFIG_FILE = "suggest_conf.json";
	static final String SORTER_CONFIG_FILE = "sorter_conf.json";
	public static final String DIM_CLASS = "ix.Class";

	static final ThreadLocal<DateFormat> YEAR_DATE_FORMAT = ThreadLocal.withInitial(()->new SimpleDateFormat("yyyy"));
	

	private static final Pattern SUGGESTION_WHITESPACE_PATTERN = Pattern.compile("[\\s/]");

	private static CachedSupplier<AtomicBoolean> ALREADY_INITIALIZED = CachedSupplier.of(()->new AtomicBoolean(false));

	
	private static class TermVectorField extends org.apache.lucene.document.Field {
        static final FieldType TermVectorFieldType = new FieldType();
        static {
            TermVectorFieldType.setIndexed(true);
            TermVectorFieldType.setTokenized(false);
            TermVectorFieldType.setStoreTermVectors(true);
            TermVectorFieldType.setStoreTermVectorPositions(false);
            TermVectorFieldType.freeze();
        }
        
        public TermVectorField (String field, String value) {
            super (field, value, TermVectorFieldType);
        }
    }

    public static class TermVectors implements Serializable {
        static private final long serialVersionUID = 0x192464a5d08ea528l;

        private Class kind;
        private String field;
        private int numDocs;
        private List<TermList> docs = new ArrayList<TermList>();
        private Map<String, DocumentSet> terms = new TreeMap<String,DocumentSet>();
        private Map<String, String> filters = new TreeMap<String, String>();

        TermVectors (Class kind, String field) {
            this.kind = kind;
            this.field = field;
        }

        public Class getKind () { return kind; }
        public String getField () { return field.substring(TERM_VEC_PREFIX.length()); }

        public Map<String, String> getFilters () { return filters; }
        public Map<String, DocumentSet> getTerms () { return terms; }
        public List<TermList> getDocs () { return docs; }
        public int getNumDocs () { return numDocs; }
        public int getNumDocsWithTerms () { return docs.size(); }
        public int getNumTerms () { return terms.size(); }
        public Integer getTermCount (String term) {
            DocumentSet map = terms.get(term);
            Integer count = null;
            if (map != null) {
                count = map.getNDocs();
            }
            return count;
        }

        public Predicate<Tuple<String, Integer>> getPredicate(Query q){
            String find = "";
            if(q instanceof PhraseQuery){
                find=Stream.of(((PhraseQuery)q).getTerms())
                        .map(t->t.text())
                        .collect(Collectors.joining(" "));
            }else if(q instanceof TermQuery){
                find=Stream.of(((TermQuery)q).getTerm())
                        .map(t->t.text())
                        .collect(Collectors.joining(" "));
            }else if(q instanceof TermRangeQuery){
                TermRangeQuery trq= (TermRangeQuery)q;
                Integer max = Integer.parseInt(trq.getUpperTerm().utf8ToString())+ ((trq.includesUpper())?1:0);
                Integer min = Integer.parseInt(trq.getLowerTerm().utf8ToString())- ((trq.includesLower())?1:0);;

                return (t)->{
                    return (t.v()< max) && (t.v()>min);
                  };

            }else if(q instanceof NumericRangeQuery){
                NumericRangeQuery nq = (NumericRangeQuery)q;
                nq.includesMax();
                nq.includesMin();

                Integer max=((NumericRangeQuery)q)
                                .getMax()
                                .intValue() + ((nq.includesMax())?1:0);
                Integer min=((NumericRangeQuery)q)
                                .getMin()
                                .intValue() - ((nq.includesMin())?1:0);

                return (t)->{
                  return (t.v()< max) && (t.v()>min);
                };
            }else if(q instanceof PrefixQuery){
                PrefixQuery pq = (PrefixQuery)q;
                find=pq.getPrefix().text();
                String finalField = find.toLowerCase();
                return (t)->{
                    return Stream
                            .of(t.k().toLowerCase().split(" "))
                            .filter(f->f.startsWith(finalField))
                            .findAny().isPresent();
                  };
            }else if(q instanceof BooleanQuery){
                BooleanQuery bq = (BooleanQuery)q;
                Set<Term> terms = new HashSet<Term>();
                bq.extractTerms(terms);

                List<String> findterms=terms.stream().map(t->t.text().toLowerCase()).collect(Collectors.toList());

                return (t)->{
                    return findterms.stream().allMatch(s->t.k().toLowerCase().contains(s));
                };

            }else{
                throw new IllegalStateException(q.getClass() + " not supported " + ":" + q.toString());

            }
            String finalField = find.toLowerCase();
            return (t)->{
              return t.k().toLowerCase().contains(finalField);
            };
        }

        public FacetMeta getFacet(int top, int skip, String filter, String uri) throws ParseException {
            FacetImpl fac = new FacetImpl(getField(), null);

            Predicate<Tuple<String,Integer>> filt=(t)->true;

            if(filter!=null && !filter.equals("")){
                QueryParser parser = new IxQueryParser("label");
                filt = getPredicate(parser.parse(filter));
            }


            terms.entrySet()
                .stream()
                .parallel()
                .map(es->Tuple.of(es.getKey(), es.getValue().getNDocs()))
                .filter(filt)
                .map(t->new FV(fac,t.k(),t.v()))
                .collect(StreamUtil.maxElements(top+skip, FacetImpl.Comparators.COUNT_SORTER_DESC))
                .skip(skip)
                .limit(top)
                .forEach(fv->{
                    fac.add(fv);
                });

            return new FacetMeta.Builder()
					            .facets(fac)
					            .ffilter(filter)
					            .fdim(top)
					            .fskip(skip)
					            .ftotal(terms.size())
					            .uri(uri)
					            .build();
        }

    }
    private static class DocumentSet implements Serializable{
        private Set s;
        public DocumentSet(Set set){
            this.s=set;
        }
        public Set getDocs(){
            return this.s;
        }
        public int getNDocs(){
            return this.s.size();
        }
        public static DocumentSet of(Set s){
            return new DocumentSet(s);
        }
    }

    private static class TermList implements Comparable<TermList>, Serializable{
        private String id;
        private List s;
        public TermList(String id,List list){
            this.id=id;
            this.s=list;
        }

        public String getDoc(){
            return id;
        }

        public int getNTerms(){
            return this.s.size();
        }

        public List getTerms(){
            return this.s;
        }
        public static TermList of(String id, List s){
            return new TermList(id,s);
        }

        @Override
        public int compareTo(TermList o) {
            return o.getNTerms()-this.getNTerms();
        }
    }
    private static <T>  T getFieldValue(Object obj, String fieldName){
        try {
            java.lang.reflect.Field f = obj.getClass().getDeclaredField(fieldName);
            f.setAccessible(true);
            return (T) f.get(obj);
        }catch(NoSuchFieldException  | IllegalAccessException e){
            throw new RuntimeException(e);
        }
    }

    static class TermVectorsCollector<T> extends Collector {
        private int docBase;
        private IndexReader reader;
        private EntityInfo<T> entityMeta;
        private TermVectors tvec;
        private Map<String, Set<Object>> counts;
        private final Set<String> fieldSet;

        private TermVectorsCollector (Class<T> kind, String originalField, IndexSearcher searcher, Filter extrafilter, Query q)
            throws IOException {
            String adaptedField = TERM_VEC_PREFIX + originalField;

            tvec = new TermVectors (kind, adaptedField);
            counts = new TreeMap<String, Set<Object>>();

            entityMeta = EntityUtils.getEntityInfoFor(kind);

            fieldSet = entityMeta.getTypeAndSubTypes()
                      .stream()
                      .map(em->em.getInternalIdField())
                      .collect(Collectors.toSet());
            fieldSet.add(FIELD_KIND);


            this.reader = searcher.getIndexReader();

            Filter filter = filterForKinds(kind);

            if(q==null){
                q = new MatchAllDocsQuery();
            }

            if(extrafilter!=null){
                filter=new ChainedFilter(new Filter[]{filter,extrafilter}, ChainedFilter.AND);
            }

            searcher.search(q, filter, this);

            Collections.sort(tvec.docs);

            tvec.terms= counts.entrySet()
                    .stream()
                    .map(Tuple::of)
                    .map(Tuple.vmap(DocumentSet::of))
                    .collect(Tuple.toMap());
            counts = null;

        }


        public void setScorer (Scorer scorer) {}

        public boolean acceptsDocsOutOfOrder () { return true; }

        public void collect (int doc) {
            int docId = docBase + doc;
            try {

                //TODO: It IS possible to get all fields
                //available here. That could be useful
                //for having a "facets" resource in the
                //context of a collection

//                StreamUtil
//                        .forIterable(reader.getTermVectors(docId))
//                        .forEach(c->{
//                            System.out.println("GOT Facet:" + c);
//                        });


                Terms docterms = reader.getTermVector(docId, tvec.field);
                if (docterms != null) {
                    Document d = reader.document(docId, fieldSet);
                    String kind=d.get(FIELD_KIND).toString();

                    EntityInfo einfo= EntityUtils.getEntityInfoFor(kind);
                    String idstring = d.get(einfo.getInternalIdField()).toString();
                    Object nativeID= einfo.formatIdToNative(idstring.toString());


                    List<String> terms = StreamUtil
                          .from(docterms.iterator(null)) //Not sure what termsEnum is here
                          .streamNullable(en->en.next())
                          .map(t->t.utf8ToString())
                          .peek(term->{
                                    counts.computeIfAbsent(term, t->new HashSet<>())
                                          .add(nativeID);
                           })
                          .collect(Collectors.toList());

                    tvec.docs.add(TermList.of(idstring, terms));
                }else {
                    //log.debug("No term vector for field \""+field+"\"!");
                }
                ++tvec.numDocs;
            }catch (Exception ex) {
                ex.printStackTrace();
            }
        }

        public void setNextReader (AtomicReaderContext ctx) {
            docBase = ctx.docBase;
        }

        public TermVectors termVectors () { return tvec; }

        public static <T> TermVectorsCollector<T> make(Class<T> kind, String originalField, IndexSearcher searcher, Filter filter, Query q) throws IOException{
            return new TermVectorsCollector<T>(kind,originalField, searcher, filter,q);
        }
    }


    public static class FacetImpl implements Facet {
		String name;
		private List<FV> values = new ArrayList<FV>();
		private Set<String> selectedLabel=new HashSet<>();
		private SearchResult sr;

		public boolean enhanced=true;

		//TODO: This is a bad way to do this,
		//need to fix
		private String prefix="";

		private CachedSupplier<List<FV>> selectedFVfetch =
		        CachedSupplier.of(()->{
		           return this.values.stream()
		               .filter(fv->selectedLabel.contains(fv.getLabel()))
		               .collect(Collectors.toList());
		        });


		/**
		 * Set the labeled facet(s) which were intentionally
		 * selected.
		 *
		 * @param label
		 */
		@Override
        public void setSelectedLabel(String... label){
			this.setSelectedLabels(Arrays.stream(label).collect(Collectors.toList()));
		}

		@Override
        public void setPrefix(String prefix){
			this.prefix=prefix;
		}

		@Override
        public String getPrefix(){
			return this.prefix;
		}


		@Override
        @JsonInclude(Include.NON_EMPTY)
		public Set<String> getSelectedLabels(){
			return this.selectedLabel;
		}

		@Override
        @JsonIgnore
		public List<FV> getSelectedFV(){
		    return this.selectedFVfetch.get();
		}

		@JsonProperty("_self")
		public String getSelfUri() throws Exception {
		    if(sr!=null){
		        return sr.getFacetURI(name);
		    }else{
		        return null;
		    }
		}

		@Override
        @JsonIgnore
		public Set<String> getMissingSelections(){
		    Set<String> containedSet=this.getSelectedFV().stream()
		        .map(FV::getLabel)
		        .collect(Collectors.toSet());

		    Set<String> totalSet = new HashSet<>(this.selectedLabel);
		    totalSet.removeAll(containedSet);
		    return totalSet;
		}


		public FacetImpl(String name, SearchResult sr) {
			this.name = name;
			this.sr=sr;
		}

		@Override
        public String getName() {
			return name;
		}

		@Override
        public List<FV> getValues() {
			return values;
		}


		/**
		 * Get the number of {@link FV} values currently present
		 * in this facet. This is equivalent to:
		 * <pre>
		 * <code>
		 *    return this.getValues().size();
		 * </code>
		 * </pre>
		 * @return
		 */
		@Override
        public int size() {
			return values.size();
		}

		@Override
        public FV getValue(int index) {
			return values.get(index);
		}

		@Override
        public String getLabel(int index) {
			return values.get(index).getLabel();
		}

		@Override
        public Integer getCount(int index) {
			return values.get(index).getCount();
		}

		@Override
        public Integer getCount(String label) {
			for (FV fv : values)
				if (fv.getLabel().equalsIgnoreCase(label))
					return fv.getCount();
			return null;
		}

		@Override
        public void sort() {
			sortCounts(true);
		}

		@Override
        public Facet filter(FacetFilter filter) {
			FacetImpl filtered = new FacetImpl(name,sr);
			for (FV fv : values)
				if (filter.accepted(fv))
					filtered.values.add(fv);
			return filtered;
		}

		@Override
        public void sortLabels(final boolean desc) {
			Collections.sort(values, new Comparator<FV>() {
				public int compare(FV v1, FV v2) {
					return desc ? v2.getLabel() .compareTo(v1.getLabel()) : v1.getLabel().compareTo(v2.getLabel());
				}
			});
		}

		@Override
        public void sortCounts(final boolean desc) {
			Collections.sort(values, (v1,v2)->{
					int d = desc ? (v2.getCount() - v1.getCount()) : (v1.getCount() - v2.getCount());
					if (d == 0)
						d = v1.getLabel().compareTo(v2.getLabel());
					return d;
			});
		}

        @Override
        public Map<String,Integer> toCountMap() {
		    Map<String, Integer> map = new LinkedHashMap<>();
            for(FV fv : values){
                map.put(fv.getLabel(), fv.getCount());
            }
            return map;
        }



        public static enum Comparators implements Comparator<FV>{
            LABEL_SORTER_ASC{
                @Override
                public int compare(FV v1, FV v2) {
                    return v1.getLabel().compareTo(v2.getLabel());
                }
            },
            COUNT_SORTER_ASC{
                @Override
                public int compare(FV v1, FV v2) {
                    int d = (v1.getCount() - v2.getCount());
                    if (d == 0)
                        d = -v1.getLabel().compareTo(v2.getLabel());
                    return d;
                }
            },
            LABEL_SORTER_DESC{
                @Override
                public int compare(FV v1, FV v2) {
                    return -LABEL_SORTER_ASC.compare(v1, v2);
                }
            },
            COUNT_SORTER_DESC{
                @Override
                public int compare(FV v1, FV v2) {
                    return -COUNT_SORTER_ASC.compare(v1, v2);
                }
            };
		}

		@Override
        @JsonIgnore
		public ArrayList<String> getLabelString() {
			ArrayList<String> strings = new ArrayList<String>();
			for (int i = 0; i < values.size(); i++) {
				String label = values.get(i).getLabel();
				strings.add(label);
			}
			return strings;
		}

		@Override
        @JsonIgnore
		public ArrayList<Integer> getLabelCount() {
			ArrayList<Integer> counts = new ArrayList<Integer>();
			for (int i = 0; i < values.size(); i++) {
				int count = values.get(i).getCount();
				counts.add(count);
			}
			return counts;
		}

		/**
		 * Creates and adds a {@link FV} to the list of values.
		 *
		 * @param label
		 * @param count
		 * @return The {@link FV} that was added
		 */
		@Override
        public FV add(String label, Integer count){
			FV val=new FV(this, label, count);
			add(val);
			return val;
		}

		private void add(FV fv){
			this.values.add(fv);
			this.selectedFVfetch.resetCache();
		}

        @Override
        public void setSelectedLabels(List<String> collect) {
            this.selectedLabel.clear();
            this.selectedLabel.addAll(collect);
            this.selectedFVfetch.resetCache();
        }


	}

	class SuggestLookup implements Closeable {
		String name;
		File dir;
		AtomicBoolean dirty = new AtomicBoolean(false);
		CachedSupplier.CachedThrowingSupplier<ExactMatchSuggesterDecorator> lookup = CachedSupplier.ofThrowing(()->{
			boolean isNew = false;
			if (!dir.exists()) {
				dir.mkdirs();
				isNew = true;
			} else if (!dir.isDirectory())
				throw new IllegalArgumentException("Not a directory: " + dir);


			AnalyzingInfixSuggester suggester = new AnalyzingInfixSuggester(LUCENE_VERSION,
					new NIOFSDirectory(dir, NoLockFactory.getNoLockFactory()), indexerService.getIndexAnalyzer());


			ExactMatchSuggesterDecorator lookupt = new ExactMatchSuggesterDecorator(suggester,()-> TextIndexer.getFieldValue(suggester, "searcherMgr"));

			// If there's an error getting the index count, it probably wasn't
			// saved properly. Treat it as new if an error is thrown.
			if (!isNew) {
				try {
					lookupt.getCount();
				} catch (Exception e) {
					isNew = true;
					log.warn("Error building lookup " + dir.getName() + " will reinitialize");
				}
			}

			if (isNew) {
				log.debug("Initializing lookup " + dir.getName());
				build(lookupt);
			} else {
				log.debug(lookupt.getCount() + " entries loaded for " + dir.getName());
			}
			return lookupt;
		});

		long lastRefresh;

		ConcurrentHashMap<String, Addition> additions = new ConcurrentHashMap<String, Addition>();

		class Addition {
			String text;
			AtomicLong weight;

			public Addition(String text, long weight) {
				this.text = text;
				this.weight = new AtomicLong(weight);
			}
			public void incrementWeight() {
				weight.incrementAndGet();
			}

			public void addToWeight(long value) {
				weight.getAndAdd(value);

			}
		}



		/**
		 * Not an ideal mechanism for flushing, but lucene does not provide
		 * a way to do this short of closing/opening in the version we use.
		 */
		private synchronized void flush() throws IOException{
			this.close();
			lookup.resetCache();
		}

		private SuggestLookup(File dir) throws IOException {
			this.dir = dir;
			this.name = dir.getName();
			//store for cache
			Optional<Throwable> ot=lookup.getThrown();
			if(ot.isPresent()){
				throw new IOException(ot.get());
			}
		}

		private SuggestLookup(String name) throws IOException {
			this(new File(suggestDir, name));
		}


		public void add(String text) throws IOException {
			addSuggest(text,1);
		}

		public void addSuggest(String text, int weight) throws IOException {
			Addition add = additions.computeIfAbsent(text, t -> new Addition(t, 0));
			add.addToWeight(weight);
			incr();
		}

		private void incr() {
			dirty.compareAndSet(false, true);
		}

		public void refreshIfDirty() {
			if (dirty.get()) {
				try {
					refresh();
				} catch (IOException ex) {
					ex.printStackTrace();
					log.trace("Can't refresh suggest index!", ex);
				}
			}
		}

		private synchronized void refresh() throws IOException {
			Iterator<Addition> additionIterator = additions.values().iterator();
			ExactMatchSuggesterDecorator emd = lookup.get().get();

			while (additionIterator.hasNext()) {
				Addition add = additionIterator.next();
				BytesRef ref = new BytesRef(add.text);
				add.addToWeight(emd.getWeightFor(ref));
				//lookup.
				((AnalyzingInfixSuggester)emd.getDelegate()).update(ref, null, add.weight.get(), ref);
				additionIterator.remove();
			}

			long start = System.currentTimeMillis();
			((AnalyzingInfixSuggester)emd.getDelegate()).refresh();
			lastRefresh = System.currentTimeMillis();
			log.debug(emd.getClass().getName() + " refreshs " + emd.getCount() + " entries in "
					+ String.format("%1$.2fs", 1e-3 * (lastRefresh - start)));
			dirty.set(false);
			flush();
		}

		@Override
		public void close() throws IOException {
			refreshIfDirty();
			//This needs to be run for it to persist. Weird.
			if(lookup.hasRun()){
				lookup.get().get().close();
			}
		}

		long build(ExactMatchSuggesterDecorator lookup) throws IOException {
			try(IndexReader reader = indexerService.createIndexReader()){

				// now weight field
				long start = System.currentTimeMillis();
				lookup.build(new DocumentDictionary(reader, name, null));
				long count = lookup.getCount();
				log.debug(lookup.getClass().getName() + " builds " + count + " entries in "
						+ String.format("%1$.2fs", 1e-3 * (System.currentTimeMillis() - start)));
				return count;
			}
		}

		List<SuggestResult> suggest(CharSequence key, int max) throws IOException {
			refreshIfDirty();
			return lookup.get().get().lookup(key, null, false, max).stream()
					.map(r -> new SuggestResult(r.payload.utf8ToString(), r.key, r.value))
					.collect(Collectors.toList());
		}
	}

	class FlushDaemon implements Runnable {

		private ReentrantLock latch = new ReentrantLock();
		FlushDaemon() {
		}

		protected void lockFlush(){
			latch.lock();
		}

		protected void unLockFlush(){
			latch.unlock();
		}

		public void run() {

			if(!latch.tryLock()){
				//someone else has the lock
				//we won't wait the schedule deamon
				//will re-run us soon anyway
				return;
			}


			try {
				// Don't execute if already shutdown
				if(isShutDown || isReindexing.get()){
					return;
				}
				execute();
			}catch(Throwable t) {
//			    t.printStackTrace();
            }finally {

				latch.unlock();
			}
		}

		/**
		 * Execute the flush, with debugging statistics, without looking at the
		 * shutdown state
		 */
		public void execute() {
		    //TODO katzelda October 2020: don't use stopwatch
//			StopWatch.timeElapsed(this::flush);
            flush();
		}

		private void flush() {
			File configFile = getFacetsConfigFile();
			if (TextIndexer.this.hasBeenModifiedSince(configFile.lastModified())) {
				log.debug(
						Thread.currentThread() + ": " + getClass().getName() + " writing FacetsConfig " + new Date());
				saveFacetsConfig(configFile, facetsConfig);
			}

			File sortFile = getSorterConfigFile();
			if (TextIndexer.this.hasBeenModifiedSince(sortFile.lastModified())) {
				saveSorters(sortFile, sorters);
			}
           
			if ( indexerService.flushChangesIfNeeded()) {
				log.debug("Committing index changes...");
				try {
					taxonWriter.commit();
				} catch (IOException ex) {
					ex.printStackTrace();
					try {
						taxonWriter.rollback();
					} catch (IOException exx) {
						exx.printStackTrace();
					}
				}

				for (SuggestLookup lookup : lookups.values()) {
                    lookup.refreshIfDirty();
                }
			}

		}
	}

	private File baseDir;
	private File suggestDir;
	private Directory taxonDir;

	private DirectoryTaxonomyWriter taxonWriter;


    private FacetsConfig facetsConfig;


    private ConcurrentMap<String, SuggestLookup> lookups;
	private ConcurrentMap<String, SortField.Type> sorters;

	private Striped<Lock> stripedLock = Striped.lazyWeakLock(200);

	private AtomicLong lastModified = new AtomicLong();

	private ExecutorService threadPool;
	private ScheduledExecutorService scheduler;

	private boolean isEmptyPool;

	// private Future[] fetchWorkers;
	// private BlockingQueue<SearchResultPayload> fetchQueue =
	// new LinkedBlockingQueue<SearchResultPayload>();

	static ConcurrentMap<File, TextIndexer> indexers;

	private File indexFileDir, facetFileDir;

	private boolean isShutDown = false;

	private AtomicBoolean isReindexing = new AtomicBoolean(false);

	private FlushDaemon flushDaemon;

	SearcherManager searchManager;

	private IndexerService indexerService;

	private IndexerServiceFactory indexerServiceFactory;

	private IndexValueMakerFactory indexValueMakerFactory;

	private Function<EntityWrapper, Boolean> deepKindFunction;



    public DirectoryTaxonomyWriter getTaxonWriter() {
        return taxonWriter;
    }

    private TextIndexer(IndexerServiceFactory indexerServiceFactory, IndexerService indexerService, TextIndexerConfig textIndexerConfig, IndexValueMakerFactory indexValueMakerFactory, Function<EntityWrapper, Boolean> deepKindFunction) {

        // empty instance should only be used for
		// facet subsearching so we only need to have
		// a single thread...
        this.indexerServiceFactory = indexerServiceFactory;
        this.indexValueMakerFactory = indexValueMakerFactory;
		threadPool = ForkJoinPool.commonPool();
		scheduler = null;
		isShutDown = false;
		isEmptyPool = true;
        this.textIndexerConfig = textIndexerConfig;
        this.indexerService = indexerService;
        this.deepKindFunction = deepKindFunction;
    }
    public TextIndexer(File dir, IndexerServiceFactory indexerServiceFactory, IndexerService indexerService, TextIndexerConfig textIndexerConfig, IndexValueMakerFactory indexValueMakerFactory,GsrsCache cache, Function<EntityWrapper, Boolean> deepKindFunction) throws IOException{
        this.gsrscache=cache;
        this.textIndexerConfig = textIndexerConfig;
        this.indexValueMakerFactory = indexValueMakerFactory;
	    this.baseDir = dir;
        threadPool = Executors.newFixedThreadPool(textIndexerConfig.getFetchWorkerCount());
        scheduler = Executors.newSingleThreadScheduledExecutor();
        isShutDown = false;
        isEmptyPool = false;

        this.deepKindFunction = deepKindFunction;
        this.indexerService = indexerService;
        this.indexerServiceFactory = indexerServiceFactory;
        
        initialSetup();

        flushDaemon = new FlushDaemon();
        // run daemon every 10s
        scheduler.scheduleWithFixedDelay(flushDaemon, 10, 35, TimeUnit.SECONDS);

    }
    
    private void initialSetup() throws IOException {

        searchManager = this.indexerService.createSearchManager();
        facetFileDir = new File(baseDir, "facet");
        Files.createDirectories(facetFileDir.toPath());
        taxonDir = new NIOFSDirectory(facetFileDir, NoLockFactory.getNoLockFactory());
        taxonWriter = new DirectoryTaxonomyWriter(taxonDir);
        facetsConfig = loadFacetsConfig(new File(baseDir, FACETS_CONFIG_FILE));
        if (facetsConfig == null) {
            int size = taxonWriter.getSize();
            if (size > 0) {
                log.warn("There are " + size + " dimensions in " + "taxonomy but no facet\nconfiguration found; "
                        + "facet searching might not work properly!");
            }
            facetsConfig = new FacetsConfig();
            facetsConfig.setMultiValued(DIM_CLASS, true);
            facetsConfig.setRequireDimCount(DIM_CLASS, true);
        }

        suggestDir = new File(baseDir, "suggest");
        Files.createDirectories(suggestDir.toPath());
        lookups = new ConcurrentHashMap<String, SuggestLookup>();
        File[] suggestFiles = suggestDir.listFiles();
        if(suggestFiles !=null) {
            for (File f : suggestFiles) {
                if (f.isDirectory()) {
                    try {
                        lookups.put(f.getName(), new SuggestLookup(f));
                    } catch (IOException ex) {
                        log.error("Unable to load lookup from " + f, ex);
                    }
                }
            }
        }

        log.info("## " + suggestDir + ": " + lookups.size() + " lookups loaded!");

        sorters = loadSorters(new File(baseDir, SORTER_CONFIG_FILE));
        log.info("## " + sorters.size() + " sort fields defined!");        
        
    }
    


	@FunctionalInterface
	public interface SearcherFunction<R> {
		R apply(IndexSearcher indexSearcher) throws Exception;
	}

	//This method is moved from SearchRequest since it belongs here since it uses indexer fields mostly

    public Query extractFullFacetQuery(String queryString, SearchOptions options, String facet) throws ParseException {
        return extractFullFacetQueryAndFilter(queryString, options,facet).k();
    }
    private Query extractFullQuery(String queryString, SearchOptions options) throws ParseException {
        Query query = extractLuceneQuery(queryString);
        if (options.getFacets().isEmpty()) {
            return query;
        }else{
            DrillDownQuery ddq = new DrillDownQuery(facetsConfig, query);
            options.getDrillDownsMap().values()
                    .stream()
                    .flatMap(t->t.stream())
                    .forEach(dp->{
                        ddq.add(dp.getDrill(), dp.getPaths());
                    });
            return ddq;
        }
    }
    /**
     * Extracts a lucene query from the contained query text,
     * using the provided {@link QueryParser}. This only
     * extracts a query from the text, and does not include
     * kind and subset information. If the query text is null,
     * this returns a {@link MatchAllDocsQuery}.
     * @param query the query String to parse into a Lucene {@link Query};
     *              can be null to mean match everything.
     * @return the {@link Query} for the given string.
     * @throws ParseException
     */
    private Query extractLuceneQuery(String query) throws ParseException {

        if (query == null) {
            return new MatchAllDocsQuery();
        } else {
            return getQueryParser().parse(query);
        }
    }

    private Tuple<Query, Filter> extractFullFacetQueryAndFilter(String queryString, SearchOptions options, String facet) throws ParseException {
        if(!options.isSideway() || options.getFacets().isEmpty()){
            return Tuple.of(extractFullQuery(queryString, options),null);
        }


        Query query = extractLuceneQuery(queryString);



        List<Filter> nonStandardFacets = new ArrayList<>();

        DrillDownQuery ddq = new DrillDownQuery(facetsConfig, query);
        options.getDrillDownsMap().values()
                .stream()
                .flatMap(t->t.stream())
                .filter(dp->!dp.getDrill().equals(facet))
                .filter(dp->{
                    if(dp.getDrill().startsWith("^")){
                        nonStandardFacets.add(new TermsFilter(new Term(TextIndexer.TERM_VEC_PREFIX + dp.getDrill().substring(1), dp.getPaths()[0])));
                        return false;
                    }else if(dp.getDrill().startsWith("!")){
                        BooleanFilter f = new BooleanFilter();
                        TermsFilter tf = new TermsFilter(new Term(TextIndexer.TERM_VEC_PREFIX + dp.getDrill().substring(1), dp.getPaths()[0]));
                        f.add(new FilterClause(tf, BooleanClause.Occur.MUST_NOT));
                        nonStandardFacets.add(f);
                        return false;
                    }
                    return true;
                })
                .forEach(dp->{
                    ddq.add(dp.getDrill(), dp.getPaths());
                });
        Filter filter = null;

        if(!nonStandardFacets.isEmpty()){
            filter = new ChainedFilter(nonStandardFacets.toArray(new Filter[0])
                    , ChainedFilter.AND);
        }
        return Tuple.of(ddq,filter);
    }

	public <R> R withSearcher(SearcherFunction<R> worker) throws Exception {
		searchManager.maybeRefresh();
		IndexSearcher searcher = searchManager.acquire();
		try {
			return worker.apply(searcher); //what happens if this starts using the
										   //searcher in another thread?
		} finally {
			searchManager.release(searcher);
		}
	}

	@SuppressWarnings("deprecation")
	static Analyzer createIndexAnalyzer() {
		Map<String, Analyzer> fields = new HashMap<String, Analyzer>();
		fields.put(FIELD_ID, new KeywordAnalyzer());
		fields.put(FIELD_KIND, new KeywordAnalyzer());
		//dkatzel 2017-08 no stop words
		return new PerFieldAnalyzerWrapper(new StandardAnalyzer(LUCENE_VERSION, CharArraySet.EMPTY_SET), fields);
	}

	/**
	 * Create a empty RAM instance. This is useful for searching/filtering of a
	 * subset of the documents stored.
	 */
	public TextIndexer createEmptyInstance() throws IOException {
	    TextIndexer indexer = new TextIndexer(indexerServiceFactory, indexerServiceFactory.createInMemory(),textIndexerConfig, indexValueMakerFactory, deepKindFunction );
		indexer.taxonDir = new RAMDirectory();
		return config(indexer);
	}

	protected TextIndexer config(TextIndexer indexer) throws IOException {


		indexer.searchManager = indexer.indexerService.createSearchManager();
		indexer.taxonWriter = new DirectoryTaxonomyWriter(indexer.taxonDir);
		indexer.facetsConfig = new FacetsConfig();

		//This should also be reset by the re-indexing trigger
		indexer.facetsConfig.getDimConfigs().forEach((dim,dconf)->{
			indexer.facetsConfig.setHierarchical(dim, dconf.hierarchical);
			indexer.facetsConfig.setMultiValued(dim, dconf.multiValued);
			indexer.facetsConfig.setRequireDimCount(dim, dconf.requireDimCount);
		});


		indexer.lookups = new ConcurrentHashMap<>();
		indexer.sorters = new ConcurrentHashMap<>();
		//TODO katzelda October 2020 : the Play version this empty factory method is only
        //called from another text indexer instance so it has the sorters already set...
        // do we need to copy it somew
		indexer.sorters.putAll(sorters);
		return indexer;
	}



	public List<? extends GsrsSuggestResult> suggest(String field, CharSequence key, int max) throws IOException {
		SuggestLookup lookup = lookups.get(field);
		if (lookup == null) {
			log.debug("Unknown suggest field \"" + field + "\"");
			return Collections.emptyList();
		}

		return lookup.suggest(key, max);
	}

	public Collection<String> getSuggestFields() {
		return Collections.unmodifiableCollection(lookups.keySet());
	}

	/**
	 * Returns the number of documents indexed here.
	 *
	 * Returns -1 if there is an issue opening the index.
	 * @return
	 */
	public int size() {
		try {
			return withSearcher(s -> s.getIndexReader().numDocs());
		} catch (Exception ex) {
			log.trace("Can't retrieve NumDocs", ex);
		}
		return -1;
	}



    public static class IxQueryParser extends ComplexPhraseQueryParser {

        private QueryParser oldQParser;

        private static final Pattern ROOT_CONTEXT_ADDER = Pattern
                .compile("(\\b(?!" + ROOT + "|" + ENTITY_PREFIX +")[^ :]*_[^ :]*[:])");
        
      
        private static final Pattern QUOTES_AROUND_WORD_REMOVER = Pattern
                .compile("\"([^\" .-]*)\"");

        public IxQueryParser(String def) {
            super(def, createIndexAnalyzer());
            oldQParser = new QueryParser(def, createIndexAnalyzer()) {
                @Override
                protected Query getRangeQuery(String field, String part1, String part2,
                        boolean startInclusive, boolean endInclusive)
                        throws ParseException {
                    Query q = super.getRangeQuery(field, part1, part2, startInclusive,
                            endInclusive);

                    return fixRangeQuery(q);
                }
                
            };
            // setDefaultOperator(QueryParser.AND_OPERATOR);
            this.setAllowLeadingWildcard(true);
            oldQParser.setAllowLeadingWildcard(true);
        }

        public IxQueryParser(String string, Analyzer indexAnalyzer) {
            super(string, indexAnalyzer);

            oldQParser = new QueryParser(string, indexAnalyzer) {
                @Override
                protected Query getRangeQuery(String field, String part1, String part2,
                        boolean startInclusive, boolean endInclusive)
                        throws ParseException {
                    Query q = super.getRangeQuery(field, part1, part2, startInclusive,
                            endInclusive);

                    return fixRangeQuery(q);
                }
                
            };
            // setDefaultOperator(QueryParser.AND_OPERATOR);
            //TP 08/14/2021 simplifying how this is done
            this.setAllowLeadingWildcard(true);
            oldQParser.setAllowLeadingWildcard(true);
        }

        @Override
        protected Query getRangeQuery(String field, String part1, String part2,
                boolean startInclusive, boolean endInclusive)
                throws ParseException {
            Query q = super.getRangeQuery(field, part1, part2, startInclusive,
                    endInclusive);

            return fixRangeQuery(q);
        }
		
		private Query fixRangeQuery(Query q){
		    // katzelda 4/14/2018
            // this is to get range queries to work with our datetimestamps
            // without having to use the lucene DateTools
            if (q instanceof TermRangeQuery) {
                TermRangeQuery trq = (TermRangeQuery) q;
                String lower = trq.getLowerTerm().utf8ToString();
                String higher = trq.getUpperTerm().utf8ToString();

                try {
                    double low = Double.parseDouble(lower);
                    double high = Double.parseDouble(higher);
                    q = NumericRangeQuery.newDoubleRange("D_" + trq.getField(),
                            low, high, trq.includesLower(),
                            trq.includesUpper());
                } catch (Exception e) {
                    log.warn("problem parsing numeric range", e);
                }
            }

            return q;
		}

        @Override
        public Query parse(String qtext) throws ParseException {
            if (qtext != null) {
                qtext = transformQueryForExactMatch(qtext);
            }
            // add ROOT prefix to all term queries (containing '_') where not
            // otherwise specified
            
            qtext = ROOT_CONTEXT_ADDER.matcher(qtext).replaceAll(ROOT + "_$1");
            qtext = QUOTES_AROUND_WORD_REMOVER.matcher(qtext).replaceAll("$1");
            
            // If there's an error parsing, it probably needs to have
            // quotes. Likely this happens from ":" chars

            Query q = null;
            try {
                if (qtext.contains("*") && !qtext.equals("*:*")) {
                    q = super.parse(qtext);
                } else {
                    q = oldQParser.parse(qtext);
                }

            } catch (Exception e) {
                // This is not a good way to deal with dangling quotes, but it
                // is A way to do it
                try {
                    q = super.parse("\"" + qtext + "\"");
                } catch (Exception e2) {
                    if (qtext.startsWith("\"")) {
                        q = super.parse(qtext + "\"");
                    } else {
                        q = super.parse("\"" + qtext);
                    }
                }
            }
            return fixRangeQuery(q);
        }
    }


	public SearchResult search(GsrsRepository gsrsRepository, String text, int size) throws IOException {
		return search(gsrsRepository, new SearchOptions(null, size, 0, 10), text);
	}
	public SearchResult search(GsrsRepository gsrsRepository, SearchOptions options, String text) throws IOException {
		return search(gsrsRepository, options, text, null);
	}
	public SearchResult search(GsrsRepository gsrsRepository,  SearchOptions options, String qtext, Collection<?> subset) throws IOException {
		SearchResult searchResult = new SearchResult(options, qtext);

		Supplier<Query> qs = ()->{
		    Query query=null;
    		if (qtext == null || qtext.equals("*:*")) {
    			query = new MatchAllDocsQuery();
    		} else {
    			try {
    				QueryParser parser = new IxQueryParser(FULL_TEXT_FIELD, indexerService.getIndexAnalyzer());     				
    				String processedQtext = preProcessQueryText(qtext);
    				query = parser.parse(processedQtext);
    			} catch (ParseException ex) {
    				log.warn("Can't parse query expression: " + qtext, ex);
    				throw new IllegalStateException(ex);
    			}
    		}
    		return query;
		};
		Supplier<Filter> fs = ()->{
			Filter f = null;
			if (subset != null) {
				List<Term> terms = getTerms(subset);
				if (!terms.isEmpty()){
					f = new TermsFilter(terms);
				}
				if(options.getOrder().isEmpty() ||
				   options.getOrder().stream().collect(Collectors.joining("_")).equals("default")){
					Stream<Key> ids = subset.stream()
											.map(o-> EntityWrapper.of(o).getKey());
					Comparator<Key> comp= Util.comparator(ids);

					searchResult.setRank(comp);
				}
			} 
			if (options.getKind() != null) {
			    if(f==null) {
			        f = createKindArrayFromOptions(options);
			    }else {
			        BooleanFilter bf = new BooleanFilter();
			        bf.add(f,Occur.MUST);
			        bf.add(createKindArrayFromOptions(options),Occur.MUST);
			        f=bf;
			    }
			} else{
			    //TODO: Unclear if this works as intended
			    if(f==null) {
			        f = new FieldCacheTermsFilter(ANALYZER_MARKER_FIELD, "false");
			    }else {
			        BooleanFilter bf = new BooleanFilter();
                    bf.add(f,Occur.MUST);
                    bf.add(new FieldCacheTermsFilter(ANALYZER_MARKER_FIELD, "false"),Occur.MUST);
                    f=bf;
			    }
			}
			return f;
		};
		Query q=qs.get();
		Filter f=fs.get();

		try{
		    search(gsrsRepository, searchResult, q, f);
		}catch(Exception e){
		    e.printStackTrace();
		    throw new IOException(e);
		}

		return searchResult;
	}	
	
	//replace special characters ComplexPhraseQueryParser does not like with space
	public static String preProcessQueryText(String qtext) {
		
		String processedQtext = qtext.trim();
		
		if( !processedQtext.contains("*") || !processedQtext.contains("\"") )
			return processedQtext;
		
		Pattern singlePhraseQueryNoFieldNamePattern = Pattern.compile("\"([^\"]*)\"");		
		Pattern phraseQueryWithFieldNamePattern = Pattern.compile("(([^\"\\:]*\\:)(\\s)*(\"[^\"]*\"))");		
			
		Matcher singlePhraseMatcher = singlePhraseQueryNoFieldNamePattern.matcher(processedQtext);
		if(singlePhraseMatcher.matches()) {			
			processedQtext = replaceTokenSplitCharsWithString(replaceSpecialCharsForExactMatch(processedQtext));					
		}else {
			StringBuilder qtextSB = new StringBuilder();
			Matcher multiPhraseMatcher = phraseQueryWithFieldNamePattern.matcher(processedQtext);			
		
			int endPos = 0;
			while(multiPhraseMatcher.find()) {				
				endPos = multiPhraseMatcher.end();	           
				qtextSB.append(multiPhraseMatcher.group(2));
				String currentString = multiPhraseMatcher.group(4);
				if(currentString.contains("*"))
					qtextSB.append(replaceTokenSplitCharsWithString(replaceSpecialCharsForExactMatch(currentString)));
				else
					qtextSB.append(currentString);
			}
			if(endPos>0) {
				qtextSB.append(processedQtext.substring(endPos));			
				processedQtext = qtextSB.toString();
			}
		}		
		return processedQtext;
	}
	
	private static FieldCacheTermsFilter filterForKinds(Class<?> cls){
	    EntityInfo einfo = EntityUtils.getEntityInfoFor(cls);
        return filterForKinds(einfo);
    }
	private static FieldCacheTermsFilter filterForKinds(EntityInfo<?> einfo){
	    String[] opts= einfo.getTypeAndSubTypes()
                            .stream()
                            .map(s->s.getName())
                            .collect(Collectors.toList())
                            .toArray(new String[0]);
	    return new FieldCacheTermsFilter(FIELD_KIND, opts);
	}



	private FieldCacheTermsFilter createKindArrayFromOptions(SearchOptions options) {
		return filterForKinds(options.getKindInfo());
	}

	public SearchResult filter(GsrsRepository gsrsRepository, Collection<?> subset) throws Exception {
		SearchOptions options = new SearchOptions(null, subset.size(), 0, subset.size() / 2);
		return filter(gsrsRepository, options, subset);
	}

	protected List<Term> getTerms(Collection<?> subset) {
		return subset.stream()
				.map(this::getTerm)
				.filter(t -> t != null)
				.collect(Collectors.toList());
	}

	protected List<Term> getTermsFromKeys(Collection<Key> subset) {
        return subset.stream()
                .map(this::getTerm)
                .filter(t -> t != null)
                .collect(Collectors.toList());
    }

	protected TermsFilter getTermsFilter(Collection<?> subset) {
		return new TermsFilter(getTerms(subset));
	}

	protected TermsFilter getTermKeysFilter(Collection<Key> keyset) {
        return new TermsFilter(getTermsFromKeys(keyset));
    }


	public SearchResult filter(GsrsRepository gsrsRepository, SearchOptions options, Collection<?> subset) throws Exception {
		return filter(gsrsRepository, options, getTermsFilter(subset));
	}

	public SearchResult range(GsrsRepository gsrsRepository, SearchOptions options, String field, Integer min, Integer max) throws Exception {
		Query query = NumericRangeQuery.newIntRange(field, min, max, true /* minInclusive? */, true/* maxInclusive? */);
		return search(gsrsRepository, new SearchResult(options), query, null);
	}

	protected SearchResult filter(GsrsRepository gsrsRepository, SearchOptions options, Filter filter) throws Exception {
		return search(gsrsRepository, new SearchResult(options), new MatchAllDocsQuery(), filter);
	}

	protected SearchResult search(GsrsRepository gsrsRepository, SearchResult searchResult, Query query, Filter filter) throws Exception {
		return withSearcher(searcher -> search(gsrsRepository, searcher, searchResult, query, filter));
	}

	public Map<String,List<Filter>> createAndRemoveRangeFiltersFromOptions(SearchOptions options) {
		Map<String, List<Filter>> filters = new HashMap<String,List<Filter>>();
		if(options !=null) {
            options.removeAndConsumeRangeFilters((f, r) -> {
                filters
                        .computeIfAbsent(f, k -> new ArrayList<Filter>())
                        .add(FieldCacheRangeFilter.newLongRange(f, r[0], r[1], true, false));
            });
        }
		return filters;
	}



	public Sort createSorterFromOptions(SearchOptions options) {
		Sort sorter = null;
		if (options !=null && !options.getOrder().isEmpty()) {
			List<SortField> fields = new ArrayList<SortField>();
			for (String f : options.getOrder()) {
				boolean rev = false;
				if (f.charAt(0) == SORT_ASCENDING_CHAR) {
					f = f.substring(1);
				} else if (f.charAt(0) == SORT_DESCENDING_CHAR) {
					f = f.substring(1);
					rev = true;
				}
				// Find the correct sorter field. The sorter fields
				// always have the SORT_PREFIX prefix, and should also have
				// a ROOT prefix for the full path. If the root prefix is
				// not
				// present, this will add it.

				SortField.Type type = sorters.get(TextIndexer.SORT_PREFIX + f);
				if (type == null) {
					type = sorters.get(TextIndexer.SORT_PREFIX + ROOT + "_" + f);
					f = TextIndexer.SORT_PREFIX + ROOT + "_" + f;
				} else {
					f = TextIndexer.SORT_PREFIX + f;
				}
				if (type != null) {
					SortField sf = new SortField(f, type, rev);
					log.debug("Sort field (rev=" + rev + "): " + sf);
					fields.add(sf);
				} else {
					log.warn("Unknown sort field: \"" + f + "\"");
				}
			}

			if (!fields.isEmpty()) {
				sorter = new Sort(fields.toArray(new SortField[0]));
			}

		}
		return sorter;
	}



	public static void collectBasicFacets(Facets facets, SearchResult sr) throws IOException{
		Map<String,List<DrillAndPath>> providedDrills = sr.getOptions().getEnhancedDrillDownsMap();

		int fdim=sr.getOptions().getFdim();
		if(fdim<=0)return;

		List<FacetResult> facetResults = facets.getAllDims(fdim);
//		if (DEBUG(1)) {
//			log.info("## Drilled " + (sr.getOptions().isSideway() ? "sideway" : "down") + " " + facetResults.size()+ " facets");
//		}

		//Convert FacetResult -> Facet, and add to
		//search result
		facetResults.stream()
			.filter(Objects::nonNull)
			.map(result -> {
				Facet fac = new FacetImpl(result.dim, sr);

				// make sure the facet value is returned
				// for selected value

				List<DrillAndPath> dp = providedDrills.get(result.dim);
				if (dp != null) {

				    List<String> selected=dp.stream()
				                        .map(l->l.asLabel())
				                        .collect(Collectors.toList());

				    dp.stream()
	                        .map(l->l.getPrefix())
	                        .filter(p->!"".equals(p))
	                        .sorted()
	                        .distinct()
	                        .findFirst()
	                        .ifPresent(pre->{
	                        	fac.setPrefix(pre);
	                        });


					fac.setSelectedLabels(selected);
				}

				for(LabelAndValue lv:result.labelValues){
				    fac.add(lv.label, lv.value.intValue());
				}

				fac.getMissingSelections().stream().forEach(l->{
				    try {
                        Number value = facets.getSpecificValue(result.dim, l);
                        if (value != null && value.intValue() >= 0) {
                            fac.add(l, value.intValue());
                        } else {
                            log.warn("Facet \"" + result.dim + "\" doesn't have any " + "value for label \""
                                    + l + "\"!");
                        }
                    } catch (Exception e) {
                       log.warn("error collecting facets", e);
                    }
				});

				fac.sort();
				return fac;
			})
			.forEach(f -> sr.addFacet(f));
	}


	// Abstracted interface to help with cleaning up workflow
	// Call search, then call getTopDocs and/or getFacets

	public static interface LuceneSearchProvider{
		public LuceneSearchProviderResult search(IndexSearcher searcher, TaxonomyReader taxon, Query q, FacetsCollector facetCollector) throws IOException;
	}
	public static interface LuceneSearchProviderResult{
		public TopDocs getTopDocs();
		public Facets getFacets();
	}
	public static class DefaultLuceneSearchProviderResult implements LuceneSearchProviderResult{
		private TopDocs hits=null;
		private Facets facets=null;
		public DefaultLuceneSearchProviderResult(TopDocs hits, Facets facets){
			this.hits=hits;
			this.facets=facets;
		}
		@Override
		public TopDocs getTopDocs() {
			return hits;
		}
		@Override
		public Facets getFacets() {
			return facets;
		}

	}


	public class BasicLuceneSearchProvider implements LuceneSearchProvider{
	    private Sort sorter;
	    private Filter filter;
	    private int max;
	    private boolean includeFacets = true;

//		public BasicLuceneSearchProvider(Sort sorter, Filter filter, int max){
//			this.sorter=sorter;
//			this.filter=filter;
//			this.max=max;
//		}
        public BasicLuceneSearchProvider(Sort sorter,Filter filter, int max, boolean includeFacets){
            this.sorter=sorter;
            this.filter=filter;
            this.max=max;
            this.includeFacets=includeFacets;
        }

		@Override
		public DefaultLuceneSearchProviderResult search(IndexSearcher searcher, TaxonomyReader taxon, Query query, FacetsCollector facetCollector) throws IOException {
			TopDocs hits=null;
			Facets facets=null;
			//FacetsCollector.
			//with sorter
			if (sorter != null) {
			    hits = (FacetsCollector.search(searcher, query, filter, max, sorter, facetCollector));
			//without sorter
			}else {
			    hits = (FacetsCollector.search(searcher, query, filter, max, facetCollector));
			}
			if(includeFacets) {
			    facets = new FastTaxonomyFacetCounts(taxon, facetsConfig, facetCollector);
			}
			return new DefaultLuceneSearchProviderResult(hits,facets);
		}

	}
	public class DrillSidewaysLuceneSearchProvider implements LuceneSearchProvider{
		private TopDocs hits=null;
		private Facets facets=null;
		private Sort sorter;
		private Filter filter;
		private SearchOptions options;

		public DrillSidewaysLuceneSearchProvider(Sort sorter, Filter filter, SearchOptions options){
			this.sorter=sorter;
			this.filter=filter;
			this.options=options;
		}

		@Override
		public LuceneSearchProviderResult search(IndexSearcher searcher, TaxonomyReader taxon, Query ddq1, FacetsCollector facetCollector) throws IOException {
			if(!(ddq1 instanceof DrillDownQuery)){
				throw new IllegalStateException("Query must be drill down query");
			}
			DrillDownQuery ddq = (DrillDownQuery)ddq1;
			DrillSideways sideway = new DrillSideways(searcher, facetsConfig, taxon);


			DrillSideways.DrillSidewaysResult swResult = sideway.search(ddq, filter, null, options.max(),
					sorter, false, false);
			


			/*
			 * TODO: is this the only way to collect the counts for
			 * range/dynamic facets?
			 *
			 */
			if (options.getIncludeFacets() && !options.getLongRangeFacets().isEmpty()){
				FacetsCollector.search(searcher, ddq, filter, options.max(), facetCollector);
			}

			if(options.getIncludeFacets()) {
			    facets = swResult.facets;
			}
			hits = swResult.hits;
			return new DefaultLuceneSearchProviderResult(hits,facets);
		}

	}

	// This is the most important method, everything goes here
	protected SearchResult search(GsrsRepository gsrsRepository,  IndexSearcher searcher, SearchResult searchResult, Query query, Filter filter)
			throws IOException {
		final TopDocs hits;

		try (TaxonomyReader taxon = new DirectoryTaxonomyReader(taxonWriter)) {
		    hits=firstPassLuceneSearch(searcher,taxon,searchResult,filter, query, gsrsRepository);
		}

//		if (DEBUG(1)) {
//			log.debug(
//					"## Query executes in "
//							+ String.format("%1$.3fs", (TimeUtil.getCurrentTimeMillis() - start) * 1e-3)
//							+ "..."
//							+ hits.totalHits
//							+ " hit(s) found!");
//		}

		try {
			LuceneSearchResultPopulator payload = new LuceneSearchResultPopulator(gsrsRepository, searchResult, hits, searcher);
            //get everything, forever
            //hard-coded for now
            //katzelda Jan 2021 : fetching is now very fast so we can get everything always
            try {
                payload.fetch();
            } finally {
                searchResult.done();
            }
		} catch (Exception ex) {
		    searchResult.done();
			ex.printStackTrace();
			log.trace("Can't queue fetch results!", ex);
		}

		return searchResult;
	}
	
	/**
     * Performs a basic Lucene query using the provided Filter and Query, and any other
     * refining information from the SearchResult options. Facet results are placed in
     * the provided SearchResult, and the TopDocs hits from the Lucene search are returned.
     *
     * @param searcher
     * @param taxon
     * @param searchResult
     * @param ifilter
     * @param query
     * @return
     * @throws IOException
     */
    public TopDocs firstPassLuceneSearch(IndexSearcher searcher, TaxonomyReader taxon, SearchResult searchResult, Filter ifilter, Query query
            
            ) throws IOException{
        return firstPassLuceneSearch(searcher, taxon, searchResult,ifilter,query, null);
    }

	/**
	 * Performs a basic Lucene query using the provided Filter and Query, and any other
	 * refining information from the SearchResult options. Facet results are placed in
	 * the provided SearchResult, and the TopDocs hits from the Lucene search are returned.
	 *
	 * @param searcher
	 * @param taxon
	 * @param searchResult
	 * @param ifilter
	 * @param query
	 * @return
	 * @throws IOException
	 */
	public TopDocs firstPassLuceneSearch(IndexSearcher searcher, TaxonomyReader taxon, SearchResult searchResult, Filter ifilter, Query query,
	        
	        GsrsRepository gsrsRepository) throws IOException{
		final TopDocs hits;
		SearchOptions options = searchResult.getOptions();
		FacetsCollector facetCollector = new FacetsCollector();
		LuceneSearchProvider lsp;

		Filter filter = ifilter;

		// You may wonder why some of these options parsing
		// elements are directly accessible from SearchOptions
		// methods, while others have external parsing functions,
		// while seeming to have the same object dependencies...
		//
		// That's really just to avoid having Lucene-specific
		// code in the SearchOptions class. If it's Lucene-specific,
		// then the parser is here. If it's a more general function,
		// then it's put into SearchOptions directly.
		//
		// This may change in the future

		Sort sorter = createSorterFromOptions(options);


		List<Filter> filtersFromOptions = createAndRemoveRangeFiltersFromOptions(options)
				.values()
				.stream()
				.map(val->new ChainedFilter(val.toArray(new Filter[0]), ChainedFilter.OR))
				.collect(Collectors.toList());

		options.getTermFilters()
		    .stream()
			.map(k-> new TermsFilter(new Term(k.getField(), k.getTerm())))
			.forEach(f->filtersFromOptions.add(f));


		Query qactual = query;

		//Collect the range filters into one giant filter.
		//Specifically, each element of a group of filters is set
		//to be joined by an "OR", while each group is joined
		//by "AND" to the other groups
		if(!filtersFromOptions.isEmpty()){
			filtersFromOptions.add(ifilter);
			filter = new ChainedFilter(filtersFromOptions.stream()
										.collect(Collectors.toList())
										.toArray(new Filter[0]), ChainedFilter.AND);
			filtersFromOptions.remove(filtersFromOptions.size()-1);
		}

		//no specified facets (normal search)
		if (options.getFacets().isEmpty()) {
			lsp = new BasicLuceneSearchProvider(sorter, filter, options.max(), options.getIncludeFacets());
		} else {
			DrillDownQuery ddq = new DrillDownQuery(facetsConfig, query);
			List<Filter> nonStandardFacets = new ArrayList<Filter>();

			options.getDrillDownsMapExcludingRanges()
			    .entrySet()
			    .stream()
			    .flatMap(e->e.getValue().stream())
			    .filter(dp->{
			    	if(dp.getDrill().startsWith("^")){
			    		nonStandardFacets.add(new TermsFilter(new Term(TextIndexer.TERM_VEC_PREFIX + dp.getDrill().substring(1), dp.getPaths()[0])));
			    		return false;
			    	}else if(dp.getDrill().startsWith("!")){
			    		BooleanFilter f = new BooleanFilter();
			    		TermsFilter tf = new TermsFilter(new Term(TextIndexer.TERM_VEC_PREFIX + dp.getDrill().substring(1), dp.getPaths()[0]));
			    		f.add(new FilterClause(tf, BooleanClause.Occur.MUST_NOT));
			    		nonStandardFacets.add(f);
			    		return false;
			    	}
			    	return true;
			    })
			    .forEach((dp)->{
			        ddq.add(dp.getDrill(), dp.getPaths());
			    });


			if(!nonStandardFacets.isEmpty()){
				nonStandardFacets.add(filter);
				filter = new ChainedFilter(nonStandardFacets.toArray(new Filter[0])
						                  , ChainedFilter.AND);
			}



			qactual=ddq;

			// sideways
			if (options.isSideway()) {
				lsp = new DrillSidewaysLuceneSearchProvider(sorter, filter, options);

			// drilldown
			} else {
				lsp = new BasicLuceneSearchProvider(sorter, filter, options.max(), options.getIncludeFacets());
			}
		} // facets is empty


		//Promote special matches
		if(searchResult.getOptions().getPromoteSpecialMatches() && searchResult.getOptions().getKindInfo() !=null && gsrsRepository !=null){
		    
		   
		    //TODO katzelda October 2020 : don't support sponsored fields yet that's a Substance only thing
		    //Special "promoted" match types
			Set<String> specialExactMatchFields =  searchResult.getOptions()
			                                           .getKindInfo()
			                                           .getSpecialFields();

			if (searchResult.getQuery() != null ) {
			    String tqq = searchResult.getQuery().trim().replace("\"", "");
			    
			    //Hacky way of avoiding exact match searches if the query looks complex
			    //TODO: real parsing and analysis
                if(tqq.contains("*")||COMPLEX_QUERY_REGEX.matcher(tqq).find()||tqq.contains(" AND ")||tqq.contains(" OR ")) {

                } else {
			    
    				try {
    				    
    				    
    				    // Look through each of the special fields and see if there's an exact match for one of them,
    				    // if there IS, promote it
    				    for (String sp : specialExactMatchFields) {
    						String theQuery = "\"" + toExactMatchQueryString(
    								TextIndexer.replaceSpecialCharsForExactMatch(tqq)).toLowerCase() + "\"";
    						//Set the default query field to the special field
    						QueryParser parser = new IxQueryParser(sp, indexerService.getIndexAnalyzer());
    						
    						Query tq = parser.parse(theQuery);
    						if(lsp instanceof DrillSidewaysLuceneSearchProvider){
    							DrillDownQuery ddq2 = new DrillDownQuery(facetsConfig, tq);
    								options.getDrillDownsMapExcludingRanges()
    								    .entrySet()
    								    .stream()
    								    .flatMap(e->e.getValue().stream())
    								    .filter(dp->{
    								        String drill = dp.getDrill();
    									    return !drill.startsWith("^") && ! drill.startsWith("!");
    								    })
    								    .forEach((dp)->{
    									ddq2.add(dp.getDrill(), dp.getPaths());
    								    });
    							tq=ddq2;
    						}
    						LuceneSearchProviderResult lspResult = lsp.search(searcher, taxon, tq,new FacetsCollector()); //special q
    						TopDocs td = lspResult.getTopDocs();
    						for (int j = 0; j < td.scoreDocs.length; j++) {
    							Document doc = searcher.doc(td.scoreDocs[j].doc);
    							//TODO katzelda October 2020 : don't do sponsored yet
    							try {
    								Key k = LuceneSearchResultPopulator.keyOf(doc);
    								
    								searchResult.addSponsoredNamedCallable(new EntityFetcher(k));
    							} catch (Exception e) {
    								log.error("error adding special match callable", e);
    							}
    						}
    
    					}
    				} catch (Exception ex) {
    				    log.warn("Error performing lucene search", ex);
    				}
                } // end if else
			}
		}

		LuceneSearchProviderResult lspResult=lsp.search(searcher, taxon,qactual,facetCollector);
		hits=lspResult.getTopDocs();

		if(options.getIncludeFacets()) {
		     collectBasicFacets(lspResult.getFacets(), searchResult);
		     collectLongRangeFacets(facetCollector, searchResult);
		}

		

		if(options.getIncludeBreakdown() && textIndexerConfig.isFieldsuggest() && options.getKind()!=null){
			EntityInfo<?> entityMeta= EntityUtils.getEntityInfoFor(options.getKind());

			//TODO katzelda October 2020 : only fieldname decorator is substancefieldname decorator
            //which we haven't implemented substances yet and also it appears to be a play legacy ui only
            //to convert json lucene index field to human readable name

            //
            // TP July 2021: Actually it's used by the beta UI too. It'd be
            // great if it wasn't used by the
            // beta UI, since the data dictionary could be used instead, but
            // alas we are where we are.
            // We need to turn this back on.
            FieldNameDecorator fndt = (f) -> f;

            // TODO: this is unforgivable reflection of an entity layer in the
            // base
            // layer, but there isn't an existing ported factory like there was
            // in play yet
            if ("ix.ginas.models.v1.Substance".equals(entityMeta.getName())) {
                try {
                fndt = (FieldNameDecorator) EntityUtils
                        .getEntityInfoFor(
                                "ix.ginas.utils.SubstanceFieldNameDecorator")
                        .getInstance();
                }catch(Exception e) {
                    //swallow
                }
            }

            // For Application Module
            if ("gov.hhs.gsrs.application.application.models.Application".equals(entityMeta.getName())) {
                try {
                    fndt = (FieldNameDecorator) EntityUtils
                            .getEntityInfoFor(
                                    "gov.hhs.gsrs.application.application.ApplicationFieldNameDecorator")
                            .getInstance();
                }catch(Exception e) {
                    //swallow
                }
            }

            // For Product Module
            if ("gov.hhs.gsrs.products.productall.models.ProductMainAll".equals(entityMeta.getName())) {
                try {
                    fndt = (FieldNameDecorator) EntityUtils
                            .getEntityInfoFor(
                                    "gov.hhs.gsrs.products.productall.ProductFieldNameDecorator")
                            .getInstance();
                }catch(Exception e) {
                    //swallow
                }
            }

            // For Adverse Event PT, DME, and CVM Modules
            if (("gov.hhs.gsrs.adverseevents.adverseeventpt.models.AdverseEventPt".equals(entityMeta.getName()))
                    || ("gov.hhs.gsrs.adverseevents.adverseeventdme.models.AdverseEventDme".equals(entityMeta.getName()))
                    || ("gov.hhs.gsrs.adverseevents.adverseeventcvm.models.AdverseEventCvm".equals(entityMeta.getName()))) {
                try {
                    fndt = (FieldNameDecorator) EntityUtils
                            .getEntityInfoFor(
                                    "gov.hhs.gsrs.adverseevents.AdverseEventFieldNameDecorator")
                            .getInstance();
                }catch(Exception e) {
                    //swallow
                }
            }

            FieldNameDecorator fnd=fndt;

			getQueryBreakDownFor(query).stream().forEach(oq->{
				try{
					FacetsCollector facetCollector2 = new FacetsCollector();
					Filter f=null;
					if(options.getKind()!=null){
						EntityUtils.getEntityInfoFor(options.getKind());
						List<String> analyzers = entityMeta.getTypeAndSubTypes()
									.stream()
									.map(e->e.getName())
									.map(n->ANALYZER_VAL_PREFIX + n)
									.collect(Collectors.toList());

						f = new FieldCacheTermsFilter(FIELD_KIND, analyzers.toArray(new String[0]));
					}
					LuceneSearchProvider lsp2 = new BasicLuceneSearchProvider(null, f, options.max(),true);
					LuceneSearchProviderResult res=lsp2.search(searcher, taxon,oq.k(),facetCollector2);
					res.getFacets().getAllDims(DEFAULT_ANALYZER_MATCH_FIELD_LIMIT).forEach(fr->{
						if(fr.dim.equals(TextIndexer.ANALYZER_FIELD)){

						Arrays.stream(fr.labelValues).forEach(lv->{
								String newQuery = serializeAndRestrictQueryToField(oq.k(),lv.label);
								searchResult.addFieldQueryFacet(
										new FieldedQueryFacet(lv.label)
												.withExplicitCount(lv.value.intValue())
												.withExplicitQuery(newQuery)
												.withExplicitMatchType(oq.v())
                                        //TODO katzelda October 2020 : only fieldname decorator is substancefieldname decorator
									    //TODO tyler July 2021 : Yes, but we still need it to work
												.withExplicitDisplayField(fnd.getDisplayName(lv.label))
												);
								});
						}
					});
				}catch(Exception e){
					log.warn("Error analyzing query:" + e.getMessage(), e);
				}
			});
		} //End of Idea

		return hits;
	}

	public IxQueryParser getQueryParser(String def){
		return new IxQueryParser(def, indexerService.getIndexAnalyzer());
	}

	public IxQueryParser getQueryParser(){
		return getQueryParser(FULL_TEXT_FIELD);
	}

//	@Deprecated
//	public Query parseQuery(String q) throws Exception{
//        QueryParser parser = getQueryParser();
//        turnOnSuffixSearchIfNeeded(q, parser);
//
//
//        Query q1=parser.parse(q);
//		//return q1;
//		return withSearcher(s -> q1.rewrite(s.getIndexReader()));
//	}


    /**
	 * Prepare a given query to be more specified by restricting it to the field
	 * provided, and using
	 * @param q
	 * @param field
	 * @return
	 */
	public static String serializeAndRestrictQueryToField(Query q, String field){
		String qAsIs=q.toString();

		//replace all mentions of text: with the actual field name provided
		qAsIs=qAsIs.replace(FULL_TEXT_FIELD + ":", field.replace(" ", "\\ ")
				                                .replace("(", "\\(")
				    				.replace(")", "\\)")
				    				.replace("/", "\\/") + ":");

		//START_WORD and STOP_WORD better be good regexes
		qAsIs= Util.replaceIgnoreCase(qAsIs, TextIndexer.START_WORD, TextIndexer.GIVEN_START_WORD);
		qAsIs= Util.replaceIgnoreCase(qAsIs, TextIndexer.STOP_WORD, TextIndexer.GIVEN_STOP_WORD);
		return qAsIs;
	}



	/**
	 * This method attempts to generate suggested variant queries (more specific)
	 * from an existing query. If this query deals with a non-generic field, or other
	 * query unlikely to be interpretable in analysis, it returns an empty list.
	 *
	 * <pre>
	 * If you provide -> Maybe you're actually interested in is
	 *
	 * Single Term Query      -> [Single Term Query, Exact Full Term Query]
	 * "OR" Term Query        -> [Exact Full Term Query, "OR" Term Query, Phrase Term Query]
	 * Phrase Term Query      -> [Exact Full Term Query, Phrase Term Query]
	 * Exact Term Query       -> [Exact Full Term Query]
	 * Starts With Term Query -> [Exact Full Term Query, Starts With Term Query]
	 * Ends With Term Query   -> [Exact Full Term Query, Ends With Term Query]
	 * Starts With * Query    -> [Starts With * Query]
	 * Field-specific Query   -> []
	 * "NOT" Term Query       -> []
	 * Other                  -> []
	 * </pre>
	 *
	 *
	 * This method also includes a MATCH_TYPE response for each new Query,
	 * which is used in categorization of the Query types.
	 *
	 * TODO: Align MATCH_TYPE options more with reality.
	 *
	 * @param q
	 * @return
	 */
	public static List<Tuple<Query,MATCH_TYPE>> getQueryBreakDownFor(Query q){
		List<Tuple<Query,MATCH_TYPE>> suggestedQueries = new ArrayList<Tuple<Query,MATCH_TYPE>>();

		Function<Stream<Term>, PhraseQuery> exatctQueryMaker = lterms->{
			PhraseQuery exactQuery = new PhraseQuery();
			exactQuery.add(new Term(FULL_TEXT_FIELD, TextIndexer.START_WORD.trim().toLowerCase()));
			lterms.forEach(tq->{
				exactQuery.add(new Term(FULL_TEXT_FIELD,tq.text()));
			});
			exactQuery.add(new Term(FULL_TEXT_FIELD, TextIndexer.STOP_WORD.trim().toLowerCase()));
			return exactQuery;
		};
		Function<Stream<Term>, PhraseQuery> phraseQueryMaker = lterms->{
			PhraseQuery exactQuery = new PhraseQuery();
			lterms.forEach(tq->{
				exactQuery.add(new Term(FULL_TEXT_FIELD,tq.text()));
			});
			return exactQuery;
		};
		Predicate<Term> isGeneric = (t->t.field().equals(FULL_TEXT_FIELD));

		//First, we explicity allow TermQueries
		if(q instanceof TermQuery){
			Term tq=((TermQuery)q).getTerm();
			if(tq.field().equals(FULL_TEXT_FIELD)){
				PhraseQuery exactQuery = new PhraseQuery();
				exactQuery.add(new Term(FULL_TEXT_FIELD, TextIndexer.START_WORD.trim().toLowerCase()));
				exactQuery.add(new Term(FULL_TEXT_FIELD,tq.text()));
				exactQuery.add(new Term(FULL_TEXT_FIELD, TextIndexer.STOP_WORD.trim().toLowerCase()));
				suggestedQueries.add(new Tuple<Query,MATCH_TYPE>(exatctQueryMaker.apply(Stream.of(tq)),MATCH_TYPE.FULL));
				suggestedQueries.add(new Tuple<Query,MATCH_TYPE>(q,MATCH_TYPE.WORD));
			}
		}else if(q instanceof PhraseQuery){
			PhraseQuery pq =(PhraseQuery)q;
			Term[] terms=pq.getTerms();
			if(Arrays.stream(terms).allMatch(isGeneric)){
				boolean starts=terms[0].text().equalsIgnoreCase(TextIndexer.START_WORD.trim());
				boolean ends=terms[terms.length-1].text().equalsIgnoreCase(TextIndexer.STOP_WORD.trim());

				if(starts && ends){
					//was exact
					suggestedQueries.add(
							new Tuple<Query, MATCH_TYPE>(
									q,
									MATCH_TYPE.FULL)
								);
				}else if(starts){
					//System.out.println("Start Only:" + q.toString());
				}else if(ends){
					//System.out.println("Ends Only:" + q.toString());
				}else{
					suggestedQueries.add(
							new Tuple<Query, MATCH_TYPE>(
									exatctQueryMaker.apply(Arrays.stream(terms)),
									MATCH_TYPE.FULL)
								);

					suggestedQueries.add(
							new Tuple<Query, MATCH_TYPE>(
									q,
									MATCH_TYPE.WORD)
								);
				}
			}else{
				//Some field-specific Phrase query?
				//System.out.println("reject:" + q.toString());
			}
		}else if(q instanceof BooleanQuery){
			BooleanQuery bq = (BooleanQuery)q;
			List<BooleanClause> bclauses = flattenToLinkedOr(bq);
			if(bclauses!=null){
				//ALL-OR Query
				List<Query> qs=bclauses.stream().map(b->b.getQuery()).collect(Collectors.toList());
				if(qs.stream().allMatch(qq->(qq instanceof TermQuery))){
					//All Terms
					List<Term> terms = qs.stream().map(qq -> ((TermQuery) qq).getTerm()).collect(Collectors.toList());
					if(terms.stream().allMatch(isGeneric)){
						suggestedQueries.add(
								new Tuple<Query, MATCH_TYPE>(
										exatctQueryMaker.apply(terms.stream()),
										MATCH_TYPE.FULL)
									);

						suggestedQueries.add(
								new Tuple<Query, MATCH_TYPE>(
										phraseQueryMaker.apply(terms.stream()),
										MATCH_TYPE.CONTAINS)
									);

						suggestedQueries.add(
								new Tuple<Query, MATCH_TYPE>(
										q,
										MATCH_TYPE.WORD)
									);


					}else{
						//More complex?
						//System.out.println("Specified term?" + q.toString());
					}
				}else{
					//More complex?
					//System.out.println("Non term?" + q.toString());
				}
			}else{
				//Boolean query is complex, with things other than "OR"
				//System.out.println("Something else QUERY" + q.toString());
			}
		}else if(q instanceof WildcardQuery){
			WildcardQuery wq = (WildcardQuery)q;
			if(isGeneric.test(wq.getTerm())){
				suggestedQueries.add(
						new Tuple<Query, MATCH_TYPE>(
								q,
								MATCH_TYPE.CONTAINS)
							);
			}else{
				//System.out.println("This is a non generic wildcard");
			}
		}else if(q instanceof PrefixQuery){
			PrefixQuery pq = (PrefixQuery)q;
			if(isGeneric.test(pq.getPrefix())){
				suggestedQueries.add(
						new Tuple<Query, MATCH_TYPE>(
								q,
								MATCH_TYPE.WORD_STARTS_WITH)
							);
			}else{
				//System.out.println("This is a non generic Prefix query");
			}
		}
		return suggestedQueries;
	}

	private static List<BooleanClause> flattenToLinkedOr(BooleanQuery bq){
		List<BooleanClause> bqs= new ArrayList<BooleanClause>();
		for(BooleanClause bcl: bq.getClauses()){
			if(bcl.getOccur() != Occur.SHOULD) return null;
			if(bcl.getQuery() instanceof BooleanQuery){
				List<BooleanClause> sublist = flattenToLinkedOr((BooleanQuery) bcl.getQuery());
				if(sublist==null){
					return null;
				}else{
					bqs.addAll(sublist);
				}
			}else{
				bqs.add(bcl);
			}
		}
		return bqs;
	}

	protected void collectLongRangeFacets(FacetsCollector fc, SearchResult searchResult) throws IOException {
		SearchOptions options = searchResult.getOptions();
		Map<String,List<DrillAndPath>> providedDrills = options.getDrillDownsMap();

		for (SearchOptions.FacetLongRange flr : options.getLongRangeFacets()) {
			if (flr.range.isEmpty())
				continue;

			log.debug("[Range facet: \"" + flr.field + "\"");

			//
			LongRange[] range = flr.range
			                .entrySet()
			                .stream().map(Tuple::of)
			                .map(me ->new LongRange(me.k(), me.v()[0], true, me.v()[1], true))
			                .toArray(i->new LongRange[i]);

			Facets facets = new LongRangeFacetCounts(flr.field, fc, range);
			FacetResult result = facets.getTopChildren(options.getFdim(), flr.field);
			FacetImpl f = new FacetImpl(result.dim, searchResult);

			f.enhanced=false;
//			if (DEBUG(1)) {
//				log.info(" + [" + result.dim + "]");
//			}

			Arrays.stream(result.labelValues)
				.forEach(lv->f.add(lv.label, lv.value.intValue()));

			List<DrillAndPath> dp = providedDrills.get(f.name);
			if (dp != null) {
				List<String> selected=dp.stream()
						                .map(l->l.asLabel())
						                .collect(Collectors.toList());
				f.setSelectedLabels(selected);
			}


			searchResult.addFacet(f);
		}
	}

	protected <T> Term getTerm(T entity) {
		if (entity == null)
			return null;

		return EntityWrapper.of(entity)
		           .getOptionalKey()
                   .map(key->getTerm(key))
                   .orElseGet(()->{
                       log.warn("Entity " + entity + "[" + entity.getClass() + "] has no Id field!");
                       return null;
                   });
	}

	protected <T> Term getTerm(Key k){
	    Tuple<String,String> tup=k.asLuceneIdTuple();
	    return new Term(tup.k(),tup.v());
	}

	public Document getDoc(Object entity) throws Exception {
		Term term = getTerm(entity);
		if (term != null) {
			// IndexSearcher searcher = getSearcher ();
			withSearcher(searcher -> {
				TopDocs docs = searcher.search(new TermQuery(term), 1);
				if (docs.totalHits > 0) {
					return searcher.doc(docs.scoreDocs[0].doc);
				}
				return null;
			});
		}
		return null;
	}

	public JsonNode getDocJson(Object entity) throws Exception {
		Document _doc = getDoc(entity);
		if (_doc == null) {
			return null;
		}
		List<IndexableField> _fields = _doc.getFields();
		ObjectMapper mapper = new ObjectMapper();
		ArrayNode fields = mapper.createArrayNode();
		for (IndexableField f : _fields) {
			ObjectNode node = mapper.createObjectNode();
			node.put("name", f.name());
			if (null != f.numericValue()) {
				node.put("value", f.numericValue().doubleValue());
			} else {
				node.put("value", f.stringValue());
			}

			ObjectNode n = mapper.createObjectNode();
			IndexableFieldType type = f.fieldType();
			if (type.docValueType() != null)
				n.put("docValueType", type.docValueType().toString());
			n.put("indexed", type.indexed());
			n.put("indexOptions", type.indexOptions().toString());
			n.put("omitNorms", type.omitNorms());
			n.put("stored", type.stored());
			n.put("storeTermVectorOffsets", type.storeTermVectorOffsets());
			n.put("storeTermVectorPayloads", type.storeTermVectorPayloads());
			n.put("storeTermVectorPositions", type.storeTermVectorPositions());
			n.put("storeTermVectors", type.storeTermVectors());
			n.put("tokenized", type.tokenized());

			node.put("options", n);
			fields.add(node);
		}

		ObjectNode doc = mapper.createObjectNode();
		doc.put("num_fields", _fields.size());
		doc.put("fields", fields);
		return doc;
	}

	public void update(EntityWrapper ew) throws Exception{
	    Lock l = stripedLock.get(ew.getKey());
	    l.lock();
	    try {
            remove(ew);
            add(ew, true);
        }finally{
	        l.unlock();
        }

    }
    public void add(EntityWrapper ew) throws IOException {
        //Don't index if any of the following:
        // 1. The entity doesn't have an Indexable annotation OR
        // 2. The config is set to only index things with Indexable Root annotation and the entity doesn't have that annotation
        // 3. Reindexing is happening and the entity has already been indexed
        boolean shouldNotAdd=     !ew.shouldIndex() ||
                (textIndexerConfig.isRootIndexOnly() && !ew.isRootIndex()) ||
                (isReindexing.get() && !alreadySeenDuringReindexingMode.add(ew.getKey().toString()));
        
        add(ew,!shouldNotAdd);
        
    }
	/**
	 * recursively index any object annotated with Entity
	 */
	private void add(EntityWrapper ew, boolean force) throws IOException {
		if(!textIndexerConfig.isEnabled()){
		    return;
        }
		Objects.requireNonNull(ew);

		if(     !force){
		    return;
		}

        Lock l = stripedLock.get(ew.getKey());
        l.lock();
        try{
            ew.toInternalJson();
			HashMap<String,List<TextField>> fullText = new HashMap<>();
            Document doc = new Document();
            if(textIndexerConfig.isShouldLog()){
                LogUtil.debug(()->{
                    String beanId;
                    if(ew.hasIdField()){
                        beanId = ew.getKey().toString();
                    }else{
                        beanId = ew.toString();
                    }
                    return "[LOG_INDEX] =======================\nINDEXING BEAN "+ beanId;
                });

            }
			Consumer<IndexableField> fieldCollector = f->{

					if(f instanceof TextField || f instanceof StringField){
						String text = f.stringValue();
						if (text != null) {
                            if(textIndexerConfig.isShouldLog()){
                                log.debug("[LOG_INDEX] .." + f.name() + ":" + text + " [" + f.getClass().getName() + "]");
                            }
// This is where you can see how things get indexed.
//						    System.out.println(".." + f.name() + ":" + text + " [" + f.getClass().getName() + "]");
//							if (DEBUG(2)){
//								log.debug(".." + f.name() + ":" + text + " [" + f.getClass().getName() + "]");
//							}
							TextField tf=new TextField(FULL_TEXT_FIELD, text, NO);
							//tf.set
							doc.add(tf);
							if(textIndexerConfig.isFieldsuggest() && deepKindFunction.apply(ew) && f.name().startsWith(ROOT +"_")){
								fullText.computeIfAbsent(f.name(),k->new ArrayList<TextField>())
									.add(tf);
							}
						}
					}else if(f instanceof FacetField){
					    String key = ((FacetField)f).dim;
					    String text = ((FacetField)f).path[0];

					    if (text != null) {
					        TermVectorField tvf = new TermVectorField(TERM_VEC_PREFIX + key,text);
					        doc.add(tvf);
					    }
					}
					doc.add(f);
			};

			//flag the kind of document
			IndexValueMaker<Object> valueMaker= indexValueMakerFactory.createIndexValueMakerFor(ew);
			valueMaker.createIndexableValues(ew.getValue(), iv->{
				this.instrumentIndexableValue(fieldCollector, iv);
			});
			if(textIndexerConfig.isFieldsuggest()  && deepKindFunction.apply(ew) && ew.hasKey()){
				Key key =ew.getKey();
				if(!key.getIdString().equals("")){  //probably not needed
					StringField toAnalyze=new StringField(FIELD_KIND, ANALYZER_VAL_PREFIX + ew.getKind(),YES);
					StringField analyzeMarker=new StringField(ANALYZER_MARKER_FIELD, "true",YES);


					Tuple<String,String> luceneKey = key.asLuceneIdTuple();
					StringField docParent=new StringField(ANALYZER_VAL_PREFIX+luceneKey.k(),luceneKey.v(),YES);
					FacetField docParentFacet =new FacetField(ANALYZER_VAL_PREFIX+luceneKey.k(),luceneKey.v());
					//This is a test of a terrible idea, which just. might. work.
					fullText.forEach((name,group)->{
							try{
                                Document fielddoc = new Document();
								fielddoc.add(toAnalyze);
								fielddoc.add(analyzeMarker);
								fielddoc.add(docParent);
								fielddoc.add(docParentFacet);
								fielddoc.add(new FacetField(ANALYZER_FIELD,name));
								for(IndexableField f:group){
										fielddoc.add(f);
								}
								addDoc(fielddoc);
							}catch(Exception e){
								log.error("Analyzing index failed", e);
							}
						});
				}
			}

			fieldCollector.accept(new StringField(FIELD_KIND, ew.getKind(), YES));
			fieldCollector.accept(new StringField(ANALYZER_MARKER_FIELD, "false", YES));

			// now index
			addDoc(doc);

//			if (DEBUG(2)) {
//                log.debug("<<< " + ew.getValue());
//            }
		}catch(Exception e){
			log.error("Error indexing record [" + ew.toString() + "] This may cause consistency problems", e);
		}finally{
            l.unlock();
        }
	}

	//One more thing:
	// 1. need list of fields indexed.
	// 2. that's easy! At index time, just also index each field
	// 3. in fact... it's already maybe present ...

	public void addDoc(Document doc) throws IOException {
		doc = facetsConfig.build(taxonWriter, doc);
//		if (DEBUG(2))
//			log.debug("++ adding document " + doc);
		indexerService.addDocument(doc);
        notifyListenersAddDocument(doc);
		markChange();
	}


	//TODO: Should be an interface, which can throw a DataHasChange event ... or something
	// like that
	public void markChange(){
		lastModified.set(TimeUtil.getCurrentTimeMillis());
		if(gsrscache!=null) {
		    gsrscache.markChange();
		}

	}



	public boolean hasBeenModifiedSince(long thistime){
		if(lastModified()>thistime)return true;
		return false;
	}

	public long lastModified() {
		return lastModified.get();
	}


	public void remove(EntityWrapper ew) throws Exception {
		if (ew.shouldIndex()) {
			if (ew.hasKey()) {
				remove(ew.getKey());
			} else {
				log.warn("Entity " + ew.getKind() + "'s Id field is null!");
			}
		}
	}

	public void remove(Key key) throws Exception {
        Lock l = stripedLock.get(key);
        l.lock();
        try {
            Tuple<String, String> docKey = key.asLuceneIdTuple();
            //if (DEBUG(2)){
            log.debug("Deleting document " + docKey.k() + "=" + docKey.v() + "...");
            //}

            BooleanQuery q = new BooleanQuery();
            q.add(new TermQuery(new Term(docKey.k(), docKey.v())), BooleanClause.Occur.MUST);
            q.add(new TermQuery(new Term(FIELD_KIND, key.getKind())), BooleanClause.Occur.MUST);

            indexerService.deleteDocuments(q);
            notifyListenersDeleteDocuments(q);

            if (textIndexerConfig.isFieldsuggest()) { //eliminate
                BooleanQuery qa = new BooleanQuery();
                qa.add(new TermQuery(new Term(ANALYZER_VAL_PREFIX + docKey.k(), docKey.v())), BooleanClause.Occur.MUST);
                qa.add(new TermQuery(new Term(FIELD_KIND, ANALYZER_VAL_PREFIX + key.getKind())), BooleanClause.Occur.MUST);
                indexerService.deleteDocuments(qa);
                notifyListenersDeleteDocuments(qa);
            }
            markChange();
        }finally{
            l.unlock();
        }
	}
	

	public void removeAllType(EntityInfo<?> ei) throws Exception{
	    ei.getTypeAndSubTypes()
    	  .forEach(ee->{
    	        TermQuery q = new TermQuery(new Term(FIELD_KIND, ee.getName()));
    	        indexerService.deleteDocuments(q);
    	        notifyListenersDeleteDocuments(q);

    	        //Delete pseudo documents associated with meta-data for field suggest
    	        if(textIndexerConfig.isFieldsuggest()){
    	            TermQuery qq= new TermQuery(new Term(FIELD_KIND, ANALYZER_VAL_PREFIX + ee.getName()));
    	            indexerService.deleteDocuments(qq);
    	            notifyListenersDeleteDocuments(qq);
    	        }
    	  });
	    
        
		markChange();
	}


	public void removeAllType(Class<?> type) throws Exception{
		removeAllType(EntityUtils.getEntityInfoFor(type));
	}
	
	public void remove(Query query) throws Exception {
	    log.debug("## removing documents: " + query);
	    indexerService.deleteDocuments(query);
	    notifyListenersDeleteDocuments(query);
	}


	public void remove(String text) throws Exception {
		try {
			QueryParser parser = new QueryParser(LUCENE_VERSION, FULL_TEXT_FIELD, indexerService.getIndexAnalyzer());
			Query query = parser.parse(text);
			log.debug("## removing documents: " + query);
            indexerService.deleteDocuments(query);
            notifyListenersDeleteDocuments(query);
		} catch (ParseException ex) {
			log.warn("Can't parse query expression: " + text, ex);
			throw new IllegalArgumentException("Can't parse query: " + text, ex);
		}
	}






	/**
	 * Gets the Facet field for a range query. Find the interval that the
	 * given value falls within. Uses that to make a Facet.
	 * @param name
	 * @param ranges
	 * @param value
	 * @return
	 */
	static FacetField getRangeFacet(String name, long[] ranges, long value) {
		if (ranges.length == 0){
		    return null;
        }
		if (value < ranges[0]){
		    return new FacetField(name, "<" + ranges[0]);
        }
		int i=0;
		for (; i < ranges.length; ++i) {
			if (value < ranges[i])
				break;
		}
		if (i == ranges.length) {
			return new FacetField(name, ">" + ranges[i - 1]);
		}
		return new FacetField(name, ranges[i - 1] + ":" + (ranges[i]-1));
	}

	static FacetField getRangeFacet(String name, double[] ranges, double value, String format) {
		if (ranges.length == 0)
			return null;

		if (value < ranges[0]) {
			return new FacetField(name, "<" + String.format(format, ranges[0]));
		}

		int i = 1;
		for (; i < ranges.length; ++i) {
			if (value < ranges[i])
				break;
		}

		if (i == ranges.length) {
			return new FacetField(name, ">" + String.format(format, ranges[i - 1]));
		}

		return new FacetField(name, String.format(format, ranges[i - 1]) + ":" + String.format(format, ranges[i]));
	}

	static void setFieldType(FieldType ftype) {
		ftype.setIndexed(true);
		ftype.setTokenized(true);
		ftype.setStoreTermVectors(true);
		ftype.setIndexOptions(IndexOptions.DOCS_AND_FREQS_AND_POSITIONS_AND_OFFSETS);
	}


	static FacetsConfig getFacetsConfig(JsonNode node) {
		if (!node.isContainerNode())
			throw new IllegalArgumentException("Not a valid json node for FacetsConfig!");

		String text = node.get("version").asText();
		Version ver = Version.parseLeniently(text);
		if (!ver.equals(LUCENE_VERSION)) {
			log.warn("Facets configuration version (" + ver + ") doesn't " + "match index version (" + LUCENE_VERSION
					+ ")");
		}

		FacetsConfig config = null;
		ArrayNode array = (ArrayNode) node.get("dims");
		if (array != null) {
			config = new FacetsConfig();
			for (int i = 0; i < array.size(); ++i) {
				ObjectNode n = (ObjectNode) array.get(i);
				String dim = n.get("dim").asText();
				config.setHierarchical(dim, n.get("hierarchical").asBoolean());
				config.setIndexFieldName(dim, n.get("indexFieldName").asText());
				config.setMultiValued(dim, n.get("multiValued").asBoolean());
				config.setRequireDimCount(dim, n.get("requireDimCount").asBoolean());
			}
		}

		return config;
	}

	static JsonNode setFacetsConfig(FacetsConfig config) {
		ObjectMapper mapper = new ObjectMapper();
		ObjectNode node = mapper.createObjectNode();
		node.put("created", TimeUtil.getCurrentTimeMillis());
		node.put("version", LUCENE_VERSION.toString());
		node.put("warning", "AUTOMATICALLY GENERATED FILE; DO NOT EDIT");
		Map<String, FacetsConfig.DimConfig> dims = config.getDimConfigs();
		node.put("size", dims.size());
		ArrayNode array = node.putArray("dims");
		for (Map.Entry<String, FacetsConfig.DimConfig> me : dims.entrySet()) {
			FacetsConfig.DimConfig c = me.getValue();
			ObjectNode n = mapper.createObjectNode();
			n.put("dim", me.getKey());
			n.put("hierarchical", c.hierarchical);
			n.put("indexFieldName", c.indexFieldName);
			n.put("multiValued", c.multiValued);
			n.put("requireDimCount", c.requireDimCount);
			array.add(n);
		}
		return node;
	}

	File getFacetsConfigFile() {
		return new File(baseDir, FACETS_CONFIG_FILE);
	}

	File getSorterConfigFile() {
		return new File(baseDir, SORTER_CONFIG_FILE);
	}

	static void saveFacetsConfig(File file, FacetsConfig facetsConfig) {
		JsonNode node = setFacetsConfig(facetsConfig);
		ObjectMapper mapper = new ObjectMapper();
		try (OutputStream out = new BufferedOutputStream(new FileOutputStream(file))) {

			mapper.writerWithDefaultPrettyPrinter().writeValue(out, node);

		} catch (IOException ex) {
			log.trace("Can't persist facets config!", ex);
			ex.printStackTrace();
		}
	}

	static FacetsConfig loadFacetsConfig(File file) {
		FacetsConfig config = null;
		if (file.exists()) {
			ObjectMapper mapper = new ObjectMapper();
			try {
				JsonNode conf = mapper.readTree(file);
				config = getFacetsConfig(conf);
				log.info("## FacetsConfig loaded with " + config.getDimConfigs().size() + " dimensions!");
			} catch (Exception ex) {
				log.trace("Can't read file " + file, ex);
			}
		}
		return config;
	}


	static ConcurrentMap<String, SortField.Type> loadSorters(File file) {
		ConcurrentMap<String, SortField.Type> sorters = new ConcurrentHashMap<String, SortField.Type>();
		if (file.exists()) {
			ObjectMapper mapper = new ObjectMapper();
			try {
				JsonNode conf = mapper.readTree(new BufferedInputStream(new FileInputStream(file)));
				ArrayNode array = (ArrayNode) conf.get("sorters");
				if (array != null) {
					for (int i = 0; i < array.size(); ++i) {
						ObjectNode node = (ObjectNode) array.get(i);
						String field = node.get("field").asText();
						String type = node.get("type").asText();
						sorters.put(field, SortField.Type.valueOf(SortField.Type.class, type));
					}
				}
			} catch (Exception ex) {
				log.trace("Can't read file " + file, ex);
			}
		}
		return sorters;
	}

	static void saveSorters(File file, Map<String, SortField.Type> sorters) {
		ObjectMapper mapper = new ObjectMapper();

		ObjectNode conf = mapper.createObjectNode();
		conf.put("created", TimeUtil.getCurrentTimeMillis());
		ArrayNode node = mapper.createArrayNode();
		for (Map.Entry<String, SortField.Type> me : sorters.entrySet()) {
			ObjectNode obj = mapper.createObjectNode();
			obj.put("field", me.getKey());
			obj.put("type", me.getValue().toString());
			node.add(obj);
		}
		conf.put("sorters", node);

		try (OutputStream fos = new BufferedOutputStream(new FileOutputStream(file))) {

			mapper.writerWithDefaultPrettyPrinter().writeValue(fos, conf);
		} catch (Exception ex) {
			log.trace("Can't persist sorter config!", ex);
			ex.printStackTrace();
		}
	}

	/**
	 * Closing this indexer will shut it down. This is the same as calling
	 * {@link #shutdown()}.
	 */
	@Override
	public void close() {
		shutdown();
	}

    @Override
	public void newProcess() {
        clearAllIndexes(true);
	}
	
    
    
    public void clearAllIndexes(boolean setupForReindexing) {
        if(!isReindexing.get()) {
            flushDaemon.lockFlush();
            try {
                //0. Notify and mark that it's happening
                isReindexing.set(true);
                notifyListenersRemoveAll();

                //0.5 Set up space for use in reindexing
                alreadySeenDuringReindexingMode = Collections.newSetFromMap(new ConcurrentHashMap<>(100_000));

                //*************
                //1. START CLEAR SUGGEST
                //*************
                closeAndClear(lookups); //delete suggest dirs?
                IOUtil.deleteRecursivelyQuitely(suggestDir);
                //remake the suggest directory again
                suggestDir.mkdirs();
                //*************
                //1. END CLEAR SUGGEST
                //*************

                //*************
                //2. START CLEAR SORTERS
                //*************
                sorters.clear();
                deleteFileIfExists(getSorterConfigFile());
                //*************
                //2. END CLEAR SORTERS
                //*************

                //*************
                //3. START CLEAR core index service
                //*************
                indexerService.removeAll();
                //*************
                //3. END CLEAR core index service
                //*************


                //*************
                //4. START CLEAR FACETS [THIS IS NEW]
                //*************
                IOUtil.closeQuietly(taxonWriter);
                IOUtil.closeQuietly(taxonDir);
                IOUtil.deleteRecursivelyQuitely(facetFileDir);
                //also delete facet and sorter files if they exist
                deleteFileIfExists(getFacetsConfigFile());
                //*************
                //4. END CLEAR FACETS  [THIS IS NEW]
                //*************

                try {
                    this.initialSetup();
                } catch (Exception e) {
                    log.error("Trouble starting up textindexer on reindexing", e);
                }
            } catch(Exception e) {
                // Add catch here for logging, and then sneaky throw
                log.error("Trouble clearing all indexes on reindexing.");
                Sneak.sneakyThrow(e);
            }finally {
                flushDaemon.unLockFlush();
            }
        }
        if(!setupForReindexing) {
            finishReindexing();
        }
	}

    private <K, V extends Closeable> void closeAndClear(Map<K, V> map){
        Iterator<Map.Entry<K, V>> iter = map.entrySet().iterator();
        while(iter.hasNext()){
            closeAndIgnore(iter.next().getValue());
            iter.remove();

        }

    }
    
    private void finishReindexing() {
        isReindexing.set(false);
        alreadySeenDuringReindexingMode =null;
    }
    
	@Override
	public void doneProcess() {
	    finishReindexing();
	}

	@Override
	public void recordProcessed(Object o) {	}

	@Override
	public void error(Throwable t) {	}

	@Override
	public void totalRecordsToProcess(int total) {	}

	@Override
	public void countSkipped(int numSkipped) {	}

	public void shutdown() {
		if (isShutDown) {
			return;
		}
		try {
			if (scheduler != null) {
				try {
					isShutDown = true;
					scheduler.shutdown();
					scheduler.awaitTermination(1, TimeUnit.MINUTES);
					flushDaemon.lockFlush();
					flushDaemon.execute();
				} catch (Throwable e) {
				    log.warn("problem shutting down textindexer", e);
//					throw new RuntimeException(e);
				}
			}

			saveFacetsConfig(getFacetsConfigFile(), facetsConfig);
			saveSorters(getSorterConfigFile(), sorters);

			for (SuggestLookup look : lookups.values()) {
				closeAndIgnore(look);
			}
			// clear the lookup value map
			// if we restart without clearing we might
			// think we have lookups we don't have if we delete the ginas.ix
			// area
			lookups.clear();

			closeAndIgnore(searchManager);
			closeAndIgnore(indexerService);
			closeAndIgnore(taxonWriter);

			closeAndIgnore(taxonDir);

		} catch (Exception ex) {
			ex.printStackTrace();
			log.trace("Closing index", ex);
		} finally {
			if (indexers != null) {
				if (baseDir != null) {
					TextIndexer indexer = indexers.remove(baseDir);
				}
			}
			if (!isEmptyPool) {
				threadPool.shutdown();
				try {
					threadPool.awaitTermination(1, TimeUnit.MINUTES);
				} catch (Exception e) {
					log.warn("problem shutting down textindexer threadpool", e);
				}
			}
			isShutDown = true;
		}
	}


	public TermVectors getTermVectors (Class kind, String field) throws Exception{
	    return getTermVectors(kind,field, (Filter)null, null);
	}

	public TermVectors getTermVectors(Class kind, String field, List<Object> subset, Query q) throws Exception{
	    LazyList<Key, Object> llist = LazyList.of(subset, (o)-> EntityWrapper.of(o).getKey());

	    List<Key> klist = llist.getInternalList()
	         .stream()
	         .map(n->n.getName())
	         .collect(Collectors.toList());

	    return getTermVectorsFromKeys(kind,field, klist,q);
    }

	public TermVectors getTermVectorsFromKeys (Class kind, String field, List<Key> subset, Query q) throws Exception{
	    return getTermVectors(kind,field, getTermKeysFilter(subset),q);
    }


	public TermVectors getTermVectors (Class kind, String field, Filter luceneFilter, Query query)
	                throws Exception {

	    return withSearcher(searcher -> {
	        return TermVectorsCollector.make(kind, field, searcher, luceneFilter, query)
	               .termVectors();
        });
	}

	private static void closeAndIgnore(Closeable closeable) {
		if (closeable == null) {
			return;
		}
		try {
			closeable.close();
		} catch (Exception e) {
//			System.out.println(e.getMessage());
		}
	}

	// ************************************
	// Things that modify state are below:


	//make the fields for the dynamic facets

	public void createDynamicField(Consumer<IndexableField> fieldTaker, IndexableValue iv) {
		facetsConfig.setMultiValued(iv.name(), true);
		facetsConfig.setRequireDimCount(iv.name(), true);
		fieldTaker.accept(new FacetField(iv.name(), iv.value().toString()));
		fieldTaker.accept(new TextField(iv.path(), TextIndexer.START_WORD + iv.value().toString() + TextIndexer.STOP_WORD, NO));

		if(iv.suggest()){
			addSuggestedField(iv.name(),iv.value().toString(),iv.suggestWeight());
		}

	}

	//make the fields for the primitive fields

	public void instrumentIndexableValue(Consumer<IndexableField> fields, IndexableValue indexableValue) {


		// Used to be configurable, now just always NO
		// for all cases we use.
		org.apache.lucene.document.Field.Store store = NO;

		if(indexableValue.isDirectIndexField()){
			fields.accept((IndexableField) indexableValue.getDirectIndexableField());
			return;
		}

		if(indexableValue.isDynamicFacet()){
			createDynamicField(fields,indexableValue);
			if(indexableValue.sortable()){
				sorters.put(SORT_PREFIX + indexableValue.name(), SortField.Type.STRING);
				
				fields.accept(new StringField(SORT_PREFIX + indexableValue.name(), indexableValue.value().toString(), store));
			}
			return;
		}


		String fname = indexableValue.name();
		String name = indexableValue.rawName();

		String full = indexableValue.path();
		Object value = indexableValue.value();
		boolean sorterAdded = false;
		boolean asText = true;

		Object nvalue = value;

		org.apache.lucene.document.Field.Store shouldStoreLong= NO;

		if (value instanceof Date) {
			long date = ((Date) value).getTime();
			nvalue = date;
			asText = indexableValue.facet();
			if (asText) {
				value = YEAR_DATE_FORMAT.get().format(date);

			}

		}

		if(nvalue instanceof Number){
			// fields.add(new DoubleDocValuesField (full, (Double)value));
			Number dval = (Number) nvalue;

			boolean addedFacet = false;
			if(nvalue instanceof Long  || nvalue instanceof Integer || (indexableValue.ranges()!=null && indexableValue.ranges().length>0)){
			    Long lval =  dval.longValue();
			    fields.accept(new LongField(full, lval, shouldStoreLong));
			    asText = indexableValue.facet();
			    if (!asText && !name.equals(full)) {
			        fields.accept(new LongField(name, lval, store));
			    }
			    if(indexableValue.facet()){
			        FacetField ffl = getRangeFacet(fname, indexableValue.ranges(), lval);
			        if (ffl != null) {
			            facetsConfig.setMultiValued(fname, true);
			            facetsConfig.setRequireDimCount(fname, true);
			            fields.accept(ffl);
			            asText = false;
			            addedFacet=true;
			        }
			    }
			}


			fields.accept(new DoubleField("D_" +name, dval.doubleValue(), store));
			if (!full.equals(name)) {
				fields.accept(new DoubleField("D_" +full, dval.doubleValue(), NO));
			}
			if (indexableValue.sortable()) {
				String f = SORT_PREFIX + full;
				sorters.put(f, SortField.Type.DOUBLE);
				                
				fields.accept(new DoubleField(f, dval.doubleValue(), NO));
				sorterAdded = true;
			}
			if(indexableValue.facet() && !addedFacet){
			    FacetField ff = getRangeFacet(fname, indexableValue.dranges(), dval.doubleValue(), indexableValue.format());
			    if (ff != null) {
			        facetsConfig.setMultiValued(fname, true);
			        facetsConfig.setRequireDimCount(fname, true);
			        fields.accept(ff);
			    }
			}
			asText = false;

			}




		if (asText) {
			String text = (value ==null?  "" : value.toString());
			if(text.trim().isEmpty()){
				if(indexableValue.indexEmpty()){
					text=indexableValue.emptyString();
				}else{
					return;
				}
			}
			String dim = indexableValue.name();
			
			if("".equals(dim)){
				dim = full;
			}
			
			if (indexableValue.facet() || indexableValue.taxonomy()) {


				if (indexableValue.taxonomy()) {
                    facetsConfig.setMultiValued(dim, true);
                    facetsConfig.setRequireDimCount(dim, true);
					facetsConfig.setHierarchical(dim, true);

					fields.accept(new FacetField(dim, indexableValue.splitPath(text)));
				} else {
                    if(indexableValue.useFullPath()){
                        facetsConfig.setMultiValued(full, true);
                        facetsConfig.setRequireDimCount(full, true);
                        fields.accept(new FacetField(full, text));
                    }else {
                        facetsConfig.setMultiValued(dim, true);
                        facetsConfig.setRequireDimCount(dim, true);
                        fields.accept(new FacetField(dim, text));
                    }
				}
			}

			if (indexableValue.suggest()) {
				// also index the corresponding text field with the
				// dimension name
				fields.accept(new TextField(dim, text, NO));
				addSuggestedField(dim, text, indexableValue.suggestWeight());
			}

			String exactMatchStr = toExactMatchString(text);
			String exactMatchStrContinuous = toExactMatchStringContinuous(text);

			if (!(value instanceof Number)) {
				if (!name.equals(full)){
					// Added exact match
					fields.accept(new TextField(full,exactMatchStr, NO));
					fields.accept(new TextField(full,exactMatchStrContinuous , NO));
				}
			}

			// Add specific sort column only if it's not added by some other
			// mechanism
			if (indexableValue.sortable() && !sorterAdded) {
				sorters.put(SORT_PREFIX + full, SortField.Type.STRING);
				
				fields.accept(new StringField(SORT_PREFIX + full, text, store));
			}
			// Added exact match
			fields.accept(new TextField(name, exactMatchStr , store));
			fields.accept(new TextField(name, exactMatchStrContinuous , store));
		}
	}
	

	public static String toExactMatchString(String in){
		return TextIndexer.START_WORD + replaceSpecialCharsForExactMatch(in) + TextIndexer.STOP_WORD;
	}

	public static String toExactMatchStringContinuous(String in){
		return TextIndexer.START_WORD + replaceTokenSplitCharsWithString(replaceSpecialCharsForExactMatch(in)) + TextIndexer.STOP_WORD;
	}

	public static String replaceTokenSplitCharsWithString(String in){			
		String replaceString = in.replaceAll("[&\\-\\s\\.]", SPACE_WORD);
		return replaceString;	 
	}

	public static String toExactMatchQueryString(String in){
        return toExactMatchString(in).replace("*", "").replace("?", ""); //remove wildcards
    }

	private static String replaceSpecialCharsForExactMatch(String in) {
        // This is called when indexing or in cases where just field value in the input parameter value.
        String tmp = in;
        // The method getEncoder() returns a combined encoder.
        tmp = DefaultIndexedTextEncoderFactory.getInstance().getEncoder().encode(tmp);
        return tmp;
	}

	/*
	qtext = qtext.replace(TextIndexer.GIVEN_START_WORD, TextIndexer.START_WORD);
				qtext = qtext.replace(TextIndexer.GIVEN_STOP_WORD, TextIndexer.STOP_WORD);
	 */

	//TODO: this is a fairly hacky way to try to recreate simple character sequence-level
	//functionality within lucene, and there needs to be a better way
	private static String transformQueryForExactMatch(String in){
        // This is called when doing searches and maybe other cases
		String tmp =  START_PATTERN.matcher(in).replaceAll(TextIndexer.START_WORD);
		tmp =  STOP_PATTERN.matcher(tmp).replaceAll(TextIndexer.STOP_WORD);

        // The method getEncoder() returns a combined encoder.
        tmp = DefaultIndexedTextEncoderFactory.getInstance().getEncoder().encodeQuery(tmp);
        return tmp;
	}

	private static final Pattern START_PATTERN = Pattern.compile(TextIndexer.GIVEN_START_WORD,Pattern.LITERAL );
	private static final Pattern STOP_PATTERN = Pattern.compile(TextIndexer.GIVEN_STOP_WORD,Pattern.LITERAL );

	/**
	 * Add the specified field and value pair to the suggests
	 * which are used for type-ahead queries.
	 * @param name
	 * @param value
	 */
	void addSuggestedField(String name, String value, int weight) {
		name = SUGGESTION_WHITESPACE_PATTERN.matcher(name).replaceAll("_");
		try {
			SuggestLookup lookup = lookups.computeIfAbsent(name, n -> {
				try {
					return new SuggestLookup(n);
				} catch (Exception ex) {
					ex.printStackTrace();
					log.trace("Can't create Lookup!", ex);
					return null;
				}
			});
			if (lookup != null) {
				lookup.addSuggest(value, weight);
			}

		} catch (Exception ex) {
			log.trace("Can't create Lookup!", ex);
		}
	}
}
