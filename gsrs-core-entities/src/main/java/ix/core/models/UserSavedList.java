package ix.core.models;

import java.util.Objects;

import jakarta.persistence.Basic;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Lob;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

@Entity
@Table(name = "ix_core_user_saved_list", 
	uniqueConstraints={@UniqueConstraint(columnNames={"name", "user_id", "kind"})})
@Indexable(indexed = false)
public class UserSavedList {
	
	@Id
	@Column(unique = true)
	@GeneratedValue(strategy = GenerationType.AUTO, generator = "LONG_SEQ_ID")
	private Long id;
	
	@ManyToOne  
	@JoinColumn(name="user_id")
	public Principal principal;

	@Column(nullable=false)
	public String name; 
	
	private String kind;
	
	@Lob
	@Basic(fetch= FetchType.EAGER)
	public String list;	
	
	public UserSavedList() {}
	
	public UserSavedList(Principal user, String name, String list, String kind) {
        this.principal = user;
        this.name = name;        
        this.list = list;
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
		return (id != null && id.equals (((UserSavedList)o).id));
	}
	
	@Override
	public int hashCode() {
		return Objects.hashCode(id);
	}
}
