package ix.ginas.models;

import com.fasterxml.jackson.annotation.JsonIgnore;

import ix.core.models.*;

import jakarta.persistence.*;
import java.util.*;

/**
 * Base class for all Ginas Model objects, contains all fields
 * common to all including UUID, and audit information.
 */
@MappedSuperclass
public class GinasCommonData extends NoIdGinasCommonData{

    @Id
    //commented these 2 annotations out to get Substance tests to pass. 20 October 2025
    // this necessitates manually setting a value for UUID for all objects created.
    //TODO: reevaluate this strategy!
    //@GenericGenerator(name = "NullUUIDGenerator", type = ix.ginas.models.generators.NullUUIDGenerator.class)
    //@GeneratedValue(generator = "NullUUIDGenerator")
    public UUID uuid;
    @Indexable()
    public UUID getUuid() {
        return uuid;
    }

    public void setUuid(UUID uuid) {
        if (this.uuid == null) {
            this.uuid = uuid;
        }
    }

    @JsonIgnore
    public UUID getOrGenerateUUID() {
        if (getUuid() != null) return uuid;
        this.uuid = UUID.randomUUID();
        return uuid;
    }
    @Override
    public String fetchGlobalId() {
        if (this.uuid == null) return null;
        return this.uuid.toString();
    }
    public boolean equals(Object o) {
        if (o == null) return false;
        if (!(o instanceof GinasCommonData)) {
            return false;
        }
        GinasCommonData g = (GinasCommonData) o;
        //change to getOrGenerate since if the uuid isn't set null will be considered equals
        //this caused problems in GSRS 2.x when we had validation rules that did List.remove( object)
        // if multiple objects had unset uuids we would accidntally remove the wrong one!!
        return Objects.equals(getOrGenerateUUID(), g.getOrGenerateUUID());
//		if(!(this.uuid+"").equals(g.uuid+"")){
//			return false;
//		}
//		return true;

    }

    public int hashCode(){
        return getOrGenerateUUID().hashCode();
    }
}
