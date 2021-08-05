package gsrs.cache;

import ix.core.util.EntityUtils;
import ix.utils.CallableUtil;

import java.io.Closeable;
import java.util.Map;

public interface GsrsCache extends Closeable {
    Object get(String key);

    Object getRaw(String key);

    <T> T getOrElse (long epoch,
                     String key, CallableUtil.TypedCallable<T> generator) throws Exception;

    <T> T getOrElse (String key, CallableUtil.TypedCallable<T> generator)
                                       throws Exception;

    // mimic play.Cache
    <T> T getOrElse (String key, CallableUtil.TypedCallable<T> generator,
                     int seconds) throws Exception;

    void clearCache();

    <T> T getOrElseRaw (String key, CallableUtil.TypedCallable<T> generator,
                        int seconds) throws Exception;

    boolean remove(String key);

    boolean removeAllChildKeys(String key);

    boolean contains(String key);

    void setRaw(String key, Object value);

    @SuppressWarnings("unchecked")
    <T> T getOrElseTemp(String key, CallableUtil.TypedCallable<T> generator) throws Exception;

    @SuppressWarnings("unchecked")
    <T> T updateTemp(String key, T t) throws Exception;

    Object getTemp(String key);

    void setTemp(String key, Object value);

    void addToMatchingContext(String contextID, EntityUtils.Key key, String prop, Object value);

    void setMatchingContext(String contextID, EntityUtils.Key key, Map<String, Object> matchingContext);

    Map<String, Object> getMatchingContextByContextID(String contextID, EntityUtils.Key key);

    Object getConfiguration();
}
