package ix.core.models;

import jakarta.persistence.Column;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;

@Entity
@DiscriminatorValue("MSH")
public class Mesh extends Value {
    public boolean majorTopic;

    @Indexable(taxonomy=true, suggest=true, name="MeSH")
    @Column(length=1024)
    public String heading;

    public Mesh () {}
    public Mesh (boolean majorTopic) {
        this.majorTopic = majorTopic;
    }
    public Mesh (String heading) {
        this.heading = heading;
    }

    @Override
    public String getValue () { return heading; }
}
