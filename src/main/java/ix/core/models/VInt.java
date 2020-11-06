package ix.core.models;

import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;

@Entity
@DiscriminatorValue("INT")
public class VInt extends Value {
    public Long intval;

    public VInt () {}
    public VInt (String label, Long value) {
        super (label);
        intval = value;
    }

    public Long getIntval () { return intval; }
    public void setIntval (Long value) { intval = value; }

    @Override
    public Long getValue () { return intval; }
}
