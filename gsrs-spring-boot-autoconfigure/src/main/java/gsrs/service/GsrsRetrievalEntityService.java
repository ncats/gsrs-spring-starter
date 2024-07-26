package gsrs.service;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.JsonNode;
import gov.nih.ncats.common.util.CachedSupplierGroup;
import gsrs.controller.*;
import ix.core.util.EntityUtils;
import ix.core.validator.ValidationResponse;
import ix.core.validator.ValidatorCategory;
import lombok.Builder;
import lombok.Data;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

import javax.servlet.http.HttpServletRequest;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public interface GsrsRetrievalEntityService<T, I> {
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

    default List<EntityUtils.Key> getKeys(){
        return new ArrayList<EntityUtils.Key>();
    }

    default List<I> getIDs(){
        return new ArrayList<I>();
    }

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

    Optional<T> getEntityBySomeIdentifier(String id);

    Optional<I> getEntityIdOnlyBySomeIdentifier(String id);

}

