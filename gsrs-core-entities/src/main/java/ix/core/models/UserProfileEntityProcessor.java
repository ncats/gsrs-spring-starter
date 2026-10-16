package ix.core.models;

import gsrs.security.UserTokenCache;
import gsrs.springUtils.AutowireHelper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import jakarta.persistence.PostPersist;
import jakarta.persistence.PostUpdate;

@Component
public class UserProfileEntityProcessor {

    @Autowired(required = false)
    private UserTokenCache userTokenCache;

    private boolean initialized=false;
    @PostPersist
    @PostUpdate
    public synchronized void updateTokenCache(UserProfile up){
        autowireIfNeeded();
        if(userTokenCache !=null){
            userTokenCache.updateUserCache(up);
        }
    }

    private synchronized void autowireIfNeeded() {
        if(!initialized) {
            AutowireHelper.getInstance().autowire(this);
            initialized = true;
        }
    }
}
