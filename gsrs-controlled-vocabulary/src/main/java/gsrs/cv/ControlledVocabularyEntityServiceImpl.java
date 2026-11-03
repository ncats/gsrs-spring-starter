package gsrs.cv;

import org.springframework.beans.factory.annotation.Qualifier;
import tools.jackson.databind.JsonNode;
import gsrs.CvUtils;
import gsrs.controller.IdHelpers;
import gsrs.cv.events.CvCreatedEvent;
import gsrs.cv.events.CvUpdatedEvent;
import gsrs.events.AbstractEntityCreatedEvent;
import gsrs.events.AbstractEntityUpdatedEvent;
import gsrs.repository.ControlledVocabularyRepository;
import gsrs.security.canManageCVs;
import gsrs.service.AbstractGsrsEntityService;
import ix.core.util.EntityUtils.Key;
import ix.ginas.models.v1.ControlledVocabulary;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.context.annotation.ScopedProxyMode;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.json.JsonMapper;

import java.io.IOException;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Scope(proxyMode = ScopedProxyMode.INTERFACES)
@Service
public class ControlledVocabularyEntityServiceImpl extends AbstractGsrsEntityService<ControlledVocabulary, Long> implements ControlledVocabularyEntityService {
    public static final String  CONTEXT = "vocabularies";


    public ControlledVocabularyEntityServiceImpl() {
        super(CONTEXT,  IdHelpers.NUMBER,
                null,null,null);
    }

    @Autowired
    private ControlledVocabularyRepository repository;

    @Autowired
    @Qualifier("legacyJsonMapper")
    private JsonMapper jsonMapper;

    @Override
    public Class<ControlledVocabulary> getEntityClass() {
        return ControlledVocabulary.class;
    }

    @Override
    public Long parseIdFromString(String idAsString) {
        return Long.parseLong(idAsString);
    }

    @canManageCVs
    @Override
    protected ControlledVocabulary fromNewJson(JsonNode json) throws IOException {
        return CvUtils.adaptSingleRecord(json, jsonMapper, true);

    }

    @Override
    public Page page(Pageable pageable) {

        return repository.findAll(pageable);
    }

    @canManageCVs
    @Override
    @Transactional
    public void delete(Long id) {
        repository.deleteById(id);
    }

    @canManageCVs
    @Override
    @Transactional
    protected ControlledVocabulary update(ControlledVocabulary controlledVocabulary) {
        return repository.saveAndFlush(controlledVocabulary);
    }

    @Override
    protected AbstractEntityUpdatedEvent<ControlledVocabulary> newUpdateEvent(ControlledVocabulary updatedEntity) {
        return new CvUpdatedEvent(updatedEntity);
    }

    @canManageCVs
    @Override
    protected AbstractEntityCreatedEvent<ControlledVocabulary> newCreationEvent(ControlledVocabulary createdEntity) {
        return new CvCreatedEvent(createdEntity);
    }

    @Override
    public Long getIdFrom(ControlledVocabulary entity) {
        return entity.getId();
    }

    @Override
    protected List<ControlledVocabulary> fromNewJsonList(JsonNode list) throws IOException {
        return CvUtils.adaptList(list, jsonMapper, true);
    }

    @Override
    protected ControlledVocabulary fromUpdatedJson(JsonNode json) throws IOException {
        return CvUtils.adaptSingleRecord(json, jsonMapper, false);
    }

    @Override
    protected List<ControlledVocabulary> fromUpdatedJsonList(JsonNode list) throws IOException {
        return CvUtils.adaptList(list, jsonMapper, false);
    }


    @Override
    protected JsonNode toJson(ControlledVocabulary controlledVocabulary) throws IOException {
        return jsonMapper.valueToTree(controlledVocabulary);
    }

    @Override
    @Transactional
    protected ControlledVocabulary create(ControlledVocabulary controlledVocabulary) {
        try {
            return repository.saveAndFlush(controlledVocabulary);
        }catch(Throwable t){
            t.printStackTrace();
            throw t;
        }
    }

    @Override
    public long count() {
        return repository.count();
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<ControlledVocabulary> get(Long id) {
        return repository.findById(id);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<ControlledVocabulary> flexLookup(String someKindOfId) {
        //is the string a domain?
        List<ControlledVocabulary> list = repository.findByDomain(someKindOfId);
        if(list.isEmpty()){
            return Optional.empty();
        }
        //get first one?
        return Optional.ofNullable(list.get(0));
    }

    @Override
    protected Optional<Long> flexLookupIdOnly(String someKindOfId) {
        //easiest way to avoid deduping data is to just do a full flex lookup and then return id
        Optional<ControlledVocabulary> found = flexLookup(someKindOfId);
        if(found.isPresent()){
            return Optional.of(found.get().getId());
        }
        return Optional.empty();
    }

	@Override
	public List<Long> getIDs() {
		return repository.getAllIDs();
	}


//    private SearchResult<ControlledVocabulary> parseQueryIntoMatch(String query, SearchSession session) {
//        Pattern pattern = Pattern.compile("(\\S+):(\\S+)");
//        Matcher matcher = pattern.matcher(query);
//
//        Map<String, List<String>> map = new LinkedHashMap<>();
//        while (matcher.find()) {
//            map.computeIfAbsent(matcher.group(1), k -> new ArrayList<>()).add(matcher.group(2));
//
//        }
//        if (map.isEmpty()) {
//            return session.search(ControlledVocabulary.class).where(f -> f.matchAll()).fetchAll();
//        }
//        if (map.size() == 1) {
//            Map.Entry<String, List<String>> entry = map.entrySet().iterator().next();
//            if (entry.getValue().size() == 1) {
//                //simpliest case
//                return session.search(ControlledVocabulary.class)
//                        .where(f -> f.match().field(entry.getKey())
//                                .matching(entry.getValue().get(0))
//
//                        ).fetchAll();
//            }
//
//            return session.search(ControlledVocabulary.class).where(f -> {
//                        BooleanPredicateClausesStep<?> step = f.bool();
//                        Iterator<String> values = entry.getValue().iterator();
//                        while (values.hasNext()) {
//                            step = step.should(f.match().field(entry.getKey())
//                                    .matching(values.next()));
//                        }
//
//                        return step;
//                    }
//            ).fetchAll();
//
//        }else{
//            //more complicated version probably need to make an AST
//            return null;
//        }
//
//
//
//    }

//    @Override
//    protected List<ControlledVocabulary> indexSearchV2(LuceneSearchRequestOp op, Optional<Integer> top, Optional<Integer> skip, Optional<Integer> fdim) {
//        SearchSession session = searchService.createSearchSession();
//
//        return session.search(ControlledVocabulary.class)
//                .where(f-> op.doIt(f))
//                .fetchHits(skip.orElse(null),top.orElse(null));
//
//    }

//    @Override
//    protected SearchResult indexSearchV1(SearchRequest searchRequest) throws Exception{
//
//            return getlegacyGsrsSearchService().search(searchRequest.getQuery(), searchRequest.getOptions() );
//

    //        SearchSession session = searchService.createSearchSession();
//        List<ControlledVocabulary> dslHits = parseQueryIntoMatch(query , session).hits();
//
//
//       System.out.println("dslHits = " + dslHits);
//        return dslHits;
//        String[] fields = parseFieldsFrom(query);
//        QueryParser parser;
//        if(fields.length==1){
//            parser = new QueryParser(fields[0], new KeywordAnalyzer());
//        }else{
//            parser = new MultiFieldQueryParser(fields, new KeywordAnalyzer());
//        }
//        System.out.println("parsed fields =" + Arrays.toString(fields));

//        QueryParser parser = new IxQueryParser(query);
//        List<ControlledVocabulary> hits = session.search( ControlledVocabulary.class )
//                .extension( LuceneExtension.get() )
//                .where( f -> {
//                    try {
//                        return f.fromLuceneQuery( parser.parse(query) );
//                    } catch (ParseException e) {
//                        return Sneak.sneakyThrow(new RuntimeException(e));
//                    }
//                })
//                .fetchHits(skip.orElse(null), top.orElse(null));

//        System.out.println("found # hits = " + hits.size());
//        return hits;

//    }

    @Override
	public List<Key> getKeys() {
		List<Long> IDs = repository.getAllIds();
		List<Key> keys = IDs.stream().map(id->Key.ofStringId(ControlledVocabulary.class, Long.toString(id))).collect(Collectors.toList());
		return keys;
	}

}
