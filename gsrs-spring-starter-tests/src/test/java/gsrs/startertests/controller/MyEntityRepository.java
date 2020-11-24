package gsrs.startertests.controller;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface MyEntityRepository extends JpaRepository<MyEntity, UUID> {
    MyEntity findDistinctByFoo(String foo);
}
