package ix.core.cache;

import java.lang.annotation.*;

/**
 * Annotation to put on classes of objects that
 * get cached to explain
 * the rules for how this object
 * should be cached.
 */
@Documented
@Retention(value=RetentionPolicy.RUNTIME)
@Inherited
@Target(value={ElementType.TYPE})
public @interface CacheStrategy {
	/**
	 * Is this object allowed to be evicted from the
	 * cache if the cache gets too big.
	 * @return {@code true} if it can (the default)
	 * and {@code false} if it should never be evicted.
     */
	 boolean evictable() default true;
}
