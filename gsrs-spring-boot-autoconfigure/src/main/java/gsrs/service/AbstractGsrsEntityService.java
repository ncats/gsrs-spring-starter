package gsrs.service;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Stack;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.annotation.PostConstruct;
import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.PersistenceContext;

import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.core.env.Environment;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import com.fasterxml.jackson.databind.JsonNode;

import gov.nih.ncats.common.sneak.Sneak;
import gov.nih.ncats.common.util.CachedSupplier;
import gsrs.EntityPersistAdapter;
import gsrs.autoconfigure.GsrsRabbitMqConfiguration;
import gsrs.controller.IdHelper;
import gsrs.events.AbstractEntityCreatedEvent;
import gsrs.events.AbstractEntityUpdatedEvent;
import gsrs.json.JsonEntityUtil;
import gsrs.springUtils.StaticContextAccessor;
import gsrs.validator.DefaultValidatorConfig;
import gsrs.validator.GsrsValidatorFactory;
import gsrs.validator.ValidatorConfig;
import ix.core.models.ForceUpdatableModel;
import ix.core.util.EntityUtils;
import ix.core.util.EntityUtils.EntityWrapper;
import ix.core.util.EntityUtils.Key;
import ix.core.util.LogUtil;
import ix.core.validator.ValidationMessage;
import ix.core.validator.ValidationResponse;
import ix.core.validator.Validator;
import ix.core.validator.ValidatorCallback;
import ix.core.validator.ValidatorCategory;
import ix.ginas.utils.validation.ValidatorFactory;
import ix.utils.pojopatch.PojoDiff;
import ix.utils.pojopatch.PojoPatch;
import lombok.extern.slf4j.Slf4j;

/**
 * Abstract implementation of {@link GsrsEntityService} to reduce most
 * of the boilerplate of creating a {@link GsrsEntityService}.
 * @param <T> the entity type.
 * @param <I> the type for the entity's ID.
 */
@Slf4j
public abstract class AbstractGsrsEntityService<T,I> implements GsrsEntityService<T, I> {

    
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
    public AbstractGsrsEntityService(String context, Pattern idPattern,
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
    public AbstractGsrsEntityService(String context, IdHelper idHelper, String exchangeName,
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

    /**
     * Update the given entity in the repository.
     * @param t the entity to update.
     * @return the new saved entity, usually this is the same object as the t passed in but it doesn't have to be.
     * The returned object may have different fields set like  audit information etc.
     */
    protected abstract T update(T t);


    @Override
    public  CreationResult<T> createEntity(JsonNode newEntityJson, boolean partOfBatchLoad){
        
        TransactionTemplate transactionTemplate = new TransactionTemplate(this.getTransactionManager());
        
        ValidatorConfig.METHOD_TYPE methodType = partOfBatchLoad? ValidatorConfig.METHOD_TYPE.BATCH : ValidatorConfig.METHOD_TYPE.CREATE;
        return transactionTemplate.execute( status-> {
            try {
                T newEntity = fromNewJson(newEntityJson);
                newEntity = JsonEntityUtil.fixOwners(newEntity, true);
                Validator<T> validator = validatorFactory.getSync().createValidatorFor(newEntity, null, methodType, ValidatorCategory.CATEGORY_ALL());

                ValidationResponse<T> response = createValidationResponse(newEntity, null, methodType);
                ValidatorCallback callback = createCallbackFor(newEntity, response, methodType);
                validator.validate(newEntity, null, callback);
                callback.complete();

                CreationResult.CreationResultBuilder<T> builder = CreationResult.<T>builder()
                        .validationResponse(response);

                if (response != null && !response.isValid()) {

                    return builder.build();
                }
                newEntity = JsonEntityUtil.fixOwners(newEntity, true);

                T createdEntity = transactionalPersist(newEntity);
                AbstractEntityCreatedEvent<T> event = newCreationEvent(createdEntity);
                if(event !=null) {
                    applicationEventPublisher.publishEvent(event);
                    if (gsrsRabbitMqConfiguration.isEnabled() && exchangeName != null) {
                        rabbitTemplate.convertAndSend(exchangeName, substanceCreatedKey, event);
                    }
                }
                return builder.createdEntity(createdEntity)
                        .created(true)
                        .build();
            } catch (Exception t) {
                t.printStackTrace();
                status.setRollbackOnly();

                throw new RuntimeException(t);
            }
        });
    }

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
    
    @Override
    public UpdateResult<T> updateEntity(T updatedEntity, EntityPersistAdapter.ChangeOperation<T> changeOperation) throws Exception {

        TransactionTemplate transactionTemplate = new TransactionTemplate(getTransactionManager());
        
        
        return transactionTemplate.execute( status-> {
            UpdateResult.UpdateResultBuilder<T> builder = UpdateResult.<T>builder();
            EntityUtils.EntityWrapper<T> savedVersion = entityPersistAdapter.performChangeOn(updatedEntity, changeOperation::apply);

            if(savedVersion ==null){
                status.setRollbackOnly();
                builder.status(UpdateResult.STATUS.ERROR);
            }else {
                builder.status(UpdateResult.STATUS.UPDATED);
                builder.updatedEntity(savedVersion.getValue());

                //only publish events if we save!
                AbstractEntityUpdatedEvent<T> event = newUpdateEvent(savedVersion.getValue());
                if(event !=null) {
                    applicationEventPublisher.publishEvent(event);
                    if (gsrsRabbitMqConfiguration.isEnabled() && exchangeName != null) {
                        rabbitTemplate.convertAndSend(exchangeName, substanceUpdatedKey, event);
                    }
                }

            }
            return builder.build();
        });
    }
    
    
    @Override
    public UpdateResult<T> updateEntity(JsonNode updatedEntityJson) throws Exception {

        TransactionTemplate transactionTemplate = new TransactionTemplate(this.getTransactionManager());
        
        return transactionTemplate.execute( status-> {
            try {
                T updatedEntity = JsonEntityUtil.fixOwners(fromUpdatedJson(updatedEntityJson), true);
                Key oKey = EntityWrapper.of(updatedEntity).getKey();                
                EntityManager entityManager = oKey.getEntityManager();

                

                UpdateResult.UpdateResultBuilder<T> builder = UpdateResult.<T>builder();
                EntityUtils.EntityWrapper<T> savedVersion = entityPersistAdapter.change(oKey, oldEntity -> {
                	EntityUtils.EntityWrapper<T> og = EntityUtils.EntityWrapper.of(oldEntity);
                	String oldJson = og.toFullJson();
                	Validator<T> validator = validatorFactory.getSync().createValidatorFor(updatedEntity, oldEntity, DefaultValidatorConfig.METHOD_TYPE.UPDATE, ValidatorCategory.CATEGORY_ALL());

                	ValidationResponse<T> response = createValidationResponse(updatedEntity, oldEntity, DefaultValidatorConfig.METHOD_TYPE.UPDATE);
                	ValidatorCallback callback = createCallbackFor(updatedEntity, response, DefaultValidatorConfig.METHOD_TYPE.UPDATE);
                	validator.validate(updatedEntity, oldEntity, callback);

                	callback.complete();

                	builder.validationResponse(response)
                           .oldJson(oldJson);

                	if (response != null && !response.isValid()) {
                		builder.status(UpdateResult.STATUS.ERROR);
                		return Optional.empty();
                	}
                	
                	EntityWrapper<T> oWrap = EntityWrapper.of(oldEntity);
                	EntityWrapper<T> nWrap = EntityWrapper.of(updatedEntity);

                	boolean usePojoPatch=false;
                	//only use POJO patch if the entities are the same type
                	if(oWrap.getEntityClass().equals(nWrap.getEntityClass())){ 
                		usePojoPatch=true;
                	}
                	if(usePojoPatch) {
                		PojoPatch<T> patch = PojoDiff.getDiff(oldEntity, updatedEntity);
                        LogUtil.debug(() -> "changes = " + patch.getChanges());
                		final List<Object> removed = new ArrayList<Object>();
                		final List<Object> added = new ArrayList<Object>();

                		//Apply the changes, grabbing every change along the way
                		Stack changeStack = patch.apply(oldEntity, c -> {
                			if ("remove".equals(c.getOp())) {
                				removed.add(c.getOldValue());
                			}
                			if ("add".equals(c.getOp())) {
                				added.add(c.getNewValue());
                			}
                			LogUtil.trace(() -> c.getOp() + "\t" + c.getOldValue() + "\t" + c.getNewValue());
                		});
                		if (changeStack.isEmpty()) {
                			throw new IllegalStateException("No change detected");
                		} else {
                			LogUtil.debug(() -> "Found:" + changeStack.size() + " changes");
                		}

                		oldEntity = fixUpdatedIfNeeded(JsonEntityUtil.fixOwners(oldEntity, true));
                		//This is the last line of defense for making sure that the patch worked
                		//Should throw an exception here if there's a major problem
                		//This is inefficient, but forces confirmation that the object is fully realized
                		String serialized = EntityUtils.EntityWrapper.of(oldEntity).toJsonDiffJson();

                		added.stream()
                		.filter(Objects::nonNull)
                		.map(o -> EntityUtils.EntityWrapper.of(o))
                		.forEach(ew -> {
                			Object o = ew.getValue();
                			log.warn("adding:" + o);
                			entityManager.persist(o);
                		});

                		while (!changeStack.isEmpty()) {
                			Object v = changeStack.pop();
                			EntityUtils.EntityWrapper<Object> ewchanged = EntityUtils.EntityWrapper.of(v);
                			if (!ewchanged.isIgnoredModel() && ewchanged.isEntity()) {
                				Object o =  ewchanged.getValue();
                				if(o instanceof ForceUpdatableModel) {
                					//Maybe don't do twice? IDK.
                					((ForceUpdatableModel)o).forceUpdate();
                				}
                				entityManager.merge(o);
                			}
                		}

                		//explicitly delete deleted things
                		//This should ONLY delete objects which "belong"
                		//to something. That is, have a @SingleParent annotation
                		//inside

                		removed.stream()
                		.filter(Objects::nonNull)
                		.map(o -> EntityUtils.EntityWrapper.of(o))
                		.filter(ew -> ew.isExplicitDeletable())
                		.forEach(ew -> {
                			Object o = ew.getValue();
                			log.warn("deleting:" + o);
                			//hibernate can only remove entities from this transaction
                			//this logic will merge "detached" entities from outside this transaction before removing anything

                			entityManager.remove(entityManager.contains(o) ? o : entityManager.merge(o));

                		});


                		try {
                			T saved = transactionalUpdate(oldEntity, oldJson);
//                			System.out.println("updated entity = " + saved);
                			String internalJSON = EntityWrapper.of(saved).toInternalJson();
//                			System.out.println("updated entity full eager fetch = " + internalJSON.hashCode());
                			builder.updatedEntity(saved);
                			
                			builder.status(UpdateResult.STATUS.UPDATED);

                			return Optional.of(saved);
                		} catch (Throwable t) {
                			t.printStackTrace();

                			builder.status(UpdateResult.STATUS.ERROR);
                            builder.throwable(t);
                			return Optional.empty();
                		}
                	}else {
                	    //NON POJOPATCH: delete and save for updates

                	    T oldValue=(T)oWrap.getValue();
                	    entityManager.remove(oldValue);

                	    // Now need to take care of bad update pieces:
                	    //	1. Version not incremented correctly (post update hooks not called) 
                	    //  2. Some metadata / audit data may be problematic
                	    //  3. The update hooks are called explicitly now
                	    //     ... and that's a weird thing to do, because the persist hooks
                	    //     will get called too. Does someone really expect things to
                	    //     get called twice?
                	    // TODO: the above pieces are from the old codebase, but the new one
                	    // has to have these evaluated too. Need unit tests.

                	    entityManager.flush();
                	    // if we clear here, it will cause issues for
                	    // some detached entities later, but not clearing causes other issues
                	    
                	    entityManager.clear();

                	    T newValue = (T)nWrap.getValue();
//                        epl.preUpdate(newValue);
                	    entityManager.persist(newValue);
                	    entityManager.flush();
//                	    entityManager.clear();
                	    
                	    
//                	    T saved=newValue;
                	    T saved = transactionalUpdate(newValue, oldJson);
                	    builder.updatedEntity(saved);
                	    builder.status(UpdateResult.STATUS.UPDATED);
                	    
                	    return Optional.of(saved); //Delete & Create
                	}
                });
                if(savedVersion ==null){
                    status.setRollbackOnly();
                }else {
                    //IDK?
//                    if(forceMoreSave[0]) {
//                        EntityUtils.EntityWrapper<T> savedVersion2 = entityPersistAdapter.performChangeOn(savedVersion, sec -> {
//                            
//                        });
//                    }
                    //only publish events if we save!
                    AbstractEntityUpdatedEvent<T> event = newUpdateEvent(savedVersion.getValue());
                    if(event !=null) {
                        applicationEventPublisher.publishEvent(event);
                        if (gsrsRabbitMqConfiguration.isEnabled() && exchangeName != null) {
                            rabbitTemplate.convertAndSend(exchangeName, substanceUpdatedKey, event);
                        }
                    }
                }

                UpdateResult<T> updateResult= builder.build();
                if(updateResult.getThrowable() !=null){
                    Sneak.sneakyThrow( updateResult.getThrowable());
                }
                return updateResult;
            }catch(IOException e){
                status.setRollbackOnly();
                throw new UncheckedIOException(e);
            }
        });
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

    /**
     * Update the given entity inside a transaction.
     * This method should NOT be overridden by client code
     * and is only here so Spring AOP can subclass it to make it transactional.
     * @param entity the entity to update.
     * @return the persisted entity.
     * @implSpec delegates to {@link #update(Object)} inside a {@link Transactional}.
     */
    protected T transactionalUpdate(T entity, String oldJson){
        if(entity instanceof ForceUpdatableModel) {
            ForceUpdatableModel mod = (ForceUpdatableModel)entity;
            mod.forceUpdate();
        }
        T after =  update(entity);
        return after;
    }

    @Override
    public ValidationResponse<T> validateEntity(JsonNode updatedEntityJson, ValidatorCategory cat) throws Exception {
        T updatedEntity = fromUpdatedJson(updatedEntityJson);
        //updatedEntity should have the same id
        I id = getIdFrom(updatedEntity);

        //Optional.ofNullable(id).map(id->get(id)); ?
        Optional<T> opt = (id==null ? Optional.empty() : get(id));
        
        Validator<T> validator;
        ValidationResponse<T> response;
        ValidatorCallback callback;
        
        //If it's an update
        if(opt.isPresent()){
            validator  = validatorFactory.getSync().createValidatorFor(updatedEntity, opt.get(), DefaultValidatorConfig.METHOD_TYPE.UPDATE, cat);
            response = createValidationResponse(updatedEntity, opt.orElse(null), DefaultValidatorConfig.METHOD_TYPE.UPDATE);
            callback = createCallbackFor(updatedEntity, response, DefaultValidatorConfig.METHOD_TYPE.UPDATE);
            validator.validate(updatedEntity, opt.orElse(null), callback);

        //If it's new (insert)
        }else{
            validator  = validatorFactory.getSync().createValidatorFor(updatedEntity, null, DefaultValidatorConfig.METHOD_TYPE.CREATE, cat);
            response = createValidationResponse(updatedEntity, opt.orElse(null), DefaultValidatorConfig.METHOD_TYPE.CREATE);
            callback = createCallbackFor(updatedEntity, response, DefaultValidatorConfig.METHOD_TYPE.CREATE);
            validator.validate(updatedEntity, opt.orElse(null), callback);
        }
        callback.complete();

        //always send 200 even if validation has errors?
        return response;

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
