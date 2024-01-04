package gsrs.startertests.userSupport;

import gsrs.model.AbstractGsrsEntity;
import ix.core.models.Principal;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.LastModifiedBy;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.OneToOne;

@Data
@Entity
@EqualsAndHashCode(callSuper=false)
public class EntityWithUser extends AbstractGsrsEntity {

    @Id
    @GeneratedValue
    private Long id;

    private String foo;

    @OneToOne
    @CreatedBy
    private Principal createdBy;
    @OneToOne
    @LastModifiedBy
    private Principal modifiedBy;
}
