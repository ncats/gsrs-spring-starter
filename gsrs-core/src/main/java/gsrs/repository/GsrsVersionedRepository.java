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
public interface GsrsVersionedRepository<T, ID> extends GsrsRepository<T, ID> {

    default Optional<T> findByKey(EntityUtils.Key key){
        return findById((ID)key.getIdNative());
    }

    @Lock(LockModeType.OPTIMISTIC_FORCE_INCREMENT)
    @Override
    <S extends T> S save(S s);
    @Lock(LockModeType.OPTIMISTIC_FORCE_INCREMENT)
    @Override
    <S extends T> List<S> saveAll(Iterable<S> iterable);
    @Lock(LockModeType.OPTIMISTIC_FORCE_INCREMENT)
    @Override
    <S extends T> S saveAndFlush(S s);
}
