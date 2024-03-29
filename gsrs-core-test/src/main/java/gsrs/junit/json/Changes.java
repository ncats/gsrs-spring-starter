package gsrs.junit.json;


import com.fasterxml.jackson.core.JsonPointer;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.flipkart.zjsonpatch.JsonDiff;
import com.flipkart.zjsonpatch.JsonPatch;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by katzelda on 2/26/16.
 */
public class Changes {

    private final Map<String, Change> changes;

    public Changes(Map<String, Change> changes) {
        Objects.requireNonNull(changes);
        this.changes = changes;
    }

    public boolean isEmpty(){
        return changes.isEmpty();
    }

    public Iterable<Change> getAllChanges(){
        return changes.values();
    }

    public Iterable<Change> getChangesByKey(String regex){
        return getChangesByKey(Pattern.compile(regex));
    }

    public Change get(String key){
        return changes.get(key);
    }

    public Iterable<Change> getChangesByKey(Pattern pattern){
        List<Change> list = new ArrayList<>();
        for(Change change : changes.values()){
            Matcher matcher = pattern.matcher(change.getKey());
            if(matcher.find()){
                list.add(change);
            }
        }
        return list;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Changes changes1 = (Changes) o;

        return changes.equals(changes1.changes);

    }


    @Override
    public int hashCode() {
        return changes.hashCode();
    }

    public Iterable<Change> getChangesByType(Change.ChangeType type) {
        Objects.requireNonNull(type);

        List<Change> list = new ArrayList<>();
        for(Change c : changes.values()){
            if(type == c.getType()){
                list.add(c);
            }
        }
        return list;
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder(2000);
        builder.append("Changes{\n");
        for(Map.Entry<String, Change> c : changes.entrySet()){
            builder.append("\t").append(c).append('\n');
        }
        return builder.append("}").toString();

    }

    public Changes missingFrom(Changes other) {
        Set<Change> copy = new HashSet<>(this.changes.values());
        copy.removeAll(other.changes.values());

        return toChanges(copy);
    }

    public void assertMatches(JsonNode old, JsonNode newVersion){

        JsonNode patch = JsonDiff.asJson(old, newVersion);

        JsonNode ourVersion = JsonPatch.apply(this.asJsonPatch(), old);
        JsonNode actualVersion = JsonPatch.apply(patch, old);
        //actual may have additional changes we don't care about so don't want to do exact equals check
        //only check the changes we care about
        for(Change c : changes.values()){
            JsonPointer key = JsonPointer.valueOf(c.getKey());
            JsonNode actualNode = actualVersion.at(key);
            if(c.getType() == Change.ChangeType.ADDED || c.getType() == Change.ChangeType.REPLACED) {

                JsonNode expected = ourVersion.at(key);
                if(expected.equals(actualNode)){
                    throw new AssertionError(c +" expected = " + expected + " but was " + actualNode);
                }
            }else if(c.getType()== Change.ChangeType.REMOVED){
               if(!actualNode.isMissingNode()){
                   throw new AssertionError("should be removed but wasn't " + c + " but was " + actualNode);
               }

            }
        }

    }

    public JsonNode asJsonPatch(){
        ObjectMapper mapper = new ObjectMapper();
        JsonNode n= mapper.valueToTree(changes.values());

        return n;
    }
    
    public Changes extra(Changes actual) {
       return actual.missingFrom(this);
    }

    
    public Changes intersection(Changes actual) {
        Set<Change> copy = new HashSet<>(changes.values());
        copy.retainAll(actual.changes.values());

        return toChanges(copy);
    }

    private Changes toChanges(Set<Change> copy) {
        Map<String, Change> map = new HashMap<>(copy.size());
        for(Change c: copy){
            map.put(c.getKey(), c);
        }
        return new Changes(map);
    }

    public Changes union(Changes actual) {
        Map<String, Change> map = new HashMap<>(changes);
        map.putAll(actual.changes);

        return new Changes(map);
    }

    public Changes diff(Changes other){
        Changes union = union(other);
        Changes intersecion = intersection(other);

        Set<Change> v = new HashSet<>(union.changes.values());
        v.removeAll(intersecion.changes.values());

        return toChanges(v);
    }
    
    public void printDifferences(Changes other){
    	System.out.println("Missing:");
    	System.out.println(this.missingFrom(other));
    	System.out.println("Extra:");
    	System.out.println(this.extra(other));
    }

	public int size() {
		return changes.size();
	}
}
