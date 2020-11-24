package gsrs.startertests.controller;

import gsrs.model.AbstractGsrsEntity;
import lombok.Data;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Version;
import java.util.Date;
import java.util.UUID;

@Entity
@Data
class MyEntity extends AbstractGsrsEntity {
    @Id
    @GeneratedValue
    private UUID uuid;

    private String foo;

    @CreatedDate
    private Date created;
    @LastModifiedDate
    private Date modified;

    @Version
    private int version=1;



}
