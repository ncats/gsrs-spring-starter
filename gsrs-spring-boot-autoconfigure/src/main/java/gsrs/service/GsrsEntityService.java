package gsrs.service;

import com.fasterxml.jackson.databind.JsonNode;
import gsrs.controller.OffsetBasedPageRequest;
import ix.core.validator.ValidationResponse;
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

    CreationResult<T> createEntity(JsonNode newEntityJson) throws IOException;

    UpdateResult<T> updateEntity(JsonNode updatedEntityJson) throws Exception;

    ValidationResponse<T> validateEntity(JsonNode updatedEntityJson) throws Exception;

    Optional<T> getEntityBySomeIdentifier(String id);

    Optional<I> getEntityIdOnlyBySomeIdentifier(String id);

    @Data
    @Builder
    class CreationResult<T>{
        private boolean created;
        private ValidationResponse<T> validationResponse;
        private T createdEntity;
    }

    @Data
    @Builder
    class UpdateResult<T>{
        public enum STATUS{
            NOT_FOUND,
            UPDATED,
            ERROR;
        }
        private STATUS status;
        private ValidationResponse<T> validationResponse;
        private T updatedEntity;
    }
}
