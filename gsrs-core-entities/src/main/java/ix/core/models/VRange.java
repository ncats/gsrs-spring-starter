package ix.core.models;

import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;

@Entity
@DiscriminatorValue("RNG")
public class VRange extends Value {
    public Double lval;
    public Double rval;
    public Double average;

    public VRange () {}
    public VRange (String label, Double lval, Double rval) {
        super (label);
        this.lval = lval;
        this.rval = rval;
    }
}
