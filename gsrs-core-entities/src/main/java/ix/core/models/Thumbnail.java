package ix.core.models;

import javax.persistence.CascadeType;
import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;
import javax.persistence.ManyToOne;

import com.fasterxml.jackson.annotation.JsonIgnore;

@Entity
@DiscriminatorValue("THU")
public class Thumbnail extends Figure {
    @JsonIgnore
    @ManyToOne(cascade = CascadeType.PERSIST)
    public Figure parent;

    public Thumbnail () {}

    public boolean isThumbnail () { return true; }
}
