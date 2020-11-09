package ix.core;



public interface EntityProcessor<K>{
	class FailProcessingException extends Exception{
		public FailProcessingException() {
			super();
		}

		public FailProcessingException(String message) {
			super(message);
		}

		public FailProcessingException(String message, Throwable cause) {
			super(message, cause);
		}

		public FailProcessingException(Throwable cause) {
			super(cause);
		}
	}
	
	default void prePersist(K obj) throws FailProcessingException{};
	default void postPersist(K obj) throws FailProcessingException{};
	default void preRemove(K obj) throws FailProcessingException{};
	default void postRemove(K obj) throws FailProcessingException{};
	default void preUpdate(K obj) throws FailProcessingException{};
	default void postUpdate(K obj) throws FailProcessingException{};
	default void postLoad(K obj) throws FailProcessingException{};

	/**
	 * Get the Class of Entityt Type K.
	 * @return Class of K.
	 *
	 * @since 3.0
	 */
	Class<K> getEntityClass();

	default EntityProcessor<K> combine(EntityProcessor<K> other){
		return new CombinedEntityProcessor<>(getEntityClass(),this, other);
	}
}