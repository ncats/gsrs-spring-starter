package gsrs.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.NoRepositoryBean;

import ix.core.util.EntityUtils;
@NoRepositoryBean
public interface GsrsRepository<T, ID> extends JpaRepository<T, ID> {


    default Optional<T> findByKey(EntityUtils.Key key){
        return findById((ID)key.getIdNative());
    }

}
