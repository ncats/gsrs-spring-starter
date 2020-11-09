package gsrs.repository;

import ix.core.util.EntityUtils;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.NoRepositoryBean;

import java.util.Optional;
@NoRepositoryBean
public interface GsrsRepository<T, ID> extends JpaRepository<T, ID> {

    default Optional<T> findByKey(EntityUtils.Key key){
        return findById((ID)key.getIdNative());
    }
}
