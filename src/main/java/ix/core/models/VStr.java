package ix.core.models;

import javax.persistence.Column;
import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;

@Entity
@DiscriminatorValue("STR")
public class VStr extends Value {
    @Column(length=1024)
    public String strval;

    public VStr () {}
    public VStr (String label) {
        super (label);
    }
    public VStr (String label, String value) {
        super (label);
        strval = value;
    }

    public String getStrval () { return strval; }
    public void setStrval (String strval) { this.strval = strval; }

    @Override
    public String getValue () { return strval; }
}
