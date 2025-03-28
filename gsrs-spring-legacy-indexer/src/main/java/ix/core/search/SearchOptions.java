package ix.core.search;

import static ix.core.search.ArgumentAdapter.doNothing;
import static ix.core.search.ArgumentAdapter.ofBoolean;
import static ix.core.search.ArgumentAdapter.ofInteger;
import static ix.core.search.ArgumentAdapter.ofList;
import static ix.core.search.ArgumentAdapter.ofSingleString;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import gov.nih.ncats.common.stream.StreamUtil;
import gov.nih.ncats.common.util.CachedSupplier;
import gov.nih.ncats.common.util.TimeUtil;
import ix.core.controllers.RequestOptions;
import ix.core.util.EntityUtils;
import ix.core.util.EntityUtils.EntityInfo;
import ix.utils.Util;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class SearchOptions implements RequestOptions {
	public static class SearchTermFilter {
		String field;
		String term;

		public SearchTermFilter(String field, String term) {
			this.field = field;
			this.term = term;
		}

		public String getField() {
			return this.field;
		}

		public String getTerm() {
			return this.term;
		}
	}

	public static class FacetLongRange {
		public String field;
		public Map<String, long[]> range = new LinkedHashMap<>();

		public FacetLongRange(String field) {
			this.field = field;
		}

		public void add(String title, long[] range) {
			this.range.put(title, range);
		}
	}

	public static final int DEFAULT_TOP = 10;
	public static final int DEFAULT_FDIM = 10;
	public static final int DEFAULT_FSKIP = 0;
	public static final String DEFAULT_FFILTER = "";

	// default number of elements to fetch while blocking
	public static final int DEFAULT_FETCH_SIZE = 100; // 0 means all
	
	// default max fetch size of user list facets
	public static final int USER_LIST_FACET_FETCH_SIZE = 10000;

	private Class<?> kind; // filter by type

	private int top = DEFAULT_TOP;
	private int skip;
	private int fetch = DEFAULT_FETCH_SIZE;
	private int fdim = DEFAULT_FDIM; // facet dimension
	private int userListFetchSize = USER_LIST_FACET_FETCH_SIZE;
	private int fskip=DEFAULT_FSKIP;
	
	private int qTop = DEFAULT_TOP;
	private int qSkip;
	private boolean bulkSearchOnIdentifiers = true;
	
    private String ffilter=DEFAULT_FFILTER;
	
	
    private String defaultField=null;


    // whether drilldown (false) or sideway (true)
	private boolean sideway = true;
	
	private boolean wait = false;
	
	//TODO: I don't think this is used anymore
	private String filter;
	
	private String userName;


	/**
	 * Facet is of the form: DIMENSION/VALUE...
	 */
	private List<String> facets = new ArrayList<String>();
	
	/**
	 * Facet names for ad-hoc ranges
	 */
	private List<String> lfacets = new ArrayList<String>();
	
	private List<FacetLongRange> longRangeFacets = new ArrayList<FacetLongRange>();
	private List<String> order = new ArrayList<String>();
	private List<String> expand = new ArrayList<String>();
	private List<SearchTermFilter> termFilters = new ArrayList<SearchTermFilter>();
	
    private boolean includeFacets = true;
    private boolean includeBreakdown = true;
    private boolean promoteSpecialMatches = true;

	public SearchOptions() {
	}

	public SearchOptions(Class<?> kind) {
		this.setKind(kind);
	}

	public SearchOptions(Class<?> kind, int top, int skip, int fdim) {
		this.setKind(kind);
		this.setTop(Math.max(1, top));
		this.skip = Math.max(0, skip);
		this.setFdim(Math.max(1, fdim));
	}

	public SearchOptions(Map<String, String[]> params) {
		parse(params);
	}

	public void setFacet(String facet, String value) {
		facets.clear();
		addFacet(facet, value);
	}

	public void addFacet(String facet, String value) {
		facets.add(facet + "/" + value);
		queryParams.resetCache();
	}

	public void addFacet(String facetString) {
		facets.add(facetString);
		queryParams.resetCache();
	}

	public int max() {
		return skip + getTop();
	}
	
	
	
	public Map<String,String[]> asQueryParams(){
		return queryParams.get();
	}
	
	private CachedSupplier<Map<String, ArgumentAdapter>> argumentAdapters = CachedSupplier.of(()->{
     	return Stream.of(
		     	ofList("order", a->setOrder(a), ()->getOrder()),
		     	ofList("expand", a->expand=a, ()->expand),
		     	ofList("facet", a->facets=a, ()->facets),
		     	ofList("termfilter", rq->{
		     	    termFilters=rq.stream()
		     	                .map(f->new SearchTermFilter(f.split(":")[0],f.split(":")[1]))
		     	                .collect(Collectors.toList());
		     	
		     	}, ()->{
		     	    return termFilters.stream()
		     	                  .map(tf->tf.getField() + ":" + tf.getTerm())
		     	                  .collect(Collectors.toList());
		     	}),
		     	ofInteger("top", a->setTop(a), ()->getTop()),
		     	ofInteger("skip", a->skip=a, ()->skip),
		     	ofInteger("qTop", a->setQTop(a), ()->getQTop()),
		     	ofInteger("qSkip", a->qSkip=a, ()->qSkip),
		     	ofInteger("fskip", a->fskip=a, ()->fskip),
		     	ofInteger("fdim", a->setFdim(a), ()->getFdim()),
		     	ofInteger("fetch", a->setFetch(a), ()->getFetch()),
		     	ofBoolean("sideway", a->setSideway(a), ()->isSideway(),true),
		     	ofBoolean("wait", a->setWait(a), ()->isWait(),false),
		     	ofSingleString("ffilter", a->ffilter=a, ()->ffilter),
		     	ofSingleString("defaultField", a->defaultField=a, ()->defaultField),
		     	ofSingleString("filter", a->filter=a, ()->filter),
		     	ofSingleString("kind", a->{
		     		try{
		     			setKind(SearchOptions.class.getClassLoader().loadClass(a));
		     		}catch(Exception e){
		     			log.error("Unknown kind:" + a, e);
		     		}
		     	}, ()->{
		     		if(getKind()==null)return null;
		     		return getKind().getName();	
		     	}),
		     	ofBoolean("includeFacets", a->setIncludeFacets(a), ()->includeFacets, true),
                ofBoolean("includeBreakdown", a->setIncludeBreakdown(a), ()->includeBreakdown, true),
                ofBoolean("promoteSpecialMatches", a->setPromoteSpecialMatches(a), ()->promoteSpecialMatches, true),
                ofBoolean("simpleSearchOnly", a->setSimpleSearchOnly(a),
                        ()->!(includeFacets||includeBreakdown||promoteSpecialMatches), false)
		     	
     	)
     	.collect(Collectors.toMap(a->a.name(), a->a));
     });
	
	private CachedSupplier<Map<String,String[]>> queryParams = CachedSupplier.of(()->{
		return argumentAdapters.get()
		.values()
		.stream()
		.collect(Collectors.toMap(a->a.name(), a->a.get()));
	});
	
	public SearchOptions parse(Map<String, String[]> params) {
		params.forEach((k,v)->{
			argumentAdapters.get()
					.getOrDefault(k, doNothing())
					.accept(v);
		});
		queryParams.resetCache();
		return this;
	}

	public void removeAndConsumeRangeFilters(BiConsumer<String, long[]> cons) {
		//Map<String, List<Filter>> filters = new HashMap<String, List<Filter>>();
		List<String> remove = new ArrayList<String>();
		for (String f : this.facets) {
			int pos = f.indexOf('/');
			if (pos > 0) {
				String facet = f.substring(0, pos);
				String value = f.substring(pos + 1);
				// options.longRangeFacets.stream()
				for (SearchOptions.FacetLongRange flr : this.getLongRangeFacets()) {
					if (facet.equals(flr.field)) {
						long[] range = flr.range.get(value);
						if (range != null) {
							// add this as filter..
							cons.accept(facet, range);
						}
						remove.add(f);
					}
				}
			}
		}
		this.lfacets=remove;
		this.facets.removeAll(remove);
	}

	public List<String> getFacets() {
		return this.facets;
	}

	public String toString() {
		StringBuilder sb = new StringBuilder("SearchOptions{kind=" + (getKind() != null ? getKind().getName() : "") + ",top="
	 + getTop() + ",skip=" + skip + ",fdim=" + getFdim() + ",fetch=" + getFetch() + ",sideway=" + isSideway() + ",filter="
	 + filter + ",facets={");
		for (Iterator<String> it = facets.iterator(); it.hasNext();) {
			sb.append(it.next());
			if (it.hasNext())
				sb.append(",");
		}
		sb.append("},order={");
		for (Iterator<String> it = getOrder().iterator(); it.hasNext();) {
			sb.append(it.next());
			if (it.hasNext())
				sb.append(",");
		}
		sb.append("},expand={");
		for (Iterator<String> it = expand.iterator(); it.hasNext();) {
			sb.append(it.next());
			if (it.hasNext())
				sb.append(",");
		}
		sb.append("}}");
		return sb.toString();
	}

	private DrillAndPath parseDrillAndPath(String dd) {
		return DrillAndPath.fromRaw(dd);
	}

	public Map<String, List<DrillAndPath>> getDrillDownsMap() {
        // parses to a map, though it doesn't NEED to be a map,
        // it could just as easily be a flat list
        Map<String, List<DrillAndPath>> providedDrills = StreamUtil.with(this.facets.stream())
                                                                   .and(this.lfacets.stream())
                                                                   .stream()
                            .map(dd -> parseDrillAndPath(dd))
                            .filter(Objects::nonNull)
                            .collect(Collectors.groupingBy(d->d.getParentDrillName()));
        return providedDrills;
    }

    public Map<String, List<DrillAndPath>> getDrillDownsMapExcludingRanges() {
        Map<String, List<DrillAndPath>> providedDrills = StreamUtil.with(this.facets.stream())
                                                                   //.and(this.lfacets.stream())
                                                                   .stream()
                            .map(dd -> parseDrillAndPath(dd))
                            .filter(Objects::nonNull)
                            .collect(Collectors.groupingBy(d->d.getParentDrillName()));
        return providedDrills;
    }

	/**
	 * Join facets together with special flags for NOT and AND
	 * @return
	 */
	public Map<String, List<DrillAndPath>> getEnhancedDrillDownsMap() {
		// parses to a map, though it doesn't NEED to be a map,
	    // it could just as easily be a flat list
		Map<String, List<DrillAndPath>> providedDrills =this.facets
		                    .stream()
		                    .map(dd -> parseDrillAndPath(dd))
		                    .filter(Objects::nonNull)

		                    .collect(Collectors.groupingBy(d->d.getParentDrillName()));
		return providedDrills;
	}

	public static class DrillAndPath {
		private String drill;
		private String[] paths;

		public DrillAndPath(String drill, String[] path) {
			this.drill = drill;
			this.paths = path;
		}

		public String asLabel() {
			return String.join("/", getPaths());
		}

		/**
		 * Returns the selected facet as it would be specified in a URL
		 * @return
		 */
		public String asEncoded(){
			return Util.encodeFacetComponent(this.getDrill()) + "/"
		         + Util.encodeFacetComponent(this.asLabel());
		}


		public String getDrill() {
			return drill;
		}

		public String getParentDrillName(){
			String edrill = getDrill();
			if(edrill.startsWith("!") || edrill.startsWith("^")){
				return edrill.substring(1);
			}
			return edrill;
		}

		public String getPrefix(){
			String edrill = getDrill();
			if(edrill.startsWith("!") || edrill.startsWith("^")){
				return edrill.substring(0, 1);
			}
			return "";
		}


		public String[] getPaths() {
			return paths;
		}

		public static DrillAndPath fromRaw(String dd){
				int pos = dd.indexOf('/');
				if (pos > 0) {
					String facet = dd.substring(0, pos);
					String value = dd.substring(pos + 1);
					String[] drill = value.split("/");
					for (int i = 0; i < drill.length; i++) {
						drill[i] = Util.decodeFacetComponent(drill[i]);
					}
					return new SearchOptions.DrillAndPath(facet, drill);
				}
				return null;
		}

	}

	public EntityInfo<?> getKindInfo() {
		try {
			return EntityUtils.getEntityInfoFor(this.getKind());
		} catch (Exception e) {
			return null;
		}
	}

	@Override
	public int getTop() {
		return this.top;
	}

	@Override
	public int getSkip() {
		return this.skip;
	}
	
	public int getQTop() {
		return this.qTop;
	}

	
	public int getQSkip() {
		return this.qSkip;
	}
	
	public boolean getBulkSearchOnIdentifiers() {
		return bulkSearchOnIdentifiers;
	}

	//TODO I don't think this is ever used anymore
	@Override
	public String getFilter() {
		return this.filter;
	}

	public static class Builder {
		
		private Class<?> kind;
		private int top=DEFAULT_TOP;
		private int skip=0;
		private int qTop=DEFAULT_TOP;
		private int qSkip=0;		
		private boolean bulkSearchOnIdentifiers;
		private int fetch=DEFAULT_FETCH_SIZE;
		private int fdim=DEFAULT_FDIM;
		private boolean sideway=true;

        private boolean includeFacets=true;
        private boolean includeBreakdown=true;
        private boolean promoteSpecialMatches =true;
		
        private String defaultField;
		private String filter;
		
		private List<String> facets = new ArrayList<>();
		private List<FacetLongRange> longRangeFacets = new ArrayList<>();
		private List<String> order = new ArrayList<>();
		private List<String> expand = new ArrayList<>();
		private List<SearchTermFilter> termFilters = new ArrayList<>();
		
		private Map<String,String[]> params;
        private int fskip;
        private String ffilter;
		
		/**
		 * Creates a clone of the given {@link SearchOptions}, clobbering any previous
		 * calls to {@link #top(int)}, {@link #skip(int)} or any other option 
		 * creating method. The only methods not clobbered by this are
		 * {@link #withParameters(Map)}
		 * which are applied after the creation.
		 * @param so
		 * @return
		 */
		public Builder from(SearchOptions so) {
			top(so.getTop());
			skip(so.skip);
			qTop(so.getQTop());
			qSkip(so.getQSkip());
			bulkSearchOnIdentifiers(so.getBulkSearchOnIdentifiers());
			fetch(so.getFetch());
			fdim(so.getFdim());
			sideway(so.isSideway());
			filter(so.filter);
			kind(so.getKind());
			facets(new ArrayList<String>(so.getFacets()));
			order(new ArrayList<String>(so.getOrder()));
			expand(new ArrayList<String>(so.expand));
			termFilters(new ArrayList<SearchTermFilter>(so.termFilters));
			longRangeFacets(new ArrayList<FacetLongRange>(so.longRangeFacets));
			includeFacets(so.getIncludeFacets());
			includeBreakdown(so.getIncludeBreakdown());
			promoteSpecialMatches(so.getPromoteSpecialMatches());
			defaultField(so.defaultField);
			return this;
		}

		public Builder kind(Class<?> kind) {
			this.kind = kind;
			return this;
		}

		public Builder top(int top) {
			this.top = top;
			return this;
		}

		public Builder skip(int skip) {
			this.skip = skip;
			return this;
		}
		
		public Builder qTop(int qTop) {
			this.qTop = qTop;
			return this;
		}

		public Builder qSkip(int qSkip) {
			this.qSkip = qSkip;
			return this;
		}

		public Builder bulkSearchOnIdentifiers(boolean on) {
			this.bulkSearchOnIdentifiers = on;
			return this;
		}
		
		public Builder fetch(int fetch) {
			this.fetch = fetch;
			return this;
		}

		public Builder fdim(int fdim) {
			this.fdim = fdim;
			return this;
		}
		
		public Builder fskip(int fskip) {
            this.fskip = fskip;
            return this;
        }
		
		public Builder ffilter(String ffilter) {
            this.ffilter = ffilter;
            return this;
        }
		
		public Builder defaultField(String defaultField) {
            this.defaultField = defaultField;
            return this;
        }
        
		

		public Builder sideway(boolean sideway) {
			this.sideway = sideway;
			return this;
		}

		public Builder filter(String filter) {
			this.filter = filter;
			return this;
		}

		public Builder facets(List<String> facets) {
			this.facets = facets;
			return this;
		}
		public Builder facets(String ... facets) {
			this.facets = Arrays.asList(facets);
			return this;
		}
		
		public Builder addfacet(String facetName, String facetValue) {
			
			String toAdd = Util.facetEncode(facetName, facetValue);
			
			if(this.facets==null)this.facets= new ArrayList<String>();
			this.facets.add(toAdd);
			
			return this;
		}

		public Builder longRangeFacets(List<FacetLongRange> longRangeFacets) {
			this.longRangeFacets = longRangeFacets;
			return this;
		}
		
		public Builder addDateRangeFacet(String facetName) {
		    if(this.longRangeFacets==null) {
		        this.longRangeFacets=new ArrayList<>();
		    }
            this.longRangeFacets.add(asDateFacet(facetName));
            return this;
        }

		public Builder order(List<String> order) {
			this.order = order;
			return this;
		}
		public Builder addOrder(String orderBy) {
            if(this.order==null) {
                this.order=new ArrayList<>();
            }
            this.order.add(orderBy);
            return this;
        }

		public Builder expand(List<String> expand) {
			this.expand = expand;
			return this;
		}

		public Builder termFilters(List<SearchTermFilter> termFilters) {
			this.termFilters = termFilters;
			return this;
		}
		//TODO anywhere we need to get this from a request we can just ask the request for it's param map
//		public Builder withRequest(HttpServletRequest req) {
//			return withParameters(req.queryString());
//		}
		
		public Builder withParameters(Map<String,String[]> params) {
			this.params=params;
			return this;
		}
	    public Builder includeFacets(boolean f) {
            this.includeFacets=f;
            return this;
        }
        public Builder includeBreakdown(boolean b) {
            this.includeBreakdown=b;
            return this;
        }

        public Builder promoteSpecialMatches(boolean p) {
            this.promoteSpecialMatches=p;
            return this;
        }

        public Builder simpleSearchOnly(boolean s) {
            this.includeFacets=!s;
            this.includeBreakdown=!s;
            this.promoteSpecialMatches=!s;
            return this;
        }
		public SearchOptions build() {
			return new SearchOptions(this);
		}
	}

	private SearchOptions(Builder builder) {
		this.setKind(builder.kind);
		this.setTop(builder.top);
		this.skip = builder.skip;
		this.qTop = builder.qTop;
		this.qSkip = builder.qSkip;
		this.bulkSearchOnIdentifiers = builder.bulkSearchOnIdentifiers;
		this.setFetch(builder.fetch);
		this.setFdim(builder.fdim);
		this.setSideway(builder.sideway);
		this.filter = builder.filter;
		this.facets = builder.facets;
		this.ffilter= builder.ffilter;
		this.fskip  = builder.fskip;
		this.setLongRangeFacets(builder.longRangeFacets);
		this.setOrder(builder.order);
		this.expand = builder.expand;
		this.termFilters = builder.termFilters;
		
		this.includeBreakdown=builder.includeBreakdown;
		this.includeFacets=builder.includeFacets;
		this.promoteSpecialMatches=builder.promoteSpecialMatches;
		
		
		
		if(builder.params!=null){
		    this.parse(builder.params);
		}
	}

	public List<SearchTermFilter> getTermFilters() {
		return termFilters;
	}
	
	public void addTermFilters(List<SearchTermFilter> termFilters) {
        this.termFilters.addAll(termFilters);
    }
	public void addTermFilter(SearchTermFilter termFilter) {
        this.termFilters.add(termFilter);
    }
	public void addTermFilter(String field, String value) {
	    addTermFilter(new SearchTermFilter(field,value));
    }

	public List<String> getOrder() {
		return order;
	}

	public List<String> setOrder(List<String> order) {
		this.order = order;
		queryParams.resetCache();
		return order;
	}

	public List<FacetLongRange> getLongRangeFacets() {
		return longRangeFacets;
	}
	public void addLongRangeFacets(List<FacetLongRange> longRangeFacets) {
        this.longRangeFacets.addAll(longRangeFacets);
    }
	
	public void addDateRangeFacet(String facetName) {
        if(this.longRangeFacets==null) {
            this.longRangeFacets=new ArrayList<>();
        }
        this.longRangeFacets.add(asDateFacet(facetName));
    }

	public void setLongRangeFacets(List<FacetLongRange> longRangeFacets) {
		this.longRangeFacets = longRangeFacets;
		queryParams.resetCache();
	}

	public boolean isSideway() {
		return sideway;
	}

	public boolean setSideway(boolean sideway) {
		this.sideway = sideway;
		queryParams.resetCache();
		return sideway;
	}

	public Class<?> getKind() {
		return kind;
	}

	public void setKind(Class<?> kind) {
		this.kind = kind;
		queryParams.resetCache();
	}

	public int getFdim() {
		return fdim;
	}

	public int setFdim(int fdim) {
		this.fdim = fdim;
		queryParams.resetCache();
		return fdim;
	}

	public int getFetch() {
		return fetch;
	}

	public void setFetch(int fetch) {
		this.fetch = fetch;
		queryParams.resetCache();
	}
	
	public int getUserListFetchSize() {
		return userListFetchSize;
	}
	
	public void setFetchAll(){
	    setFetch(-1);
	}
	

    public int getFskip() {
        return fskip;
    }

    public void setFskip(int fskip) {
        this.fskip = fskip;
        queryParams.resetCache();
    }
    
    public String getFfilter() {
        return ffilter;
    }

    public void setFfilter(String ffilter) {
        this.ffilter = ffilter;
    }

	public int setTop(int top) {
		this.top = top;
		queryParams.resetCache();
		return top;
	}
	
	public int setSkip(int skip) {
		this.skip = skip;
		queryParams.resetCache();
		return skip;
	}
	
	public int setQTop(int qTop) {
		this.qTop = qTop;
		queryParams.resetCache();
		return top;
	}
	
	public int setQSkip(int qSkip) {
		this.qSkip = qSkip;
		queryParams.resetCache();
		return skip;
	}
	
	public void setBulkSearchOnIdentifiers(boolean on) {
		bulkSearchOnIdentifiers = on;
	}

	public boolean isWait() {
		return wait;
	}

	public boolean setWait(boolean wait) {
	    this.wait = wait;
	    return wait;
	}

	public boolean setIncludeFacets(boolean includeThem) {
	    this.includeFacets=includeThem;
	    return includeThem;
	}

	public boolean setIncludeBreakdown(boolean includeBreakdown) {
	    this.includeBreakdown= includeBreakdown;
	    return includeBreakdown;
	}

	public boolean setPromoteSpecialMatches(boolean promoteSpecialMatches) {
	    this.promoteSpecialMatches=promoteSpecialMatches;
	    return promoteSpecialMatches;
	}

	public boolean setSimpleSearchOnly(boolean simpleSearch) {
	    this.promoteSpecialMatches = !simpleSearch;
	    this.includeBreakdown=!simpleSearch;
	    this.includeFacets=!simpleSearch;
	    return simpleSearch;
	}

	public boolean getIncludeFacets() {
	    return this.includeFacets;
	}

	public boolean getIncludeBreakdown() {
	    return  this.includeBreakdown;
	}

	public boolean getPromoteSpecialMatches()  {
	    return this.promoteSpecialMatches;
	}
	
    public String getDefaultField() {
        return this.defaultField;
    }
	 
	private static FacetLongRange asDateFacet(String fname) {
	    return asDateFacet(fname, getDefaultDateOrderMap());
	}
	private static FacetLongRange asDateFacet(String fname,Map<String, Function<LocalDateTime, LocalDateTime>> orderedMap) {
	    FacetLongRange facet= new SearchOptions.FacetLongRange(fname);
        LocalDateTime now = TimeUtil.getCurrentLocalDateTime();

        // 1 second in future
        long end = TimeUtil.toMillis(now) + 1000L;
        LocalDateTime last = now;
        for (Map.Entry<String, Function<LocalDateTime, LocalDateTime>> entry : orderedMap.entrySet()) {
            String name = entry.getKey();
            LocalDateTime startDate = entry.getValue().apply(now);
            long start = TimeUtil.toMillis(startDate);
            // This is terrible, and I hate it, but this is a
            // quick fix for the calendar problem.
            if (start > end) {
                startDate = entry.getValue().apply(last);
                start = TimeUtil.toMillis(startDate);
            }
            long[] range = new long[] { start, end };
            facet.add(name, range);

            end = start;
            last = startDate;
        }
        return facet;
	}

	
	//TODO: this should be refactored to be in a utility class with a builder and default values
	// which can be set in a config.
    private static Map<String, Function<LocalDateTime, LocalDateTime>> getDefaultDateOrderMap(){

        // Note, this is not really the right terminology.
        // durations of time are better than actual cal. references,
        // as they don't behave quite as well, and may cause more confusion
        // due to their non-overlapping nature. This makes it easier
        // to have a bug, and harder for a user to understand.

        Map<String, Function<LocalDateTime, LocalDateTime>> map = new LinkedHashMap<>();
        map.put("Today", now -> LocalDateTime.of(now.toLocalDate(), LocalTime.MIDNIGHT));

        // (Last 7 days)
        map.put("This week", now -> {
            return now.minusDays(7);
            // TemporalField dayOfWeek = weekFields.dayOfWeek();
            // return LocalDateTime.of(now.toLocalDate(), LocalTime.MIDNIGHT)
            // .with(dayOfWeek, 1);
        });

        // (Last 30 days)
        map.put("This month", now -> {
            return now.minusDays(30);
            // LocalDateTime ldt=LocalDateTime.of(now.toLocalDate(),
            // LocalTime.MIDNIGHT)
            // .withDayOfMonth(1);
            // return ldt;
        });

        // (Last 6 months)
        map.put("Past 6 months", now -> {
            return now.minusMonths(6);
            // LocalDateTime.of(now.toLocalDate(), LocalTime.MIDNIGHT)
            // .minusMonths(6)
        });

        // (Last 1 year)
        map.put("Past 1 year", now -> {
            return now.minusYears(1);
            // return LocalDateTime.of(now.toLocalDate(), LocalTime.MIDNIGHT)
            // .minusYears(1);
        });

        map.put("Past 2 years", now -> {
            return now.minusYears(2);
            // return LocalDateTime.of(now.toLocalDate(), LocalTime.MIDNIGHT)
            // .minusYears(2);
        });

        // Older than 2 Years
        map.put("Older than 2 years", now -> now.minusYears(6000));
        return map;
    }

    public void setDefaultField(String defaultField) {
        this.defaultField=defaultField;
    }
}
