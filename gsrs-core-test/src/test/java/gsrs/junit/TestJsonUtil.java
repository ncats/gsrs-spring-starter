package gsrs.junit;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import gsrs.junit.json.JsonUtil;


import java.util.*;

import static org.junit.Assert.assertTrue;

public final class TestJsonUtil {

	private TestJsonUtil(){
		//can not instantiate
	}
	
	public static void assertEquals(JsonNode a, JsonNode b, Comparator<JsonNode> comparator){

		if(!equals(a,b, comparator)){
			throw new AssertionError("expected : " + a +" but was : " + b);
		}
	}

	public static boolean equals(JsonNode a, JsonNode b, Comparator<JsonNode> comparator) {
		if(a instanceof ObjectNode){
			return objectNodeEqualTraversal((ObjectNode) a, b, comparator);
		}else if( a instanceof ArrayNode){
			return arrayNodeEqualTraversal((ArrayNode) a, b, comparator);
		}else{
			return comparator.compare(a, b) ==0;
		}
	}

	private static <K, V> Map<K, V> toMap(Iterator<Map.Entry<K,V>> iterator){
		LinkedHashMap<K,V> map = new LinkedHashMap<>();
		while(iterator.hasNext()){
			Map.Entry<K,V> e = iterator.next();
			map.put(e.getKey(), e.getValue());
		}
		return map;
	}
	private static boolean objectNodeEqualTraversal(ObjectNode a, JsonNode o, Comparator<JsonNode> comparator){
		if (!(o instanceof ObjectNode)) {
			return false;
		}
		ObjectNode other = (ObjectNode) o;
		Map<String, JsonNode> m1 = toMap(a.fields());
		Map<String, JsonNode> m2 = toMap(other.fields());

		final int len = m1.size();
		if (m2.size() != len) {
		    System.out.println("obj node size notequal "+ m2.size() + "vs "+ len);
			return false;
		}

		for (Map.Entry<String, JsonNode> entry : m1.entrySet()) {
			JsonNode v2 = m2.get(entry.getKey());
			if ((v2 == null) || !equals(entry.getValue(), v2,comparator)) {
				System.out.println("FAILED : \n"+entry.getValue()+"\n"+ v2);
				return false;
			}
		}
		return true;
	}

	private static <T> List<T> toList(Iterator<T> iter){
		List<T> list = new ArrayList<>();
		while(iter.hasNext()){
			list.add(iter.next());
		}
		return list;
	}
	private static boolean arrayNodeEqualTraversal(ArrayNode a, JsonNode o, Comparator<JsonNode> comparator){

			if (!(o instanceof ArrayNode)) {
				return false;
			}
			ArrayNode other = (ArrayNode) o;
			final int len = a.size();
			if (other.size() != len) {
				return false;
			}
			List<JsonNode> aList = toList(a.iterator());
			aList.sort(comparator);

			List<JsonNode> oList = toList(o.iterator());
			oList.sort(comparator);


			Iterator<JsonNode> aIter = aList.iterator();
			Iterator<JsonNode> oIter = oList.iterator();
			for(; aIter.hasNext() & oIter.hasNext(); ){
				if(! equals(aIter.next(), oIter.next(), comparator)){
					System.out.println("FAILED\n" + aList+"\n"+ oList);
					return false;
				}

			}

			return true;


	}





	public static JsonNode prepareUnapproved(JsonNode substance){
		
		return new JsonUtil.JsonNodeBuilder(substance)
				.remove("/approvalID")
				.remove("/approved")
				.remove("/approvedBy")
				
				.set("/status", "pending")
				.ignoreMissing().build();
		
		
	}



	public static boolean isLiteralNull( JsonNode js){
		return js.isNull();
	}

	public static void ensureIsValid(JsonNode js){
		try{
			assertTrue( isValid(js));
		}catch(Throwable e){
			System.err.println(js.toString());
			throw e;
		}
	}
	
	public static boolean isValid(JsonNode js){
		return js.get("valid").asBoolean();
	}

    public static String getApprovalStatus(JsonNode js){
        return js.get("status").asText().toLowerCase();
    }

    public static String getApprovalId(JsonNode js){
        return js.get("approvalID").asText();
    }

	public static String getRefUuidOnFirstRelationship(JsonNode js){
		JsonNode relations = js.get("relationships").get(0);
		JsonNode relatedSubs = relations.get("relatedSubstance");
		return relatedSubs.get("refuuid").asText();
	}
	public static String getTypeOnFirstRelationship(JsonNode js){
		return js.at("/relationships/0/type").asText();
	}


}
