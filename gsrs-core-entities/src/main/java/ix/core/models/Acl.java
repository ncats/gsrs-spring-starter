package ix.core.models;

import jakarta.persistence.*;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name="ix_core_acl")
//GSRS 2.x had distinct
@SequenceGenerator(name = "LONG_SEQ_ID", sequenceName = "ix_core_acl_seq", allocationSize = 1)
public class Acl extends LongBaseModel {

    public enum Permission {
        None, // 
            Read, // read-only
            Write, // write-only
            ReadWrite, // read+write
            Execute, // execute
            Admin,
            Owner
            }

    public Permission perm = Permission.Read;

    @ManyToMany(cascade= CascadeType.ALL)
    @Basic(fetch= FetchType.EAGER)
//    @JoinTable(name="ix_core_acl_principal")
    @JoinTable(name="ix_core_acl_principal", inverseJoinColumns = {
            @JoinColumn(name="ix_core_principal_id")
    })
    public List<Principal> principals = new ArrayList<Principal>();

    @ManyToMany(cascade= CascadeType.ALL)
    @Basic(fetch= FetchType.EAGER)
    @JoinTable(name="ix_core_acl_group", inverseJoinColumns = {
            @JoinColumn(name="ix_core_group_id")
    })
    public List<Group> groups = new ArrayList<Group>();

    public Acl() {}
    public Acl(Permission perm) {
        this.perm = perm;
    }

    public static Acl newNone () {
        return new Acl(Permission.None);
    }
    public static Acl newRead () {
        return new Acl(Permission.Read);
    }
    public static Acl newWrite () {
        return new Acl(Permission.Write);
    }
    public static Acl newReadWrite () {
        return new Acl(Permission.ReadWrite);
    }
    public static Acl newExecute () {
        return new Acl(Permission.Execute);
    }
    public static Acl newAdmin () {
        return new Acl(Permission.Admin);
    }

    public String getValue()
    {
        return perm.toString();
    }

    public static List<String> options(){
        List<String> vals = new ArrayList<String>();
        for (Permission permission: Permission.values()) {
            vals.add(permission.name());
        }
        return vals;
    }
    
}

