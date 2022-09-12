package gsrs.repository;

import ix.core.models.Text;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Repository
@Transactional
public interface TextRepository extends GsrsRepository<Text, Long> {

    @Query("select t from Text t where t.label = ?1")
    public List<Text> findByLabel(String label);

    @Modifying
    @Transactional
    @Query("delete from Text t where t.id = ?1")
    void deleteByRecordId(Long id);
}
