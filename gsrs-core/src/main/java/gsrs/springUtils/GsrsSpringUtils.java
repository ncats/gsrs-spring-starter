package gsrs.springUtils;

import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import jakarta.servlet.http.HttpServletRequest;

import gov.nih.ncats.common.Tuple;
import gov.nih.ncats.common.stream.StreamUtil;
import gov.nih.ncats.common.util.Unchecked;

public class GsrsSpringUtils {

    private GsrsSpringUtils(){
        //can not instantiate
    }
    
    @Deprecated
    public static String getFullUrlFrom(HttpServletRequest req) {
      
        String queryString = req.getQueryString();
        if(queryString ==null || queryString.isEmpty()){
            return req.getRequestURL().toString();
        }else {
            return req.getRequestURL().toString() + "?" + req.getQueryString();
        }
    }
    
    public static Map<String,List<String>> toHeadersMap(HttpServletRequest parentRequest){
        Map<String, List<String>> headers = StreamUtil.forEnumeration(parentRequest.getHeaderNames())
                .map(n->Tuple.of(n,n))
                .map(Tuple.vmap(n->StreamUtil.forEnumeration(parentRequest.getHeaders(n)).collect(Collectors.toList())))
                .collect(Tuple.toMap());
        return headers;
    }

    public static void tryTaskAtMost(Unchecked.ThrowingRunnable t, Consumer<Throwable> cons, int n) {
        n = Math.max(1, n);
        while (n-- > 0) {
            try {
                t.run();
                return;
            } catch (Throwable e) {
                if (n == 0)
                    cons.accept(e);
            }
        }
    }

}
