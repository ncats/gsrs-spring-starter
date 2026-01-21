package ix.ginas.models;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import ix.core.models.Group;
import ix.ginas.models.serialization.GroupDeserializer;
import ix.ginas.models.serialization.GroupSerializer;

import jakarta.persistence.*;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Set;

//Note: this is NOT an entity anymore. GSRS 2.x decided to store
//access container data as JSON not a separate column...
public class GinasAccessContainer {
	
	@Id
	public Long id;
	
	@ManyToMany(cascade = CascadeType.ALL)
	@JsonSerialize(contentUsing = GroupSerializer.class)
	@JsonDeserialize(contentUsing = GroupDeserializer.class)
	private Set<Group> access;
	
	
	public String entityType;
	
	public void add(Group p) {
		if (access == null) {
			access = new LinkedHashSet<Group>();
		}
		access.add(p);
	}
	
	public GinasAccessContainer(){
		
	}


    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }
    
    public GinasAccessContainer(Object o){
		this.entityType=o.getClass().getName();
	}

	public Set<Group> getAccess() {
		if (access == null) {
			return new LinkedHashSet<Group>();
		}
		return access;
	}
	public void setAccess(Collection<Group> acc){
		this.access=new LinkedHashSet<Group>(acc);
	}


	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (!(o instanceof GinasAccessContainer)) return false;
		GinasAccessContainer that = (GinasAccessContainer) o;
		return Objects.equals(id, that.id) &&
				Objects.equals(access, that.access) &&
				Objects.equals(entityType, that.entityType);
	}

	@Override
	public int hashCode() {
		return Objects.hash(id, access, entityType);
	}
}
