package gsrs.services;

import gsrs.events.CreateEditEvent;
import gsrs.repository.EditRepository;
import ix.core.models.Edit;
import ix.core.util.EntityUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionalEventListener;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

import java.util.Optional;

/**
 * A Service for creating new {@link Edit}
 * objects.
 */
@Service
public class EditEventService {

//    @Autowired

    @PersistenceContext(unitName =  "defaultEntityManager")
    private EntityManager entityManager;

    @Autowired
    private EditRepository editRepository;

    @Transactional
    public void createNewEditFromEvent(CreateEditEvent event){
        Object entity = entityManager.find(event.getKind(), event.getId());
        if(entity !=null){
            EntityUtils.EntityWrapper<?> ew = EntityUtils.EntityWrapper.of(entity);
            String refid = ew.getKey().getIdString();
            Edit newEdit = new Edit(event.getKind(), refid);
            newEdit.version = ew.getVersion().orElse("1");
            newEdit.newValue = ew.toFullJson();
            if(event.getComments() !=null){
                newEdit.comments = event.getComments();
            }
            //set old value
            if(!("1".equals(newEdit.version))){
                try {
                    int prevVersion = Integer.parseInt(newEdit.version) - 1;
                    Optional<Edit> prevEdit = editRepository.findByRefidAndVersion(refid, Integer.toString(prevVersion));

                    prevEdit.ifPresent( e-> newEdit.oldValue = e.newValue);
                }catch(NumberFormatException e){
                    //ignore ?
                }
            }

            editRepository.saveAndFlush(newEdit);
        }


    }
}
