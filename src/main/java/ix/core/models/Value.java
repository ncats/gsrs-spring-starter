package ix.core.models;

import com.fasterxml.jackson.annotation.JsonIgnore;

import javax.persistence.*;
import java.io.Serializable;

@Entity
@Table(name="ix_core_value")
@Inheritance
@DiscriminatorValue("VAL")
public class Value extends LongBaseModel implements Serializable{
    @Id
    @GeneratedValue(strategy=GenerationType.SEQUENCE)
    public Long id;
    public String label;
    
    public Value () {}
    public Value (String label) {
        this.label = label;
    }

    @JsonIgnore
    public Object getValue () {
        throw new UnsupportedOperationException
            ("getValue is not defined for class "+getClass().getName());
    }
}
