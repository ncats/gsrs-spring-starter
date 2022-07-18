package gsrs.holdingArea.model;

import gov.nih.ncats.common.Tuple;
import ix.core.util.EntityUtils;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MatchedRecordSummary {
    /*
        new method:
            input: List<MatchableKeyValueTuple>
            output: matched record summary (object). (new class)
                        matched key value summary  (new object)
                            has both original MatchableKeyValueTuple  used for mapping + list of entity.keys (defined in starter)
                            convenience methods:
                                   [get all entity.keys that mapped in more than one way]
                                   get all unique entity.keys that map (at all)
                                   get all entity.keys that map, given a set of matchableKeys (fields)
                                    which have a matchable key found in a set of field names.
     */
    private List<gsrs.holdingArea.model.MatchableKeyValueTuple> query = new ArrayList<>();
    private List<MatchedKeyValue> matches = new ArrayList<>();

    public List<String> getMultiplyMatchedKeys(){

        return query.stream()
                .map(gsrs.holdingArea.model.MatchableKeyValueTuple::getKey)
                .filter(k->matches.stream(). anyMatch(ma->ma.getTupleUsedInMatching().getKey().equals(k)))
                .map(i->{
                    long count= matches.size();
                    return Tuple.of(i, count);
                })
                .filter(t->t.v()>1)
                .map(Tuple::k)
                .collect(Collectors.toList());

    }
    public List<String> getUniqueMatchingKeys(){
        return query.stream()
                .map(gsrs.holdingArea.model.MatchableKeyValueTuple::getKey)
                .distinct()
                .collect(Collectors.toList());
    }

    /*
    Get a list of Entity IDs that were found using a set of Keys within the query
     */
    public List<EntityUtils.Key> getEntitiesMatchingSearchKeys(List<String> searchKeys){
        List<EntityUtils.Key> entityKeys = new ArrayList<>();
        matches.forEach(m->{
            if( searchKeys.contains( m.getTupleUsedInMatching().getKey())) {
                m.getMatchingRecords().stream().forEach(k->entityKeys.add( k.getRecordId()));
            }
        });
        return entityKeys;
    }
}
