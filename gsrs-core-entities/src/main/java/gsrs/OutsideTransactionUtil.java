package gsrs;

import org.springframework.stereotype.Component;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.transaction.Transactional;
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
