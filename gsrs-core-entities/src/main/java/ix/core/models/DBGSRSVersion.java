package ix.core.models;

import java.sql.Timestamp;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;

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
