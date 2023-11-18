package gsrs.service;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.JsonNode;
import gov.nih.ncats.common.util.CachedSupplierGroup;
import gsrs.EntityPersistAdapter;
import gsrs.controller.OffsetBasedPageRequest;
import gsrs.security.*;
import ix.core.validator.ValidationResponse;
import ix.core.validator.ValidatorCategory;
import lombok.Builder;
import lombok.Data;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.io.IOException;
import java.util.Optional;

/**
 * Contains all the business logic for converting JSON into an Entity, reading and writing
 *  entities to their repository as well as any validation logic.
 * This decouples this business logic from the controller
 * so we can change the controller without touching the business logic,
 * allow for multiple ways software can interact with a GSRSEntityService
 * and finally to ease testing by being able to test the business logic
 * without the need for standing up a full server/controller and
 * to test the controller with a mock service.
 * @param <T>
 * @param <I>
 */
public interface GsrsEntityService<T, I> {
    /**
     * A {@link CachedSupplierGroup} for all CachedSuppliers used by EntityServices
     * that should be reset all together at particular times (for example at the before a
     * tests).
     */
    CachedSupplierGroup ENTITY_SERVICE_INTIALIZATION_GROUP = new CachedSupplierGroup();
    /**
     * Get the API Context for this Entity Service.
     * @return the Context; should never be null or empty;
     */
    String getContext();
    /**
     * Get the number of entities in your data repository.
     * @return a number &ge;0.
     */
    long count();


    /**
     * Remove the given entity from the repository.
     * @param id the id of the entity to delete.
     */
    @hasUpdateRole
    void delete(I id);

    /**
     * Get the Class of the Entity.
     * @return a Class; will never be {@code null}.
     */
    Class<T> getEntityClass();

    /**
     * Return a {@link Page} of entities from the repository using the
     * given {@link Pageable} which often contains subset limitations as such as: offset, num records and sort order.
     * @param pageable the {@link Pageable to fetch from the repository.}
     * @return the Page from the repository.
     *
     * @see OffsetBasedPageRequest
     */
    Page page(Pageable pageable);
    @hasDataEntryRole
    CreationResult<T> createEntity(JsonNode newEntityJson, boolean partOfBatchLoad) throws IOException;

    @hasDataEntryRole
    default CreationResult<T> createEntity(JsonNode newEntityJson) throws IOException{
        return createEntity(newEntityJson, false);
    }

    @hasUpdateRole
    UpdateResult<T> updateEntity(T updatedEntity, EntityPersistAdapter.ChangeOperation<T> changeOperation) throws Exception;

    @hasUpdateRole
    UpdateResult<T> updateEntity(JsonNode updatedEntityJson) throws Exception;

    default ValidationResponse<T> validateEntity(JsonNode updatedEntityJson) throws Exception{
        return this.validateEntity(updatedEntityJson, ValidatorCategory.CATEGORY_ALL());        
    }
    
    ValidationResponse<T> validateEntity(JsonNode updatedEntityJson, ValidatorCategory cat) throws Exception;

    Optional<T> getEntityBySomeIdentifier(String id);

    Optional<I> getEntityIdOnlyBySomeIdentifier(String id);

    @Data
    @Builder
    class CreationResult<T>{
        private boolean created;
        private ValidationResponse<T> validationResponse;
        private T createdEntity;
        private Throwable throwable;
    }

    @Data
    @Builder
    class UpdateResult<T>{
        public enum STATUS{
            NOT_FOUND,
            UPDATED,
            ERROR;
        }
        private Throwable throwable;
        private STATUS status;
        private ValidationResponse<T> validationResponse;
        private T updatedEntity;
        @JsonIgnore
        private String oldJson;
    }

    @Data
    @Builder
    class ProcessResult<T>{
        private boolean saved;
        private ValidationResponse<T> validationResponse;
        private T entity;
        private Throwable throwable;
        private Object entityId;

        public static ProcessResult ofCreation(CreationResult update) {
            ProcessResultBuilder builder = ProcessResult.builder();
            return builder.validationResponse(update.validationResponse)
                    .entity(update.createdEntity)
                    .throwable(update.throwable)
                    .saved(update.created)
                    .build();
        }
        public static ProcessResult ofUpdate(UpdateResult update) {
            ProcessResultBuilder builder = ProcessResult.builder();
            builder.throwable(update.throwable)
                    .saved(update.status== UpdateResult.STATUS.UPDATED)
                    .entity(update.updatedEntity)
                    .validationResponse(update.validationResponse);

            return builder.build();
        }
    }
}
