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

    @Lock(LockModeType.OPTIMISTIC_FORCE_INCREMENT)
    default Optional<T> findByKey(EntityUtils.Key key){
        return findById((ID)key.getIdNative());
    }

    @Lock(LockModeType.OPTIMISTIC_FORCE_INCREMENT)
    @Override
    List<T> findAll();

    @Lock(LockModeType.OPTIMISTIC_FORCE_INCREMENT)
    @Override
    List<T> findAll(Sort sort);

    @Lock(LockModeType.OPTIMISTIC_FORCE_INCREMENT)
    @Override
    List<T> findAllById(Iterable<ID> iterable);

    @Lock(LockModeType.OPTIMISTIC_FORCE_INCREMENT)
    @Override
    T getOne(ID id);

    @Lock(LockModeType.OPTIMISTIC_FORCE_INCREMENT)
    @Override
    <S extends T> List<S> findAll(Example<S> example);

    @Lock(LockModeType.OPTIMISTIC_FORCE_INCREMENT)
    @Override
    <S extends T> List<S> findAll(Example<S> example, Sort sort);

    @Lock(LockModeType.OPTIMISTIC_FORCE_INCREMENT)
    @Override
    Page<T> findAll(Pageable pageable);

    @Lock(LockModeType.OPTIMISTIC_FORCE_INCREMENT)
    @Override
    Optional<T> findById(ID id);

    @Lock(LockModeType.OPTIMISTIC_FORCE_INCREMENT)
    @Override
    <S extends T> Optional<S> findOne(Example<S> example);

    @Lock(LockModeType.OPTIMISTIC_FORCE_INCREMENT)
    @Override
    <S extends T> Page<S> findAll(Example<S> example, Pageable pageable);
}
