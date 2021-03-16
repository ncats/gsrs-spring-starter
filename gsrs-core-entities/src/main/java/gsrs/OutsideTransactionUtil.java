package gsrs;

import org.springframework.stereotype.Component;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.transaction.Transactional;
import java.util.function.Consumer;
@Component
public class OutsideTransactionUtil {
    @PersistenceContext
    protected EntityManager entityManager;


    public void DoInOutsideTransaction(Consumer<EntityManager> consumer){
        //katzelda we need to create an Edit object here so hibernate can access the "old value"
//        EntityManager cleanEM = entityManager.getEntityManagerFactory().createEntityManager();

            consumer.accept(entityManager);

    }
}
