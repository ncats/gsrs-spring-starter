package gsrs.services;

import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import gsrs.events.CreateEditEvent;
import gsrs.repository.EditRepository;
import ix.core.models.Edit;
import ix.core.util.EntityUtils;
import ix.core.util.EntityUtils.Key;
import lombok.extern.slf4j.Slf4j;

/**
 * A Service for creating new {@link Edit}
 * objects.
 */
@Service
@Slf4j
public class EditEventService {


    @Autowired
    private EditRepository editRepository;

    @Transactional
    public void createNewEditFromEvent(CreateEditEvent event){

        Key kk = Key.of(event.getKind(), event.getId());
        Object entity=null;
        try {
            entity = kk.getEntityManager().find(event.getKind(), event.getId());
        }catch(Throwable e) {
            log.warn("Trouble making edit for:" + kk.toString(),e);
            return;
        }
        
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
