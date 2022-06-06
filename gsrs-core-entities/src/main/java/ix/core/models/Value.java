package ix.core.models;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import org.hibernate.annotations.GenericGenerator;


import javax.persistence.*;
import java.io.Serializable;

@Entity
@Table(name = "ix_core_value",
    indexes = {
        @Index(name = "value_label_index", columnList = "label"),
        @Index(name = "value_term_index", columnList = "term")
    }
)
@Inheritance
@DiscriminatorValue("VAL")
@JsonInclude(JsonInclude.Include.NON_NULL)
@SequenceGenerator(name = "LONG_SEQ_ID", sequenceName = "ix_core_value_seq", allocationSize = 1)
public class Value extends LongBaseModel implements Serializable{
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
