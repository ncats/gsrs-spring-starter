package ix.core.models;

import gsrs.services.PrivilegeService;

import java.util.ArrayList;
import java.util.List;

public class Role {

    public Role() {}

    public static Role of(String roleName){
        return new Role(roleName);
    }

    public Role(String roleValue) {
        this.role = roleValue;
    }

    private String role;

    public String getRole() {
        return this.role;
    }

    public void setRole(String roleValue) {
        this.role = roleValue;
    }

    private PrivilegeService privilegeService = PrivilegeService.instance();

        public static List<Role> options(){
            List<Role> vals = new ArrayList<Role>();
            PrivilegeService.instance().getAllRoleNames().forEach(r->{
                vals.add(new Role(r));
            });
            return vals;
        }
        
        public static List<Role> roles(Role... roles ){
        	List<Role> rolelist= new ArrayList<Role>();
        	for(Role r:roles){
        		rolelist.add(r);
        	}
        	return rolelist;
        }

}