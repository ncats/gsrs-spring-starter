package gsrs.startertests.controller;

import gsrs.model.AbstractGsrsEntity;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;

import jakarta.persistence.*;
import java.util.Date;
import java.util.UUID;

@Entity
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper=false)
public class MyEntity extends AbstractGsrsEntity {
    @Id
    @GeneratedValue
    @Column(columnDefinition="UUID")
    private UUID uuid;

    private String foo;

    @CreatedDate
    private Date created;
    @LastModifiedDate
    private Date modified;

    @Version
    @Builder.Default
    private int version=1;

}
