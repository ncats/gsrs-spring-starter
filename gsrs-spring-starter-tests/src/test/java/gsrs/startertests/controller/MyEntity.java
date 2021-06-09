package gsrs.startertests.controller;

import gsrs.model.AbstractGsrsEntity;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;

import javax.persistence.*;
import java.util.Date;
import java.util.UUID;

@Entity
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MyEntity extends AbstractGsrsEntity {
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



    @PostLoad
    public void postLoad(){
        System.out.println("here!!!");
    }
}
