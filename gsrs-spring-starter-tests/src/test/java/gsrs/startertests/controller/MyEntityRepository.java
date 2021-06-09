package gsrs.startertests.controller;

import gsrs.repository.GsrsVersionedRepository;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface MyEntityRepository extends GsrsVersionedRepository<MyEntity, UUID> {
    MyEntity findDistinctByFoo(String foo);
}
