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
        try {
            log.debug("Making edit for:" + kk.toString());
            
            Object entity=null;
            try {
                entity = kk.toRootKey().fetch().get().getValue();
            }catch(Throwable e) {
                log.error("Trouble making edit for:" + kk.toString(),e);
                return;
            }

            if(entity !=null){
                EntityUtils.EntityWrapper<?> ew = EntityUtils.EntityWrapper.of(entity);
                String refid = ew.getKey().getIdString();
                Edit newEdit = new Edit(event.getKind(), refid);
                // TP 10/03/2021 I've changed this to use the supplied
                // version for the edit. That's often "null" for objects
                // which weren't editted via a "registred edit"
                newEdit.version = event.getVersion();
                newEdit.newValue = ew.toFullJson();
                if(event.getComments() !=null){
                    newEdit.comments = event.getComments();
                }

                //use supplied old JSON if possible
                if(event.getOldJson()!=null) {
                    newEdit.oldValue = event.getOldJson();
                }else {
                    //otherwise use JSON by trying to find last edit in repository
                    // set old value
                    //                    
                    //                if(!("1".equals(newEdit.version))){
                    //                    try {
                    //                        int prevVersion = Integer.parseInt(newEdit.version) - 1;
                    //                        //made this a list in case there are multiple edits with the same version?
                    //                        //TODO: This logic and contract will need to be more explicit. The exact nature of what it means
                    //                        // to store the same edit with more than one value needs to be decided.
                    //                        // It's also worth noting that sometimes a version number may skip, so simply looking at the
                    //                        // <currentVersion - 1> may not be good enough. You should look for the last stored version before
                    //                        // this one. (Tyler Peryea, 10/03/2021)
                    //                        List<Edit> prevEdit = editRepository.findByRefidAndVersion(refid, Integer.toString(prevVersion));
                    //                        if(!prevEdit.isEmpty()){
                    //                            newEdit.oldValue = prevEdit.get(0).newValue;
                    //                        }
                    //    
                    //                    }catch(NumberFormatException e){
                    //                        //ignore ?
                    //                    }
                    //                }


                    //This is a slightly safer and less version-dependent way of getting the last edit
                    Optional<Edit> prevEdit = editRepository.findFirstByKeyOrderByCreatedDesc(kk);
                    if(prevEdit.isPresent()){
                        newEdit.oldValue = prevEdit.get().newValue;
                    }

                }
                // TODO: 
                // The way these edits are working is quite a bit different than 2.X. I wouldn't want to clobber edits unless
                // we absolutely have to. In 2.X, outside of core entities, edit version was an option field for edits. You
                // could "infer" version number just by create date. The vast majority of edit objects are for non-version-key
                // controlled things, and this logic below would wipe out / clobber those edits and collapse them all to a single
                // edit. To deal with this case, I'm adding a null check. (Tyler Peryea, 10/03/2021) 

                if(newEdit.version!=null) {
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
                }else {
//                    editRepository.saveAndFlush(newEdit);
                }
            }
        }catch(Exception e) {
            log.error("Trouble making edit for:" + kk.toString(),e);
            throw e;
        }


    }
}
