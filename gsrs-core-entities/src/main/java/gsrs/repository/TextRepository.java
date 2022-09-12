package gsrs.repository;


import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import ix.core.models.Text;

@Repository
@Transactional
public interface TextRepository extends JpaRepository<Text, Long> {
	
}