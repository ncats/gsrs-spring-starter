package gsrs.service;

import com.fasterxml.jackson.databind.JsonNode;
import gov.nih.ncats.common.util.CachedSupplier;
import gsrs.controller.IdHelper;
import gsrs.validator.GsrsValidatorFactory;
import gsrs.validator.ValidatorConfig;
import ix.core.validator.ValidationResponse;
import ix.core.validator.Validator;
import ix.ginas.utils.validation.ValidatorFactory;
import ix.utils.pojopatch.PojoDiff;
import ix.utils.pojopatch.PojoPatch;
import org.springframework.beans.factory.annotation.Autowired;

import javax.annotation.PostConstruct;
import javax.transaction.Transactional;
import java.io.IOException;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Abstract implementation of {@link GsrsEntityService} to reduce most
 * of the boilerplate of creating a {@link GsrsEntityService}.
 * @param <T> the entity type.
 * @param <I> the type for the entity's ID.
 */
public abstract class AbstractGsrsEntityService<T,I> implements GsrsEntityService<T, I> {

    @Autowired
    private GsrsValidatorFactory validatorFactoryService;


    private final String context;
    private final Pattern idPattern;

    private CachedSupplier<ValidatorFactory> validatorFactory;
    /**
     * Create a new GSRS Entity Service with the given context.
     * @param context the context for the routes of this controller.
     * @param idPattern the {@link Pattern} to match an ID.  This will be used to determine
     *                  Strings that are IDs versus Strings that should be flex looked up.  @see {@link #flexLookup(String)}.
     * @throws NullPointerException if any parameters are null.
     */
    public AbstractGsrsEntityService(String context, Pattern idPattern) {
        this.context = Objects.requireNonNull(context, "context can not be null");
        this.idPattern = Objects.requireNonNull(idPattern, "ID pattern can not be null");
    }
    /**
    * Create a new GSRS Entity Service with the given context.
    * @param context the context for the routes of this controller.
    * @param idHelper the {@link IdHelper} to match an ID.  This will be used to determine
    *                  Strings that are IDs versus Strings that should be flex looked up.  @see {@link #flexLookup(String)}.
    * @throws NullPointerException if any parameters are null.
    */
    public AbstractGsrsEntityService(String context, IdHelper idHelper) {
        this(context, Pattern.compile("^"+idHelper.getRegexAsString() +"$"));
    }

    @Override
    public String getContext() {
        return context;
    }

    @PostConstruct
    private void initValidator(){
        //need this in a post construct so the validator factory service is injected
        validatorFactory = CachedSupplier.of(()->validatorFactoryService.newFactory(context));
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
     * Perform any object clean up or "data cleansing" here.
     *
     * @param list the JSON to use; will never be null.
     *
     * @return a new List of instances will never be null and probably shouldn't be empty.
     *
     * @throws IOException if there's a problem processing the JSON.
     */
    protected abstract List<T> fromNewJsonList(JsonNode list) throws IOException;
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
     * Perform any object clean up or "data cleansing" here  here but keep in mind different data cleansings
     * might be done on new vs updated records.
     *
     * @param list the JSON to use; will never be null.
     *
     * @return a new List of instances will never be null and probably shouldn't be empty.
     *
     * @throws IOException if there's a problem processing the JSON.
     */
    protected abstract List<T> fromUpdatedJsonList(JsonNode list) throws IOException;

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
    public  CreationResult<T> createEntity(JsonNode newEntityJson) throws IOException {
        T newEntity = fromNewJson(newEntityJson);

        Validator<T> validator  = validatorFactory.getSync().createValidatorFor(newEntity, null, ValidatorConfig.METHOD_TYPE.CREATE);
        ValidationResponse<T> resp = validator.validate(newEntity, null);
        CreationResult.CreationResultBuilder<T> builder = CreationResult.<T>builder()
                                                                            .validationResponse(resp);

        if(resp!=null && !resp.isValid()){

            return builder
                    .build();
        }
        return builder.createdEntity(transactionalPersist(newEntity))
                                .created(true)
                                .build();

    }

    /**
     * Persist the given entity inside a transaction.
     * This method should NOT be overridden by client code
     * and is only here so Spring AOP can subclass it to make it transactional.
     * @param newEntity the entity to persist.
     * @return the persisted entity.
     * @implSpec delegates to {@link #create(Object)} inside a {@link Transactional}.
     */
    @Transactional
    protected T transactionalPersist(T newEntity){
        return create(newEntity);
    }

    @Override
    public UpdateResult<T> updateEntity(JsonNode updatedEntityJson) throws Exception {
        T updatedEntity = fromUpdatedJson(updatedEntityJson);
        //updatedEntity should have the same id
        I id = getIdFrom(updatedEntity);
        Optional<T> opt = get(id);
        if(!opt.isPresent()){
            return UpdateResult.<T>builder().status(UpdateResult.STATUS.NOT_FOUND).build();
        }

        T oldEntity = opt.get();

        Validator<T> validator  = validatorFactory.getSync().createValidatorFor(updatedEntity, oldEntity, ValidatorConfig.METHOD_TYPE.CREATE);
        ValidationResponse<T> resp = validator.validate(updatedEntity, oldEntity);
        UpdateResult.UpdateResultBuilder<T> builder = UpdateResult.<T>builder()
                                                            .validationResponse(resp);
        if(resp!=null && !resp.isValid()){
            return builder.status(UpdateResult.STATUS.ERROR)
                    .build();
        }

        PojoPatch<T> patch = PojoDiff.getDiff(oldEntity, updatedEntity);
        System.out.println("changes = " + patch.getChanges());
        patch.apply(oldEntity);
        System.out.println("updated entity = " + oldEntity);
        try {
            builder.updatedEntity(transactionalUpdate(oldEntity));
            builder.status(UpdateResult.STATUS.UPDATED);
        }catch(Throwable t){
            builder.status(UpdateResult.STATUS.ERROR);
        }
        //match 200 status of old GSRS
        return builder.build();
    }
    /**
     * Update the given entity inside a transaction.
     * This method should NOT be overridden by client code
     * and is only here so Spring AOP can subclass it to make it transactional.
     * @param entity the entity to update.
     * @return the persisted entity.
     * @implSpec delegates to {@link #update(Object)} inside a {@link Transactional}.
     */
    @Transactional
    protected T transactionalUpdate(T entity){
        return update(entity);
    }

    @Override
    public ValidationResponse<T> validateEntity(JsonNode updatedEntityJson) throws Exception {
        T updatedEntity = fromUpdatedJson(updatedEntityJson);
        //updatedEntity should have the same id
        I id = getIdFrom(updatedEntity);

        Optional<T> opt = id==null? Optional.empty() : get(id);
        Validator<T> validator;
        if(opt.isPresent()){
            validator  = validatorFactory.getSync().createValidatorFor(updatedEntity, opt.get(), ValidatorConfig.METHOD_TYPE.UPDATE);

        }else{
            validator  = validatorFactory.getSync().createValidatorFor(updatedEntity, null, ValidatorConfig.METHOD_TYPE.CREATE);

        }
        ValidationResponse<T> resp = validator.validate(updatedEntity, opt.orElse(null));

        //always send 200 even if validation has errors?
        return resp;

    }

    private boolean isId(String s){
        Matcher idMatch = idPattern.matcher(s);
        return idMatch.matches();
    }

    @Override
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
    public abstract Optional<T> flexLookup(String someKindOfId);
}
