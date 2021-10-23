package ix.core.cache;

import ix.utils.CallableUtil.TypedCallable;

import net.sf.ehcache.Element;
import net.sf.ehcache.statistics.CoreStatistics;

import java.io.Closeable;
import java.util.List;
import java.util.stream.Stream;

/**
 * Created by katzelda on 5/26/16.
 */
public interface GateKeeper extends Closeable {
    boolean remove(String key);

    boolean removeAllChildKeys(String key);

    <T> T getSinceOrElse(String key, long creationTime, TypedCallable<T> generator) throws Exception;
    <T> T getSinceOrElseRaw(String key, long creationTime, TypedCallable<T> generator) throws Exception;

    Object get(String key);

    Object getRaw(String key);

    void put(String key, Object value, int expiration);

    void putRaw(String key, Object value);

    void putRaw(String key, Object value, int expiration);

    boolean contains(String key);

    void put(String key, Object value);

    <T> T getOrElseRaw(String key, TypedCallable<T> generator) throws Exception;

    <T> T getOrElse(String key, TypedCallable<T> generator) throws Exception;

    @Override
    void close();

    Element getRawElement(String key);

    Stream<Element> elements(int top, int skip);

    List<CoreStatistics> getStatistics();

    void clear();
}
