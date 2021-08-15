package gsrs.springUtils;

import gov.nih.ncats.common.util.Unchecked;

import javax.servlet.http.HttpServletRequest;

import java.net.URI;
import java.util.function.Consumer;

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
