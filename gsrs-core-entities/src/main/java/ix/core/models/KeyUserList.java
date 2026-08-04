package ix.core.models;

import java.util.Objects;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

@Entity
@Table(name = "ix_core_key_user_list", 
	uniqueConstraints={@UniqueConstraint(columnNames={"entity_key", "list_name", "user_id", "kind"})})
@Indexable(indexed = false)
public class KeyUserList {
	
	@Id
	@Column(unique = true)
	@GeneratedValue(strategy = GenerationType.AUTO, generator = "LONG_SEQ_ID")
	public Long id;
	
	@Column(name = "entity_key")
	public String entityKey;	
	
	@ManyToOne   
	@JoinColumn(name = "user_id")
	public Principal principal;

	@Column(nullable = false, name = "list_name")
	public String listName;	
	
	private String kind;
	
	public KeyUserList() {}
	
	public KeyUserList(String key, Principal user, String name, String kind) {
		this.entityKey = key;
        this.principal = user;
        this.listName = name;
        this.kind = kind;
    }	
	
	@Override
	public boolean equals(Object o) {
		if(o == null)
			return false;
		if(this == o)
			return true;
		if(getClass() != o.getClass())
			return false;
		return (id != null && id.equals (((KeyUserList)o).id));
	}
	
	@Override
	public int hashCode() {
		return Objects.hashCode(id);
	}
}
