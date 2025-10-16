package ix.core.models;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

import jakarta.persistence.*;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

/**
 * A group of users usually
 * a way to provide access controls
 * on which groups can view/edit ginas objects.
 */
@Entity
@Table(name="ix_core_group")
@SequenceGenerator(name = "LONG_SEQ_ID", sequenceName = "ix_core_group_seq", allocationSize = 1)
public class Group extends LongBaseModel {

    @Column(unique=true)
    public String name;

    @ManyToMany(cascade= CascadeType.ALL)
    @Basic(fetch= FetchType.EAGER)
    @JoinTable(name="ix_core_group_principal", inverseJoinColumns = {
            @JoinColumn(name="ix_core_principal_id")
    })
    @JsonIgnore
    public Set<Principal> members = new HashSet<>();

    public Group(){
        //required for hibernate?
    }
    @JsonIgnore
    public Set<Principal> getMembers(){
        return members;
    }
    public void addMember(Principal p){
        getMembers().add(Objects.requireNonNull(p));
        setIsDirty("members");
    }
    public void removeMember(Principal p){
        Objects.requireNonNull(p);
        getMembers().remove(p);
        setIsDirty("members");
    }
   @JsonCreator
    public Group(@JsonProperty("name") String name) {
        this.name = name;
    }
    
    
    public int hashCode(){
    	return this.name.hashCode();
    }
    public boolean equals(Object o){
    	if(o!=null && o instanceof Group){
    		return this.name.equals(((Group)o).name);
    	}
    	return false;
    }
}
