package ix.core.models;

import java.util.Objects;

import javax.persistence.Basic;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.Lob;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;

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
