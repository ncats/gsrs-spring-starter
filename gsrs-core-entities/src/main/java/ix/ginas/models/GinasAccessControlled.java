package ix.ginas.models;



import ix.core.models.Group;

import java.util.Set;

/**
 * Locks down a ginas object so that only
 * users who belong to particular {@link Group}s
 * can access this object.
 */
public interface GinasAccessControlled {
	Set<Group> getAccess();
	void setAccess(Set<Group> access);
	void addRestrictGroup(Group p);
	//FIXME katzelda Feb 2021: removed adding group by String name because it requires accessing repository in model layer
//	void addRestrictGroup(String group);
	
}
