package gsrs.util;

public final class SanitizerUtil {

    private SanitizerUtil(){
        //can not instantiate
    }
    public static Integer sanitizeInteger(Integer i, int defaultValue) {
        if(i==null || i.intValue() <0){
            return defaultValue;
        }
        return i;

    }

    public static Double sanitizeDouble(Double i, double defaultValue) {

        if(i==null || i.intValue() <0){
            return defaultValue;
        }
        return i;
    }
}
