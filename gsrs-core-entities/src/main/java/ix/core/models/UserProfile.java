package ix.core.models;


import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.ObjectMapper;

import gov.nih.ncats.common.util.TimeUtil;
import ix.utils.Util;
import lombok.extern.slf4j.Slf4j;

import javax.persistence.*;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.UUID;

@Slf4j
@Entity
@Table(name = "ix_core_userprof")
@SequenceGenerator(name = "LONG_SEQ_ID", sequenceName = "ix_core_userprof_seq", allocationSize = 1)
@EntityListeners(UserProfileEntityProcessor.class)
@Indexable(indexed = false)
public class UserProfile extends IxModel{
	@Basic(fetch = FetchType.EAGER)
	@OneToOne(cascade = CascadeType.ALL)
	public Principal user;

	// is the profile currently active? authorization should take
	// this into account
	public boolean active;

	private String hashp;
	private String salt;
	public boolean systemAuth; // FDA, NIH employee

	@Lob
	@JsonIgnore
	@Column(name="ROLES_JSON") //match GSRS 2.x schema
	private String rolesJSON = null; // this is a silly, but quick way to
	// serialize roles

	// private key to be used in authentication
	// This is not a public/private key,
	// just a special secret to be used via API
	@Column(name = "apikey")
	private String key;

	// Not sure if this should be shown here?
	public String getKey() {
		return key;
	}

	public void regenerateKey() {
		key = Util.generateRandomString(20);
	}

	@ManyToMany(cascade = CascadeType.ALL)
    @JoinTable(name="ix_core_userprof_prop", inverseJoinColumns = {
            @JoinColumn(name="ix_core_value_id")
    })
	public List<Value> properties = new ArrayList<Value>();

	public UserProfile() {
	}

	public UserProfile(Principal user) {
		this.user = user;
		regenerateKey();
	}

	//Needed for JSON
	public String getIdentifier() {
		return user.username;
	}

	public List<Role> getRoles() {
		List<Role> rolekinds = new ArrayList<Role>();
		if (this.rolesJSON != null) {
			try {
				ObjectMapper om = new ObjectMapper();
				List l = om.readValue(rolesJSON, List.class);
				for (Object o : l) {
					try {
						rolekinds.add(Role.valueOf(o.toString()));
					} catch (Exception e) {
						e.printStackTrace();
					}
				}
			} catch (Exception e) {
				log.warn(e.getMessage(), e);
			}
		}
		return rolekinds;
	}

	public void setRoles(List<Role> rolekinds) {
		ObjectMapper om = new ObjectMapper();
		rolesJSON = om.valueToTree(rolekinds).toString();
	}

	public void addRole(Role role) {
		List<Role> roles = getRoles();
		roles.add(role);
		setRoles(new ArrayList<Role>(new LinkedHashSet<Role>(roles)));
	}

	public boolean hasRole(Role role) {
		return this.getRoles().contains(role);
	}


	public String getComputedToken(){
		return getComputedToken(this.user.username, this.key);
	}
	public static String getComputedToken(String username, String key) {
		String date = "" + Util.getCanonicalCacheTimeStamp();
		return Util.sha1(date + username + key);
	}

	public Long getTokenTimeToExpireMS() {
		long date = (Util.getCanonicalCacheTimeStamp() + 1) * Util.getTimeResolutionMS();
		return (date - TimeUtil.getCurrentTimeMillis());
	}

	private String getPreviousComputedToken() {
		String date = "" + (Util.getCanonicalCacheTimeStamp() - 1);
		return Util.sha1(date + this.user.username + this.key);
	}

	public boolean acceptKey(String key) {
		if (key.equals(this.key))
			return true;
		return false;
	}

	public boolean acceptToken(String token) {
		if (this.getComputedToken().equals(token))
			return true;
		if (this.getPreviousComputedToken().equals(token))
			return true;
		return false;
	}

	public boolean acceptPassword(String password) {
		if (this.hashp == null || this.salt == null)
			return false;
		return this.hashp.equals(Util.encrypt(password, this.salt));
	}

	public void setPassword(String password) {
		if (password == null || password.length() <= 0) {
			password = UUID.randomUUID().toString();
		}
		this.salt = Util.generateSalt();
		this.hashp = Util.encrypt(password, this.salt);
	}
	@JsonIgnore
	public String getEncodePassword(){
		return salt.length()+"$"+salt+hashp;
	}


	public static UserProfile GUEST() {
		UserProfile up = new UserProfile(new Principal("GUEST"));
		up.addRole(Role.Query);

		return up;
	}

	public boolean isRoleQueryOnly(){

		if(this.hasRole(Role.Query) && this.getRoles().size()==1){
			return true;

		}
		return false;
	}

}
