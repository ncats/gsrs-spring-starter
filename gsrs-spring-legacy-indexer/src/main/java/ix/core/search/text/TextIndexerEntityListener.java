package ix.core.search.text;

import gsrs.indexer.IndexCreateEntityEvent;
import gsrs.indexer.IndexRemoveEntityEvent;
import gsrs.indexer.IndexUpdateEntityEvent;
import gsrs.springUtils.AutowireHelper;
import ix.core.util.EntityUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import javax.persistence.PostPersist;
import javax.persistence.PostRemove;
import javax.persistence.PostUpdate;
import java.io.IOException;
import java.io.UncheckedIOException;

/**
 * Hibernate Entity listener that will update our legacy {@link TextIndexer}.
 */
@Component
public class TextIndexerEntityListener {

    @Autowired
    private TextIndexerFactory textIndexerFactory;

    private void autowireIfNeeded(){
        if(textIndexerFactory==null) {
            AutowireHelper.getInstance().autowire(this);
        }
    }

    @EventListener
    public void created(IndexCreateEntityEvent event) {
        try {
            textIndexerFactory.getDefaultInstance().add(event.getSource());
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }



    @EventListener
    public void updateEntity(IndexUpdateEntityEvent event) throws Exception {
//        System.out.println("updating index " + obj);
        autowireIfNeeded();
        EntityUtils.EntityWrapper ew = event.getSource();
        textIndexerFactory.getDefaultInstance().remove(ew);
        textIndexerFactory.getDefaultInstance().add(ew);
    }
    @EventListener
    public void deleteEntity(IndexRemoveEntityEvent event) throws Exception {
//        System.out.println("removing from index " + obj);
        autowireIfNeeded();
        textIndexerFactory.getDefaultInstance().remove(event.getSource());
    }
}
