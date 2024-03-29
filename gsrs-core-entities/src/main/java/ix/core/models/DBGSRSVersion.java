package ix.core.models;

import java.sql.Timestamp;
import java.util.Date;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

import gov.nih.ncats.common.util.TimeUtil;
import ix.ginas.models.serialization.GsrsDateDeserializer;
import ix.ginas.models.serialization.GsrsDateSerializer;
import lombok.Data;

@Entity
@Data
@Table(name = "ix_core_db_gsrs_version")
@Indexable(indexed = false)
@SequenceGenerator(name = "db_gsrs_version", sequenceName = "db_gsrs_version_seq", allocationSize = 1)
public class DBGSRSVersion {

	@Id
	@Column(unique = true)
	@GeneratedValue(strategy = GenerationType.AUTO, generator = "db_gsrs_version")
	public Long id;

	@Column(nullable = false)
	String entity;

	@Column(nullable = false, name = "version_info")
	String versionInfo;

	public Timestamp modified;

	String hash;

	public DBGSRSVersion() {
	}

	public DBGSRSVersion(String entity, String versionInfo, Timestamp timestamp) {
		this.entity = entity;
		this.versionInfo = versionInfo;
		this.modified = timestamp;
	}
}
