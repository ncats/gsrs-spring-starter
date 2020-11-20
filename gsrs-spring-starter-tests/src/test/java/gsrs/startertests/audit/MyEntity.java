package gsrs.startertests.audit;

import gsrs.model.AbstractGsrsEntity;
import ix.core.models.Principal;
import lombok.Data;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.LastModifiedBy;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;

@Entity
@Data
public class MyEntity extends AbstractGsrsEntity {
    @Id
    @GeneratedValue
    private Long id;

    @CreatedBy
    private Principal createdBy;

    @LastModifiedBy
    private Principal lastModifiedBy;

    private String foo;

}
