package gsrs.repository;

import ix.core.models.Text;
import org.springframework.stereotype.Repository;
import java.util.UUID;

@Repository
public interface TextRepository extends GsrsRepository<Text, Long> {

}
