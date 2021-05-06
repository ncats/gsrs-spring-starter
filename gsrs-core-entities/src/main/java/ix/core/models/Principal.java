package ix.core.models;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import gov.nih.ncats.common.util.TimeUtil;
import gsrs.model.AbstractNonAuditingGsrsEntity;
import ix.ginas.models.serialization.GsrsDateDeserializer;
import ix.ginas.models.serialization.GsrsDateSerializer;

import javax.persistence.*;
import javax.validation.constraints.Email;
import java.util.Date;

@Entity
@Table(name="ix_core_principal")
@Inheritance
@DiscriminatorValue("PRI")
public class Principal extends AbstractNonAuditingGsrsEntity implements FetchableEntity{
    @Id
    //Ebean added GeneratedValue by default we have to be explicit in hibernate
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "ix_core_principal_seq" )
    public Long id;
    @Version
    public Long version;

    @JsonSerialize(using = GsrsDateSerializer.class)
    @JsonDeserialize(using = GsrsDateDeserializer.class)
    public Date created = TimeUtil.getCurrentDate();

    @JsonSerialize(using = GsrsDateSerializer.class)
    @JsonDeserialize(using = GsrsDateDeserializer.class)
    public Date modified;

    public boolean deprecated;



    @PrePersist
    private void markCreated(){
        Date date =TimeUtil.getCurrentDate();
        created = date;
        modified= date;
    }
    @PreUpdate
    private void markUpdated(){
        Date date =TimeUtil.getCurrentDate();
        modified= date;
    }

    @Override
    public String fetchGlobalId() {
        if(id!=null)return this.getClass().getName() + ":" + id.toString();
        return null;
    }
    // provider of this principal
    public String provider; 
    
   // @Required
    @Indexable(facet=true,name="Principal")
    @Column(unique=true)
    public String username;

    @Email
    public String email;
    
    @Column(name = "is_admin")
    public boolean admin = false;

    @Column(length=1024)
    public String uri; // can be email or any unique uri

    @ManyToOne(cascade = CascadeType.PERSIST)
    public Figure selfie;

    public Principal() {}
    
    public Principal(boolean admin) {
        this.admin = admin;
    }
    public Principal(String email) {
        this.email = email;
    }
    public Principal(boolean admin, String email) {
        this.admin = admin;
        this.email = email;
    }
    public Principal(String username, String email) {
        this.username = username;
        this.email = email;
    }
    
    @JsonIgnore
    public String toString(){
    	return username;
    }
    //TODO katzelda Octobe 2020 : don't think we need this userprofile factory call? its used in a few places in GSRS 2.x but in all cases we could use a repository instead?
    /*
    @JsonIgnore
    public UserProfile getUserProfile(){
    	return UserProfileFactory.getUserProfileForPrincipal(this);
    }
*/
    public boolean isAdmin () {
    	return admin; 
    }

    
}
