package ix.core.models;

import javax.persistence.*;

@Entity
@Table(name="ix_core_namespace")
@SequenceGenerator(name = "LONG_SEQ_ID", sequenceName = "ix_core_namespace_seq", allocationSize = 1)
public class Namespace extends LongBaseModel {
    public enum Modifier {
        Public, // anyone can access this resource
            Internal, // only authenticated users have access
            Private // only specific users have access
            }
//    @Id
//    @GeneratedValue(generator = "ix_core_namespace_seq")
//    public Long id;

    @Column(unique=true)
    @Indexable(facet=true,name="Namespace")
    public String name;
    
    @ManyToOne(cascade = CascadeType.PERSIST)
    public Principal owner;

    @Column(length=1024)
    public String location; // url, path, etc.
    public Modifier modifier = Modifier.Private;

    public Namespace() {}
    public Namespace(Modifier modifier) {
        this.modifier = modifier;
    }
    public Namespace(String name, Modifier modifier) {
        this.name = name;
        this.modifier = modifier;
    }

    public boolean isPublic () { 
        return modifier == Modifier.Public; 
    }

    public static Namespace newPublic () {
        return new Namespace(Modifier.Public);
    }
    public static Namespace newPublic (String name) {
        return new Namespace(name, Modifier.Public);
    }
    public static Namespace newInternal () {
        return new Namespace(Modifier.Internal);
    }
    public static Namespace newInternal (String name) {
        return new Namespace(name, Modifier.Internal);
    }
    public static Namespace newPrivate () {
        return new Namespace(Modifier.Private);
    }
    public static Namespace newPrivate (String name) {
        return new Namespace(name, Modifier.Private);
    }
}
