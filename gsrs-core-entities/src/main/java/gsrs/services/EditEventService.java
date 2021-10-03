package gsrs.services;

import java.util.List;
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
            entity = kk.toRootKey().fetch().get().getValue();
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
            
            if(event.getOldJson()!=null) {
                newEdit.oldValue = event.getOldJson();
            }else {
                //set old value
                if(!("1".equals(newEdit.version))){
                    try {
                        int prevVersion = Integer.parseInt(newEdit.version) - 1;
                        //made this a list incase there are multiple edits with the same version?
                        List<Edit> prevEdit = editRepository.findByRefidAndVersion(refid, Integer.toString(prevVersion));
                        if(!prevEdit.isEmpty()){
                            newEdit.oldValue = prevEdit.get(0).newValue;
                        }
    
                    }catch(NumberFormatException e){
                        //ignore ?
                    }
                }
            }
            //check to see if there's an edit with this version already
            List<Edit> oldEdits = editRepository.findByRefidAndVersion(refid, newEdit.version);
            if(oldEdits.isEmpty()) {
                editRepository.saveAndFlush(newEdit);
            }else{
                //update ?
                Edit oldEdit = oldEdits.get(0);
                oldEdit.comments = newEdit.comments;
                oldEdit.newValue = newEdit.newValue;
                oldEdit.oldValue = newEdit.oldValue;
                editRepository.saveAndFlush(oldEdit);
            }
        }


    }
}
