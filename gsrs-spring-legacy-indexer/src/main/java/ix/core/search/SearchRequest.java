package ix.core.search;

import ix.core.search.SearchOptions.FacetLongRange;
import ix.core.search.SearchOptions.SearchTermFilter;
import ix.core.search.text.TextIndexer;
import ix.utils.Util;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.search.Query;

import jakarta.servlet.http.HttpServletRequest;
import java.util.*;

public class SearchRequest {
	private SearchOptions options;
	private final Collection<?> subset;
	private String query;
	
	public SearchOptions getOptions() {
		return options;
	}

	public void setOptions(SearchOptions options) {
		this.options = options;
	}

	public Collection<?> getSubset() {
		return subset;
	}


	public String getQuery() {
		return query;
	}

	public void setQuery(String query) {
		this.query = query;
	}


	public static class Builder {
		private Collection<?> subset;
		private String query = "*:*"; //defaults to "all" query
		private SearchOptions.Builder opBuilder = new SearchOptions.Builder();
		
		public Builder kind(Class<?> kind) {
			opBuilder.kind(kind);
			return this;
		}

		public Builder top(int top) {
			opBuilder.top(top);
			return this;
		}

		public Builder skip(int skip) {
			opBuilder.skip(skip);
			return this;
		}
		
		public Builder qTop(int qTop) {
			opBuilder.qTop(qTop);
			return this;
		}

		public Builder qSkip(int qSkip) {
			opBuilder.qSkip(qSkip);
			return this;
		}
		
		public Builder bulkSearchOnIdentifiers(boolean on) {
			opBuilder.bulkSearchOnIdentifiers(on);
			return this;
		}

		public Builder fetch(int fetch) {
			opBuilder.fetch(fetch);
			return this;
		}

		public Builder fdim(int fdim) {
			opBuilder.fdim(fdim);
			return this;
		}

		public Builder sideway(boolean sideway) {
			opBuilder.sideway(sideway);
			return this;
		}

		public Builder filter(String filter) {
			opBuilder.filter(filter);
			return this;
		}

		public Builder facets(List<String> facets) {
			opBuilder.facets(facets);
			return this;
		}
		
		public Builder addFacet(String facetName, String facetValue){
			this.opBuilder.addfacet(facetName, facetValue);
			return this;
			
		}
		

		public Builder longRangeFacets(List<FacetLongRange> longRangeFacets) {
			opBuilder.longRangeFacets(longRangeFacets);
			return this;
		}

		public Builder addDateRangeFacet(String facetName) {
		    opBuilder.addDateRangeFacet(facetName);
		    return this;
		}

		public Builder order(List<String> order) {
			opBuilder.order(order);
			return this;
		}
		public Builder addOrder(String orderBy) {
		    opBuilder.addOrder(orderBy);
		    return this;
		}

		public Builder expand(List<String> expand) {
			opBuilder.expand(expand);
			return this;
		}

		public Builder termFilters(List<SearchTermFilter> termFilters) {
			opBuilder.termFilters(termFilters);
			return this;
		}

		public Builder withRequest(HttpServletRequest req) {
			return withParameters(req.getParameterMap());
		}

		public Builder withParameters(Map<String, String[]> params) {
			opBuilder.withParameters(params);
			return this;
		}

		public Builder options(SearchOptions options) {
			opBuilder.from(options);
			return this;
		}

		public Builder subset(Collection<?> subset) {
			this.subset = subset;
			return this;
		}

		public Builder query(String query) {
			this.query = query;
			return this;
		}


		public Builder includeFacets(boolean f) {
			this.opBuilder.includeFacets(f);
			return this;
		}
		public Builder includeBreakdown(boolean b) {
			this.opBuilder.includeBreakdown(b);
			return this;
		}

		public Builder promoteSpecialMatches(boolean p) {
			this.opBuilder.promoteSpecialMatches(p);
			return this;
		}

		public Builder simpleSearchOnly(boolean s) {
			this.opBuilder.simpleSearchOnly(s);
			return this;
		}

		public SearchRequest build() {
			return new SearchRequest(this);
		}
	}

	private SearchRequest(Builder builder) {
		this.options = builder.opBuilder.build();
		this.subset = builder.subset;
		this.query = builder.query;
	}

	

	public Query extractFullFacetQuery(TextIndexer indexer, String facet) throws ParseException {
		return indexer.extractFullFacetQuery(this.query, this.options, facet);
	}

	
	
	
    public Map<String, String[]> asQueryParams() {
        Map<String,String[]> map = options.asQueryParams();
        map.put("q", new String[]{query});
        return map;
    }
    
    public static enum RequestUniquenessLevel{
        SET("q", "facet", "filter", "kind", "termfilter", "sideway"),
        SET_ORDER("q", "facet", "filter", "kind", "termfilter", "order","sideway"),
        SET_ORDER_STATS("q", "facet", "filter", "kind", "termfilter", "order", "fdim","sideway"),
        SET_ORDER_STATS_PAGE("q", "facet", "filter", "kind", "termfilter", "order", "fdim", "top","skip","expand","sideway");
        
        private String[] params;
        private RequestUniquenessLevel(String ... params){
            this.params=params;
        }
        
        public String[] getParams(){
            return this.params;
        }
        
    }
    
    public String getSha1Hash(RequestUniquenessLevel level){
        int chash = Optional.ofNullable(this.subset).orElse(new HashSet()).hashCode();
        return Util.sha1("search" + chash, asQueryParams(), level.getParams());
    }
    
    
    /**
     * This is a sha1 hash of the pieces of this
     * request which are considered to fundamentally
     * alter the <i>set</i> of results returned.
     * 
     * <p>
     * More specifically, this request will result
     * in a collection of entities. It may also encode 
     * certain post-processing and reporting options.
     * If 2 requests (done before any data changes)
     * would return (and <i>must</i> always return) the same 
     * total <b>set</b> of results, then those two requests 
     * should have the same {@link #getDefiningSetSha1()} 
     * result. Note that a canonical encoding of a request
     * isn't always possible, so there are cases where
     * equivalent requests will receive different sha1
     * hashes.
     * </p>
     * 
     * @return
     */
    public String getDefiningSetSha1(){
        return getSha1Hash(RequestUniquenessLevel.SET);
    }
    
    /**
     * This is a sha1 hash of the pieces of this request which are considered to
     * fundamentally alter the <i>order</i>, <i>set</i>, or <i>statistics</i> 
     * of results returned.
     * 
     * <p>
     * More specifically, this request will result in a collection of entities.
     * It may also encode certain post-processing and reporting options. If 2
     * requests (done before any data changes) would return (and <i>must</i>
     * always return) the same total <b>set</b> and <i>order</i> and 
     * <i>facets, or other statistical meta data</i> of results,
     * then those two requests should have the same
     * {@link #getOrderedSetSha1()} result. Note that a canonical encoding of a
     * request isn't always possible, so there are cases where equivalent
     * requests will receive different sha1 hashes.
     * </p>
     * 
     * @return
     */
    public String getOrderedSetSha1(){
        return getSha1Hash(RequestUniquenessLevel.SET_ORDER_STATS);
    }

//    public SearchResult execute() throws IOException {
//        return SearchFactory.search(this);
//    }
//    public SearchResult execute(TextIndexer t) throws IOException {
//        return SearchFactory.search(t,this);
//    }
}