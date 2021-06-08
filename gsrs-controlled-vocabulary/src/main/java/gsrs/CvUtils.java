package gsrs;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import ix.ginas.models.v1.CodeSystemControlledVocabulary;
import ix.ginas.models.v1.ControlledVocabulary;
import ix.ginas.models.v1.FragmentControlledVocabulary;
import ix.ginas.models.v1.VocabularyTerm;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public final class CvUtils {
    //these fields are used to figure out which ControlledVocabulary subclass to use
    private static Set<String> fragmentDomains;
    private static Set<String> codeSystemDomains;
    static {
        fragmentDomains = new HashSet<>();
        fragmentDomains.add("NUCLEIC_ACID_SUGAR");
        fragmentDomains.add("NUCLEIC_ACID_LINKAGE");
        fragmentDomains.add("NUCLEIC_ACID_BASE");
        fragmentDomains.add("AMINO_ACID_RESIDUE");

        codeSystemDomains = new HashSet<>();
        codeSystemDomains.add("CODE_SYSTEM");
        codeSystemDomains.add("DOCUMENT_TYPE");
    }

    private CvUtils(){
        //can not instantiate
    }
    public static List<ControlledVocabulary> adaptList(JsonNode cvList, ObjectMapper objectMapper, boolean stripIds) throws IOException {
        List<ControlledVocabulary> adaptedCvs = new ArrayList<>(cvList.size());
        for(JsonNode cvValue: cvList){

            ControlledVocabulary cv = adaptSingleRecord(cvValue, objectMapper, stripIds);
//            System.out.println("cv terms = " + cv.getTerms());
            //the Play version called cv.save() here we won't
            adaptedCvs.add(cv);


        }
        return adaptedCvs;
    }
        public static ControlledVocabulary adaptSingleRecord(JsonNode cvValue, ObjectMapper objectMapper, boolean stripIds) throws IOException {
        try {
            String domain = cvValue.at("/domain").asText();
            JsonNode vtype = cvValue.at("/vocabularyTermType");
            String termType = null;
            System.out.println("cvValue = " + cvValue);
            System.out.println("vType =  " + vtype);
            if (!vtype.isTextual()) {
                ObjectNode objn = (ObjectNode) cvValue;
                //Sometimes stored as an object, instead of a text value
                objn.set("vocabularyTermType", cvValue.at("/vocabularyTermType/value"));
            }

            termType = cvValue.at("/vocabularyTermType").asText();

            ControlledVocabulary cv = (ControlledVocabulary) objectMapper.treeToValue(cvValue, objectMapper.getClass().getClassLoader().loadClass(termType));
            if(stripIds) {
                //if there was an ID with this object, get rid of it
                //it was added by mistake
                cv.id = null;
//                cv.setVersion(1L);

            }
            if (cv.terms != null) { //Terms can be null sometimes now
                for (VocabularyTerm vt : cv.terms) {
                    if(stripIds) {
                        vt.id = null;
                    }
                    //katzelda Oct 2020 owner might not be set
                    vt.setOwner(cv);
                }
            }

            cv.setVocabularyTermType(getCVClass(domain).getName());
            return cv;
        }catch(ClassNotFoundException e){
            throw new IOException("error creating Controlled Vocabulary instance", e);
        }
    }

    private static Class<? extends ControlledVocabulary> getCVClass (String domain){


        if(fragmentDomains.contains(domain)){
            return FragmentControlledVocabulary.class;
        }else if(codeSystemDomains.contains(domain)){
            return CodeSystemControlledVocabulary.class;
        }else{
            return ControlledVocabulary.class;
        }
    }

}
