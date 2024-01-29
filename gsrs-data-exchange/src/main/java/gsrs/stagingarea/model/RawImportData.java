package gsrs.stagingarea.model;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.annotations.GenericGenerator;
import org.hibernate.annotations.Type;

import jakarta.persistence.*;
import java.util.UUID;

@Entity
@Table(name = "ix_import_raw")
@Slf4j
@Data
public class RawImportData {
    @Id
    @GenericGenerator(name = "NullUUIDGenerator", type = ix.ginas.models.generators.NullUUIDGenerator.class)
    @GeneratedValue(generator = "NullUUIDGenerator")
    private UUID recordId;

    @Lob
    private byte[] rawData;

    private String recordFormat;
}
