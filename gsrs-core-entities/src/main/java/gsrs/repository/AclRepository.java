package gsrs.repository;

import ix.core.models.Acl;
import ix.core.models.Principal;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
@Repository
public interface AclRepository extends JpaRepository<Acl, Long> {
      /*
    @Override
	public List<Acl> getPermissions() {
		return AdminFactory.permissionByPrincipal(user); // return permissions;
	}

	public List<Group> getGroups() {
		return AdminFactory.groupsByPrincipal(user);
	}
     */

      List<Acl> getPermissionsByPrincipals(Principal principal);
}
