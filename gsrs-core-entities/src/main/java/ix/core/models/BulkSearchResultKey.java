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
@Table(name = "ix_core_bulk_search_result_key_list", 
	uniqueConstraints={@UniqueConstraint(columnNames={"key","list_name","user_id"})})
@Indexable(indexed = false)
public class BulkSearchResultKey {
	
	@Id
    @GeneratedValue(strategy=GenerationType.IDENTITY)
    private Long id;
	
	public String key;	
	
	@ManyToOne    
    @JoinColumn(name="user_id", nullable = false)	
    public Principal principal;

	@Column(nullable=false)
	public String list_name; 	
	
	
	public BulkSearchResultKey(String key, Principal user, String name) {
		this.key = key;
        this.principal = user;
        this.list_name = name;        
    }	
}
