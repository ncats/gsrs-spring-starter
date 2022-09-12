package gsrs.repository;

import ix.core.models.Text;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
@Transactional
public interface TextRepository extends GsrsRepository<Text, Long> {

}
