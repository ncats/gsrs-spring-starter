package ix.core.search.text;

import gsrs.indexer.IndexValueMakerFactory;
import ix.core.util.EntityUtils;
import org.springframework.beans.factory.annotation.Autowired;

import javax.persistence.PostPersist;
import javax.persistence.PostRemove;
import javax.persistence.PostUpdate;
import java.io.IOException;

/**
 * Hibernate Entity listener that will update our legacy {@link TextIndexer}.
 */
public class TextIndexerEntityListener {

    @Autowired
    private TextIndexerFactory textIndexerFactory;

    @PostPersist
    public void indexNewEntity(Object obj) throws IOException {
//        System.out.println("adding to index " + obj);
        textIndexerFactory.getDefaultInstance().add(EntityUtils.EntityWrapper.of(obj));
    }

    @PostUpdate
    public void updateEntity(Object obj) throws Exception {
//        System.out.println("updating index " + obj);
        EntityUtils.EntityWrapper ew = EntityUtils.EntityWrapper.of(obj);
        textIndexerFactory.getDefaultInstance().remove(ew);
        textIndexerFactory.getDefaultInstance().add(ew);
    }
    @PostRemove
    public void deleteEntity(Object obj) throws Exception {
//        System.out.println("removing from index " + obj);
        EntityUtils.EntityWrapper ew = EntityUtils.EntityWrapper.of(obj);
        textIndexerFactory.getDefaultInstance().remove(ew);
    }
}
