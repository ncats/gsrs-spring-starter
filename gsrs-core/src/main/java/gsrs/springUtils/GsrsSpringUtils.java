package gsrs.springUtils;

import javax.servlet.http.HttpServletRequest;

public class GsrsSpringUtils {
    public static String getFullUrlFrom(HttpServletRequest req) {
        String queryString = req.getQueryString();
        if(queryString ==null || queryString.isEmpty()){
            return req.getRequestURL().toString();
        }else {
            return req.getRequestURL().toString() + "?" + req.getQueryString();
        }
    }
}
