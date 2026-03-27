package ix.core.models;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import gov.nih.ncats.common.util.TimeUtil;
import gsrs.model.AbstractNonAuditingGsrsEntity;
import ix.ginas.models.serialization.GsrsDateDeserializer;
import ix.ginas.models.serialization.GsrsDateSerializer;

import jakarta.persistence.*;
import jakarta.validation.constraints.Email;

import java.util.Date;

@Entity
@Table(name="ix_core_principal")
@Inheritance
@DiscriminatorValue("PRI")
@SequenceGenerator(name = "ix_core_principal_seq", sequenceName = "ix_core_principal_seq", allocationSize = 1)
public class Principal extends AbstractNonAuditingGsrsEntity implements FetchableEntity{
    @Id
    //Ebean added GeneratedValue by default we have to be explicit in hibernate
    @GeneratedValue(strategy = GenerationType.AUTO, generator = "ix_core_principal_seq")
    public Long id;
    @Version
    public Long version = 0L;

    @JsonSerialize(using = GsrsDateSerializer.class)
    @JsonDeserialize(using = GsrsDateDeserializer.class)
    public Date created = TimeUtil.getCurrentDate();

    @JsonSerialize(using = GsrsDateSerializer.class)
    @JsonDeserialize(using = GsrsDateDeserializer.class)
    public Date modified;

    public boolean deprecated;

    /**
     * Return the standardized username.
     * @return a String or {@code null} if username is {@code null}.
     */
    public String computeStandardizedName() {
        if(this.username ==null){
            return null;
        }
        return this.username.toUpperCase();
    }

    @PrePersist
    private void markCreated(){
        Date date =TimeUtil.getCurrentDate();
        created = date;
        modified= date;
        this.username = computeStandardizedName();
    }
    @PreUpdate
    private void markUpdated(){
        Date date =TimeUtil.getCurrentDate();
        modified= date;
        this.username = computeStandardizedName();
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
    /**
     * @deprecated admin field is deprecated since 3.0 use Roles instead.
     */
    @Deprecated
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

    /**
     * Create a new Principal object with a standardized username.
     *
     * @param username
     * @param email
     * @return
     */
    public static Principal createStandardized(String username, String email){
        Principal p = new Principal(username, email);
        if(username !=null) {
            p.username = p.computeStandardizedName();
        }
        return p;

    }

    /**
     * Standardize this principal object and update the fields
     * and return the updated principal.  Update here does not imply
     * it is persisted anywhere just that the fields are updated.
     * @return a Principal, usually {@code this} but might not always be.
     */
    public Principal standardizeAndUpdate(){
        this.username = computeStandardizedName();
        return this;
    }
    @JsonIgnore
    public String toString(){
    	return this.computeStandardizedName();
    }

    public boolean isAdmin () {
    	return admin; 
    }
}
