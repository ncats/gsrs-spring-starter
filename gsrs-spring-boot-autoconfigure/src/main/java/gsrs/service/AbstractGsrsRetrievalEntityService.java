package gsrs.service;

import com.fasterxml.jackson.databind.JsonNode;
import gov.nih.ncats.common.util.CachedSupplier;
import gsrs.EntityPersistAdapter;
import gsrs.autoconfigure.GsrsRabbitMqConfiguration;
import gsrs.controller.IdHelper;
import gsrs.events.AbstractEntityCreatedEvent;
import gsrs.events.AbstractEntityUpdatedEvent;
import gsrs.springUtils.StaticContextAccessor;
import gsrs.validator.DefaultValidatorConfig;
import gsrs.validator.GsrsValidatorFactory;
import ix.core.models.ForceUpdatableModel;
import ix.core.validator.ValidationMessage;
import ix.core.validator.ValidationResponse;
import ix.core.validator.ValidatorCallback;
import ix.ginas.utils.validation.ValidatorFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.PostConstruct;
import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public abstract class AbstractGsrsRetrievalEntityService<T,I> implements GsrsRetrievalEntityService<T, I> {
    @Autowired
    private GsrsValidatorFactory validatorFactoryService;

    @Autowired
    private ApplicationEventPublisher applicationEventPublisher;

    @Autowired
    private EntityPersistAdapter entityPersistAdapter;

    @Autowired
    private RabbitTemplate rabbitTemplate;
    @Autowired
    private GsrsRabbitMqConfiguration gsrsRabbitMqConfiguration;

    private String exchangeName;

    private String substanceCreatedKey, substanceUpdatedKey, substanceFailedKey;

    private final String context;
    private final Pattern idPattern;

    private CachedSupplier<ValidatorFactory> validatorFactory;
    /**
     * Create a new GSRS Entity Service with the given context.
     * @param context the context for the routes of this controller.
     *
     * @param idPattern the {@link Pattern} to match an ID.  This will be used to determine
     *                  Strings that are IDs versus Strings that should be flex looked up.  @see {@link #flexLookup(String)}.
     * @param exchangeName The Rabbit exchange name for this entity; can not be {@code null}.
     *
     * @param entityCreateKey The Rabbit queue to publish to when an entity is created; can not be {@code null}.
     *
     * @param entityUpdateKey The Rabbit exchange name for this entity; can not be {@code null}.
     * @throws NullPointerException if any parameters are null.
     */
    public AbstractGsrsRetrievalEntityService(String context, Pattern idPattern,
                                     String exchangeName,
                                     String entityCreateKey,
                                     String entityUpdateKey) {
        this.context = Objects.requireNonNull(context, "context can not be null");
        this.idPattern = Objects.requireNonNull(idPattern, "ID pattern can not be null");
        this.exchangeName = exchangeName;
        if(exchangeName !=null) {
            this.substanceCreatedKey = Objects.requireNonNull(entityCreateKey);
            this.substanceUpdatedKey = Objects.requireNonNull(entityUpdateKey);
        }else{
            //ignore fields
            this.substanceCreatedKey = entityCreateKey;
            this.substanceUpdatedKey = entityUpdateKey;
        }
    }
    /**
     * Create a new GSRS Entity Service with the given context.
     * @param context the context for the routes of this controller.
     * @param idHelper the {@link IdHelper} to match an ID.  This will be used to determine
     *                  Strings that are IDs versus Strings that should be flex looked up.  @see {@link #flexLookup(String)}.
     * @param exchangeName The Rabbit exchange name for this entity; can not be {@code null}.
     * @param entityCreateKey The Rabbit queue to publish to when an entity is created; can not be {@code null}.
     *
     * @param entityUpdateKey The Rabbit exchange name for this entity; can not be {@code null}.
     * @throws NullPointerException if any parameters are null.
     */
    public AbstractGsrsRetrievalEntityService(String context, IdHelper idHelper, String exchangeName,
                                     String entityCreateKey, String entityUpdateKey) {
        this(context, Pattern.compile("^"+idHelper.getRegexAsString() +"$"), exchangeName, entityCreateKey, entityUpdateKey);
    }

    @Override
    public String getContext() {
        return context;
    }

    @PostConstruct
    private void initValidator(){
        //need this in a post construct so the validator factory service is injected
        //This is added to the initization Group so that we can reset this in tests

        //This cache might be unncessary as of now the call to newFactory isn't cached
        //by tests where the return value could change over time?  but keep it here anyway for now...
        validatorFactory = ENTITY_SERVICE_INTIALIZATION_GROUP.add(()->validatorFactoryService.newFactory(context));
    }

    /**
     * Create a new instance of your entity type from the provided JSON.
     * Perform any object clean up or "data cleansing" here.
     *
     * @param json the JSON to use; will never be null.
     *
     * @return a new entity instance will never be null.
     *
     * @throws IOException if there's a problem processing the JSON.
     */
    protected abstract T fromNewJson(JsonNode json) throws IOException;
    /**
     * Create List of  new instances of your entity type from the provided JSON which
     * is a List of entities.
     * Please override to perform any object clean up or "data cleansing" here.
     *
     * @param list the JSON to use; will never be null.
     *
     * @return a new List of instances will never be null and probably shouldn't be empty.
     *
     * @throws IOException if there's a problem processing the JSON.
     *
     * @implNote by default this will perform the following:
     * <pre>
     * {@code
     *
     *         List<T> l = new ArrayList<>(list.size());
     *         for(JsonNode n : list){
     *             l.add(fromNewJson(n));
     *         }
     *
     *         return l;
     *   }
     *   </pre>
     */
    protected List<T> fromNewJsonList(JsonNode list) throws IOException{
        List<T> l = new ArrayList<>(list.size());
        for(JsonNode n : list){
            l.add(fromNewJson(n));
        }
        return l;
    }
    /**
     * Create a instance of your entity type from the provided JSON representing
     * an updated version of a  record.
     * Perform any object clean up or "data cleansing" here but keep in mind different data cleansings
     * might be done on new vs updated records.
     *
     * @param json the JSON to use; will never be null.
     *
     * @return a new entity instance will never be null.
     *
     * @throws IOException if there's a problem processing the JSON.
     */
    protected abstract T fromUpdatedJson(JsonNode json) throws IOException;



    /**
     * Create List of  instances of your entity type from the provided JSON which
     * is a List of updated versions of records.
     * please override to perform any object clean up or "data cleansing" here  here but keep in mind different data cleansings
     * might be done on new vs updated records.
     *
     * @param list the JSON to use; will never be null.
     *
     * @return a new List of instances will never be null and probably shouldn't be empty.
     *
     * @throws IOException if there's a problem processing the JSON.
     * @implNote by default this will perform the following:
     * <pre>
     * {@code
     *
     *         List<T> l = new ArrayList<>(list.size());
     *         for(JsonNode n : list){
     *             l.add(fromUpdatedJson(n));
     *         }
     *
     *         return l;
     *   }
     *   </pre>
     */
    protected List<T> fromUpdatedJsonList(JsonNode list) throws IOException{
        List<T> l = new ArrayList<>(list.size());
        for(JsonNode n : list){
            l.add(fromUpdatedJson(n));
        }
        return l;
    }

    /**
     * Create a JSON representation of your entity.
     * @param t
     * @return
     * @throws IOException
     */
    protected abstract JsonNode toJson(T t) throws IOException;

    /**
     * Save the given entity to your data repository.
     * @param t the entity to save.
     * @return the new saved entity, usually this is the same object as the t passed in but it doesn't have to be.
     * The returned object may have different fields set like id, or audit information etc.
     */
    protected abstract T create(T t);

    /**
     * Get the entity from your data repository with the given id.
     * @param id the id to use to look up the entity.
     * @return an Optional that is empty if there is no entity with the given id;
     * or an Optional that has the found entity.
     */
    public abstract Optional<T> get(I id);

    /**
     * Parse the Id from the given String.
     * @param idAsString the String to parse into an ID.
     * @return the ID as the correct type.
     */
    public abstract I parseIdFromString(String idAsString);

    public EntityManager getEntityManager() {
        EntityManager em = StaticContextAccessor.getEntityManagerFor(this.getEntityClass());
        return em;
    }

    /**
     * Get the ID from the entity.
     * @param entity the entity to get the id of.
     * @return the ID of this entity.
     */
    //TODO should this be turned into a Function so we can pass a method reference in constructor?
    public abstract I getIdFrom(T entity);

    protected abstract AbstractEntityUpdatedEvent<T> newUpdateEvent(T updatedEntity);

    protected abstract AbstractEntityCreatedEvent<T> newCreationEvent(T createdEntity);

    protected <T>  ValidationResponse<T> createValidationResponse(T newEntity, Object oldEntity, DefaultValidatorConfig.METHOD_TYPE type){
        return new ValidationResponse<T>(newEntity);
    }

    /**
     * Persist the given entity inside a transaction.
     * This method should NOT be overridden by client code
     * and is only here so Spring AOP can subclass it to make it transactional.
     * @param newEntity the entity to persist.
     * @return the persisted entity.
     * @implSpec delegates to {@link #create(Object)} inside a {@link Transactional}.
     */
    protected T transactionalPersist(T newEntity){
        T saved = create(newEntity);
//        EntityUtils.EntityWrapper<?> ew = EntityUtils.EntityWrapper.of(saved);
//
//
//        EntityManager em = ew.getKey().getEntityManager();
//
//        //TODO:Fix this to be a standalone thing
//        boolean shouldStore =  !env.getProperty(EntityPersistAdapter.GSRS_HISTORY_DISABLED , Boolean.class,  false);
//
//
//        try {
//            //TODO: this edit feels like a mistake since there's another edit
//            // creation area?
//            if(shouldStore &&  ew.storeHistory() && ew.hasKey()) {
//                Edit edit = new Edit(ew.getEntityClass(), ew.getKey().getIdString());
//                String newVersionStr = ew.getVersion().orElse(null);
//                if(newVersionStr ==null) {
//                    edit.version = null;
//                }else{
//                    edit.version = Long.toString(Long.parseLong(newVersionStr) -1);
//                }
//                edit.comments = ew.getChangeReason().orElse(null);
//                edit.kind = ew.getKind();
//                edit.newValue = ew.toFullJson();
//                editRepository.save(edit);
//
//            }
//            //Feels wrong, not sure what to make of it?
//            entityManager.flush();
//
//            //TODO: work out which EMs to use here
////            em.flush();
//
//        }catch(Exception e) {
//            e.printStackTrace();
//            throw e;
//        }
        return saved;
    }

    /**
     * Override this method if any post processing on an entity to be updated needs to be done.
     * @apiNote by default this just return updatedEntity as is.
     * @param updatedEntity the entity being updated; should never be null.
     * @return the Entity to update.
     */
    protected T fixUpdatedIfNeeded(T updatedEntity){
        return updatedEntity;
    }

    private static PlatformTransactionManager getTransactionManagerForEntityManager(EntityManager em) {
        EntityManagerFactory emf = em.getEntityManagerFactory();
        JpaTransactionManager transactionManager = new JpaTransactionManager();
        transactionManager.setEntityManagerFactory(emf);
        return transactionManager;
    }

    public PlatformTransactionManager getTransactionManager() {
        Class cls = this.getEntityClass();
        EntityManager em = StaticContextAccessor.getEntityManagerFor(cls);
        return getTransactionManagerForEntityManager(em);
    }


    protected <T> ValidatorCallback createCallbackFor(T object, ValidationResponse<T> response, DefaultValidatorConfig.METHOD_TYPE type) {
        return new ValidatorCallback() {
            @Override
            public void addMessage(ValidationMessage message) {
                response.addValidationMessage(message);
            }

            @Override
            public void setInvalid() {
                response.setValid(false);
            }

            @Override
            public void setValid() {
                response.setValid(true);
            }

            @Override
            public void haltProcessing() {

            }

            @Override
            public void addMessage(ValidationMessage message, Runnable appyAction) {
                response.addValidationMessage(message);
                appyAction.run();
            }

            @Override
            public void complete() {
                if(response.hasError()){
                    response.setValid(false);
                }
            }
        };
    }

    private boolean isId(String s){
        Matcher idMatch = idPattern.matcher(s);
        return idMatch.matches();
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<T> getEntityBySomeIdentifier(String id) {
        Optional<T> opt;
        if(isId(id)) {
            opt = get(parseIdFromString(id));
        }else {
            opt = flexLookup(id);
        }
        return opt;
    }
    @Override
    @Transactional(readOnly = true)
    public Optional<I> getEntityIdOnlyBySomeIdentifier(String id) {
        Optional<I> opt;
        if(isId(id)) {
            opt = Optional.ofNullable(parseIdFromString(id));
        }else {
            opt = flexLookupIdOnly(id);
        }
        return opt;
    }

    /**
     * Fetch an entity's real ID from the data repository using a unique
     * identifier that isn't the entity's ID.  Override this method
     * if a more efficient query can be made rather than fetching the whole record.
     *
     * @implSpec the default implementation of this method is:
     * <pre>
     * {@code
     *  Optional<T> opt = flexLookup(someKindOfId);
     *  if(!opt.isPresent()){
     *     return Optional.empty();
     *  }
     *  return Optional.of(getIdFrom(opt.get()));
     * }
     * </pre>
     * @param someKindOfId a String that isn't the ID of the entity.
     * @return an Optional that is empty if there is no entity with the given id;
     *  or an Optional that has the found entity's ID.
     */
    protected Optional<I> flexLookupIdOnly(String someKindOfId){
        Optional<T> opt = flexLookup(someKindOfId);
        if(!opt.isPresent()){
            return Optional.empty();
        }
        return Optional.of(getIdFrom(opt.get()));
    }

    /**
     * Fetch an entity from the data repository using a unique
     * identifier that isn't the entity's ID.
     *
     * @param someKindOfId a String that isn't the ID of the entity.
     * @return an Optional that is empty if there is no entity with the given id;
     *  or an Optional that has the found entity.
     */
    @Transactional
    public abstract Optional<T> flexLookup(String someKindOfId);

}
