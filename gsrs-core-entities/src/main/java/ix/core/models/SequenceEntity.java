package ix.core.models;

public interface SequenceEntity {

    enum SequenceType{
     NUCLEIC_ACID,
     PROTEIN,
     UNKNOWN
    }

    SequenceType computeSequenceType();

}
