package ix.core.models;

import java.util.ArrayList;
import java.util.List;

public enum Role {
        Query,
        DataEntry,
        SuperDataEntry,
        Updater,
        SuperUpdate,
        Approver,
        Admin;
        //Guest, Owner, Admin, User; //authenticated user


        public static List<Role> options(){
            List<Role> vals = new ArrayList<Role>();
            for (Role role: Role.values()) {
                vals.add(role);
            }
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

