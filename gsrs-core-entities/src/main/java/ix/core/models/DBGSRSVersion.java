package ix.core.models;

import java.util.Date;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

import gov.nih.ncats.common.util.TimeUtil;
import ix.ginas.models.serialization.GsrsDateDeserializer;
import ix.ginas.models.serialization.GsrsDateSerializer;
import lombok.Data;

@Entity
@Data
@Table(name = "ix_core_DB_GSRS_version")
@Indexable(indexed = false)
public class DBGSRSVersion {
	
	@Id    
    @GeneratedValue(strategy = GenerationType.AUTO, generator = "LONG_SEQ_ID")
    public Long id;
	
	@Column(nullable=false)
	String entity;
	
	@Column(nullable=false)
	String version;
	
	@JsonSerialize(using = GsrsDateSerializer.class)
    @JsonDeserialize(using = GsrsDateDeserializer.class)
    public Date modified = TimeUtil.getCurrentDate();
	
	String hash;
	
	public DBGSRSVersion() {}
	
	public DBGSRSVersion(String entity, String version) {
		this.entity = entity;
		this.version = version;
	}
	
}
