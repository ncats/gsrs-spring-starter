package gsrs.indexer;

/**
 * A Factory to get the {@link IndexerEventFactory} for particular Objects.
 */
public interface IndexerEventFactoryFactory {
    /**
     * Get a {@link IndexerEventFactory} for the given Object to be indexed.
     * @param o the object o index; will never be null.
     * @return an IndexerEventFactory; if the returned object is null, then the object should not be indexed.
     */
    IndexerEventFactory getIndexerFactoryFor(Object o);
}
