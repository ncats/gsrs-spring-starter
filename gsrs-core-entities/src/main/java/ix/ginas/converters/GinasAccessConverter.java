package ix.ginas.converters;


import gsrs.repository.GroupRepository;
import gsrs.springUtils.AutowireHelper;
import ix.core.models.Group;
import ix.ginas.models.GinasAccessContainer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.persistence.AttributeConverter;
import javax.persistence.Converter;
import java.util.BitSet;
import java.util.Optional;

@Converter(autoApply = true)
public class GinasAccessConverter implements AttributeConverter<GinasAccessContainer, byte[]> {

	@Autowired
	private GroupRepository groupRepository;

//	public GinasAccessConverter(GroupRepository groupRepository) {
//		this.groupRepository = groupRepository;
//	}

	@Override
	public byte[] convertToDatabaseColumn(GinasAccessContainer attribute) {
		BitSet bs = new BitSet();

		try {
			for(Group g: attribute.getAccess()){
				Long lid=g.id;
				if(lid==null){
					throw new IllegalStateException(g.name + " not persisted yet?");
				}else{
					bs.set(lid.intValue());
				}
			}
			//System.out.println("serializaing access");
			return bs.toByteArray();
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
	}

	private void injectGroupRepositoryIfNeeded(){
		if(groupRepository ==null){
			AutowireHelper.getInstance().autowire(this);
		}
	}
	@Override
	public synchronized GinasAccessContainer convertToEntityAttribute(byte[] dbData) {
		injectGroupRepositoryIfNeeded();
		BitSet bs = BitSet.valueOf(dbData);

		try {
			GinasAccessContainer gac=new GinasAccessContainer();
			for (int i = bs.nextSetBit(0); i >= 0; i = bs.nextSetBit(i+1)) {
				Optional<Group> g1=groupRepository.findById(Long.valueOf(i));
				if(g1.isPresent()){
					gac.add(g1.get());
				}
				if (i == Integer.MAX_VALUE) {
					break; // or (i+1) would overflow
				}
			}
			return gac;

		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}
}