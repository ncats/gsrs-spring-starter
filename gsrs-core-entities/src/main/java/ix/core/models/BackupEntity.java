package ix.core.models;

import java.nio.charset.StandardCharsets;
import java.util.Objects;
import java.util.Optional;

import jakarta.persistence.Basic;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Lob;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;

import com.fasterxml.jackson.annotation.JsonIgnore;

import ix.core.History;
import ix.core.controllers.EntityFactory.EntityMapper;
import ix.utils.Util;
import lombok.extern.slf4j.Slf4j;

/**
 * A Backup record of a {@link FetchableEntity}.
 */
@Slf4j
@Entity
@Table(name="ix_core_backup")
@History(store=false)
@Indexable(indexed = false)
@SequenceGenerator(name = "LONG_SEQ_ID", sequenceName = "ix_core_backup_seq", allocationSize = 1)
public class BackupEntity extends IxModel{

	private static final EntityMapper em = EntityMapper.INTERNAL_ENTITY_MAPPER();

	@Column(unique = true)
	private String refid;
	private String kind;
	
	@Lob
    @JsonIgnore
    @Indexable(indexed=false)
    @Basic(fetch= FetchType.LAZY)
    public byte[] data;
//	@Lob
//	private String json;
	
	private String sha1;
	
	private boolean compressed=true;
	
	public BackupEntity(){
		
	}

	@Override
	public String toString() {
		return "BackupEntity{" +
				"refid='" + refid + '\'' +
				", kind='" + kind + '\'' +
				", id=" + id +
				"} " + super.toString();
	}

	public BackupEntity(boolean compressed){
		this.compressed=compressed;
	}
	
	public Class<?> getKind(){
		try {
			return Class.forName(kind);
		} catch (ClassNotFoundException e) {
			return null;
		}
	}

	
	
	@JsonIgnore
	private byte[] getBytes() throws Exception{
		if(compressed){
			return Util.decompress(this.data);
		}else{
			return this.data;
		}
	}
	
	@JsonIgnore
	private void setBytes(byte[] data) throws Exception{
		if(compressed){
			this.data= Util.compress(data);
		}else{
			this.data=data;
		}
	}

	/**
	 * Like {@link #getInstantiated()} but instead of throwing
	 * an error return {@link Optional#empty()}.
	 * @return the instantiated entity that was backed up wrapped in an Optional
	 * or empty if there was a problem.
	 */
	@JsonIgnore
	public Optional<Object> getOptionalInstantiated(){
		Class<?> cls= getKind();
		if(cls ==null){
			return Optional.empty();
		}
		try {
			return Optional.of(em.readValue(getBytes(), cls));
		} catch (Exception e) {
		    log.warn("Unable to instantiate backup entity:" + this.getKind()+":" +  this.getRefid(),e);
//			e.printStackTrace();
			return Optional.empty();
		}
	}
	@JsonIgnore
	public Object getInstantiated() throws Exception{
		Class<?> cls= getKind();
		if(cls==null){
			throw new IllegalStateException("Kind is not set for object " + kind);
		}
		
		Object inst=em.readValue(getBytes(), cls);
		return inst;
	}
	@JsonIgnore
	public void setFromOther(BackupEntity other) throws Exception {
		kind = other.kind;
		refid= other.refid;
		sha1 = other.sha1;
		setBytes(other.getBytes());
		setIsDirty("kind");
		setIsDirty("refid");
		setIsDirty("sha1");
		setIsDirty("data");
	}
	@JsonIgnore
	public void setInstantiated(FetchableEntity o) throws Exception{
		kind=o.getClass().getName();
		refid=o.fetchGlobalId();
		String json = em.toJson(o);
		Objects.requireNonNull(json);
		setBytes(json.getBytes(StandardCharsets.UTF_8));
		sha1= Util.sha1(data);
	}
	
	public boolean matchesHash(){
		String sha1= Util.sha1(data);
		return this.sha1.equals(sha1);
	}
	
	public boolean isOfType(Class<?> type){
		Class<?> cls= getKind();
		if(cls==null)return false;
		return type.isAssignableFrom(cls);
	}
	@JsonIgnore
	public String getRefid() {
		return refid;
	}
}
