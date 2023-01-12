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
@Table(name = "ix_core_user_bulk_search_result", 
	uniqueConstraints={@UniqueConstraint(columnNames={"name"})})
@Indexable(indexed = false)
public class UserBulkSearchResult {
	
	@Id
    @GeneratedValue(strategy=GenerationType.IDENTITY)
    private Long id;
	
	@ManyToOne    
    @JoinColumn(name="user_id", nullable = false)	
    public Principal principal;

	@Column(nullable=false)
	public String name; 
	
	@Lob
    @Basic(fetch= FetchType.EAGER)	
	public String list;	
	
	public UserBulkSearchResult(Principal user, String name, String list) {
        this.principal = user;
        this.name = name;
        this.list = list;
    }
	
}
