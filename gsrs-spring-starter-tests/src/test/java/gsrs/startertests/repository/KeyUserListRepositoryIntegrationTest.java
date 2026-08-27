package gsrs.startertests.repository;

import static org.assertj.core.api.Assertions.assertThat;

import java.lang.reflect.Field;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.test.context.ActiveProfiles;

import gsrs.repository.KeyUserListRepository;
import gsrs.startertests.GsrsJpaTest;
import gsrs.startertests.jupiter.AbstractGsrsJpaEntityJunit5Test;
import ix.core.models.KeyUserList;
import ix.core.models.Principal;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.SequenceGenerator;

@GsrsJpaTest
@ActiveProfiles("test")
public class KeyUserListRepositoryIntegrationTest extends AbstractGsrsJpaEntityJunit5Test {

	@Autowired
	private TestEntityManager entityManager;

	@Autowired
	private KeyUserListRepository repository;

	@Test
	public void keyUserListUsesDedicatedSequenceGenerator() throws NoSuchFieldException {
		Field id = KeyUserList.class.getDeclaredField("id");
		SequenceGenerator sequenceGenerator = id.getAnnotation(SequenceGenerator.class);
		GeneratedValue generatedValue = id.getAnnotation(GeneratedValue.class);

		assertThat(sequenceGenerator.name()).isEqualTo("KEY_USER_LIST_SEQ_GENERATOR");
		assertThat(sequenceGenerator.sequenceName()).isEqualTo("ix_core_key_user_list_seq");
		assertThat(sequenceGenerator.allocationSize()).isEqualTo(1);
		assertThat(generatedValue.strategy()).isEqualTo(GenerationType.SEQUENCE);
		assertThat(generatedValue.generator()).isEqualTo("KEY_USER_LIST_SEQ_GENERATOR");
	}

	@Test
	public void saveMultipleKeyUserListRowsWithGeneratedIds() {
		Principal user = new Principal("key-list-user", null);
		entityManager.persistAndFlush(user);

		KeyUserList first = repository.saveAndFlush(new KeyUserList("key-1", user, "list-1", "kind-1"));
		KeyUserList second = repository.saveAndFlush(new KeyUserList("key-2", user, "list-1", "kind-1"));

		assertThat(first.id).isNotNull();
		assertThat(second.id).isNotNull();
		assertThat(second.id).isNotEqualTo(first.id);
	}
}
