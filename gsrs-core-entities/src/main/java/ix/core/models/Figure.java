package ix.core.models;

import com.fasterxml.jackson.annotation.JsonIgnore;

import javax.persistence.*;

@Entity
@Table(name="ix_core_figure")
@Inheritance
@DiscriminatorValue("FIG")
public class Figure extends LongBaseModel {
    @Id
    public Long id; // internal id

    public String caption;
    public String mimeType;
    @Column(length=1024)
    public String url;

    @Lob
    @JsonIgnore
    @Indexable(indexed=false)
    @Basic(fetch= FetchType.EAGER)
    public byte[] data;

    @Column(name="data_size")
    public int size;
    @Column(length=140)
    public String sha1;

    public Figure() {}
    public Figure(String caption) {
        this.caption = caption;
    }

    //TODO katzelda October 2020 : this link should be made in a helper method in the controller
//    public String getUrl () {
//        return url != null ? url : (Global.getRef(this) + "?format=image");
//    }
}
