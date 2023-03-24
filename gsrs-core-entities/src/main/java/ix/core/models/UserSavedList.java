package ix.core.models;

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
	uniqueConstraints={@UniqueConstraint(columnNames={"name", "user_id"})})
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
	
	@Lob
    @Basic(fetch= FetchType.EAGER)	
	public String list;	
	
	public UserSavedList() {}
	
	public UserSavedList(Principal user, String name, String list) {
        this.principal = user;
        this.name = name;
        this.list = list;
    }
	
}
