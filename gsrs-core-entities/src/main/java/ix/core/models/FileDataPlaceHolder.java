package ix.core.models;

import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;

@Entity
@DiscriminatorValue("FAKE")
public class FileDataPlaceHolder extends FileData {
   
}
