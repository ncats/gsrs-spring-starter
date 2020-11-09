package gsrs.indexer;

//
//import gov.nih.ncats.common.util.CachedSupplier;
//import org.hibernate.search.mapper.orm.Search;
//import org.hibernate.search.mapper.orm.massindexing.MassIndexer;
//import org.hibernate.search.mapper.orm.session.SearchSession;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.stereotype.Service;
//
//import javax.persistence.EntityManager;


public abstract class EntitySearchService<T> {
    /*
    @Autowired
    private EntityManager entityManager;

    private final Class<T> entityClass;

    public EntitySearchService(Class<T> entityClass) {
        this.entityClass = entityClass;
    }

    private CachedSupplier<Void> initializeCachedSupplier = CachedSupplier.runOnce(()-> {
        initialize();
        return null;
    });

    public void initalizeIfNeeded(){
        initializeCachedSupplier.get();
    }

    public void initialize() {

        try {
            //don't call createSearchSession here to avoid Stackoverflow
            SearchSession searchSession = Search.session( entityManager );

            MassIndexer indexer = searchSession.massIndexer( entityClass);
            indexer.startAndWait();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    public SearchSession createSearchSession(){
        initalizeIfNeeded();
        return Search.session( entityManager );

    }

     */

}
