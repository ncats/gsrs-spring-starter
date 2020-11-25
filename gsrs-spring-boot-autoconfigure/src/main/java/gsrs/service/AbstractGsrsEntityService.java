package gsrs.service;

import com.fasterxml.jackson.databind.JsonNode;
import gov.nih.ncats.common.util.CachedSupplier;
import gsrs.controller.GsrsControllerConfiguration;
import gsrs.controller.IdHelper;
import gsrs.controller.OffsetBasedPageRequest;
import gsrs.validator.GsrsValidatorFactory;
import gsrs.validator.ValidatorConfig;
import ix.core.validator.ValidationResponse;
import ix.core.validator.Validator;
import ix.ginas.utils.validation.ValidatorFactory;
import ix.utils.pojopatch.PojoDiff;
import ix.utils.pojopatch.PojoPatch;
import lombok.Builder;
import lombok.Data;
import org.hibernate.sql.Update;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

import javax.annotation.PostConstruct;
import javax.transaction.Transactional;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public abstract class AbstractGsrsEntityService<T,I> {

    @Autowired
    private GsrsValidatorFactory validatorFactoryService;


    private final String context;
    private final Pattern idPattern;

    private CachedSupplier<ValidatorFactory> validatorFactory;

    public AbstractGsrsEntityService(String context, Pattern idPattern) {
        this.context = context;
        this.idPattern = idPattern;
    }
    public AbstractGsrsEntityService(String context, IdHelper idHelper) {
        this(context, Pattern.compile("^"+idHelper.getRegexAsString() +"$"));
    }

    @PostConstruct
    private void initValidator(){
        //need this in a post construct so the validator factory service is injected
        validatorFactory = CachedSupplier.runOnce(()->validatorFactoryService.newFactory(context));
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
     * Get the number of entities in your data repository.
     * @return a number &ge;0.
     */
    public abstract long count();

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
     * Fetch an entity from the data repository using a unique
     * identifier that isn't the entity's ID.
     *
     * @param someKindOfId a String that isn't the ID of the entity.
     * @return an Optional that is empty if there is no entity with the given id;
     *  or an Optional that has the found entity.
     */
    public abstract Optional<T> flexLookup(String someKindOfId);

    /**
     * Get the Class of the Entity.
     * @return a Class; will never be {@code null}.
     */
    public abstract Class<T> getEntityClass();

    /**
     * Return a {@link Page} of entities from the repository using the
     * given offset, num records and sort order.
     * @param offset the number of records to start at ( 0-based).
     * @param numOfRecords the number of records to include in the page.
     * @param sort the {@link Sort} to use.
     * @return the Page from the repository.
     *
     * @see OffsetBasedPageRequest
     */
    public abstract Page page(long offset, long numOfRecords, Sort sort);

    /**
     * Remove the given entity from the repository.
     * @param id the id of the entity to delete.
     */
    public abstract void delete(I id);

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
        return builder.createdEntity(persist(newEntity))
                                .created(true)
                                .build();

    }


    @Transactional
    protected T persist(T newEntity){
        return create(newEntity);
    }

    @Data
    @Builder
    public static class CreationResult<T>{
        private boolean created;
        private ValidationResponse<T> validationResponse;
        private T createdEntity;
    }

    @Data
    @Builder
    public static class UpdateResult<T>{
        public enum STATUS{
            NOT_FOUND,
            UPDATED,
            ERROR;
        }
        private STATUS status;
        private ValidationResponse<T> validationResponse;
        private T updatedEntity;
    }

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

    @Transactional
    protected T transactionalUpdate(T entity){
        return update(entity);
    }

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

    public Optional<T> getEntityBySomeIdentifier(String id) {
        Optional<T> opt;
        if(isId(id)) {
            opt = get(parseIdFromString(id));
        }else {
            opt = flexLookup(id);
        }
        return opt;
    }
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
}
