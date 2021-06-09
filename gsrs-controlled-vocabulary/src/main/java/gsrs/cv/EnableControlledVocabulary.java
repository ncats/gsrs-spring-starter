package gsrs.cv;


import gsrs.repository.ControlledVocabularyRepository;
import org.springframework.context.annotation.Import;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE})
@Import({GsrsControlledVocabSelector.class, /*ControlledVocabularyRepository.class*/})
public @interface EnableControlledVocabulary {
}
