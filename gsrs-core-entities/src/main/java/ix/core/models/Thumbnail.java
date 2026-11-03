package ix.core.models;

import jakarta.persistence.CascadeType;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import jakarta.persistence.ManyToOne;

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
