package gsrs.repository;

import ix.core.util.EntityUtils;
import org.springframework.data.domain.Example;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.repository.NoRepositoryBean;

import javax.persistence.LockModeType;
import java.util.List;
import java.util.Optional;
@NoRepositoryBean
public interface GsrsRepository<T, ID> extends JpaRepository<T, ID> {


    default Optional<T> findByKey(EntityUtils.Key key){
        return findById((ID)key.getIdNative());
    }

}
