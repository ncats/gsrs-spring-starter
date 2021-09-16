package gsrs;

import java.util.concurrent.ConcurrentHashMap;

import gov.nih.ncats.common.Tuple;

public class DataSourceConfigRegistry {
    
    private static ConcurrentHashMap<String, String> qualifierRegistry= new ConcurrentHashMap<>();
    private static ConcurrentHashMap<String, String> qualifierClassRegistry= new ConcurrentHashMap<>();
    
    public static void register(String qual, String ...pack) {
        for(String p: pack) {
            qualifierRegistry.put(p, qual);
        }
    }
    
    public static String getQualifierFor(Class c) {
        return qualifierClassRegistry.computeIfAbsent(c.getName(), k->{
String q=            qualifierRegistry.entrySet()
            .stream()
            .map(Tuple::of)
            .filter(t->k.startsWith(t.k()))
            .findFirst()
            .map(t->t.v())
            .orElse(null)
            ;
         System.out.println("Found:" + c + " as " + q);
return q;
        });
        
    }
}
