package ix.core.models;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import jakarta.persistence.*;

@Entity
@DiscriminatorValue("TXT")
public class Text extends Value {
    @JdbcTypeCode(SqlTypes.LONG32VARCHAR)
    @Basic(fetch= FetchType.EAGER)
    public String text;

    public Text () {}
    public Text (String label) {
        super (label);
    }
    public Text (String label, String value) {
        super (label);
        text = value;
    }

    public String getText () { return text; }
    public void setText (String text) { this.text = text; }

    @Override
    public String getValue () { return text; }

    public void setValue(String newValue){
        text=newValue;
    }
}
