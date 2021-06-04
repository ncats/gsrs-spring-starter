package ix.core.models;

import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;

@Entity
@DiscriminatorValue("FAKE")
public class FileDataPlaceHolder extends FileData {
   
}
