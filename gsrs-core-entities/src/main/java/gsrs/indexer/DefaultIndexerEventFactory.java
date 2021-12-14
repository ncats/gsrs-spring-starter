package gsrs.indexer;

import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;

/**
 * Default implementation of {@link IndexerEventFactory}
 * that creates IndexEvents for any Object.  It has an `{@link #getOrder()}
 * that returns {@link Ordered#LOWEST_PRECEDENCE} so that
 * it will always be last in the list of factories.
 */
public class DefaultIndexerEventFactory implements IndexerEventFactory{
    @Override
    public boolean supports(Object object) {
        return true;
    }
    // last order to be last in a sorted list
    @Override
    public int getOrder() {
        return Ordered.LOWEST_PRECEDENCE;
    }
}
