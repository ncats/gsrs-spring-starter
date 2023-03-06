package ix.core.models;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;

@Entity
@Table(name = "ix_core_key_user_list", 
	uniqueConstraints={@UniqueConstraint(columnNames={"entity_key","list_name","user_id"})})
@Indexable(indexed = false)
public class KeyUserList {
	
	@Id
	@Column(unique = true)
	@GeneratedValue(strategy = GenerationType.AUTO, generator = "LONG_SEQ_ID")
    public Long id;		
	
	public String entity_key;	
	
	@ManyToOne    
    @JoinColumn(name="user_id")	
    public Principal principal;

	@Column(nullable=false)
	public String list_name; 	
	
	
	public KeyUserList(String key, Principal user, String name) {
		this.entity_key = key;
        this.principal = user;
        this.list_name = name;        
    }	
}
