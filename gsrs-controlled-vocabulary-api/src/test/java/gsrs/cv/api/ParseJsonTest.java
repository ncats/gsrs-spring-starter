package gsrs.cv.api;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;

import static org.junit.jupiter.api.Assertions.*;

public class ParseJsonTest {

    ObjectMapper mapper = new ObjectMapper();
    @Test
    public void parseSingleDTOWithDefaultCVTerm() throws JsonProcessingException {
        String json = "{\"id\":1795,\"version\":1,\"created\":1473443705000,\"modified\":1612668776000,\"deprecated\":false,\"domain\":\"ACCESS_GROUP\",\"vocabularyTermType\":\"ix.ginas.models.v1.ControlledVocabulary\",\"fields\":[\"ACCESS\"],\"editable\":false,\"filterable\":false,\"terms\":[{\"id\":43473,\"version\":1,\"created\":1473443705000,\"modified\":1612668776000,\"deprecated\":false,\"value\":\"protected\",\"display\":\"PROTECTED\",\"filters\":[],\"hidden\":false,\"selected\":false},{\"id\":43474,\"version\":1,\"created\":1473443705000,\"modified\":1612668776000,\"deprecated\":false,\"value\":\"admin\",\"display\":\"admin\",\"filters\":[],\"hidden\":false,\"selected\":false}]}";

        AbstractGsrsControlledVocabularyDTO sut = mapper.readValue(json, AbstractGsrsControlledVocabularyDTO.class);

        GsrsControlledVocabularyDTO expected = GsrsControlledVocabularyDTO.builder()
                                                .id(1795)
                                                .version(1)
                                                .created(new Date(1473443705000L))
                                                .modified(new Date(1612668776000L))
                                                .domain("ACCESS_GROUP")
                                                .fields(Arrays.asList("ACCESS"))
                                                .vocabularyTermType("ix.ginas.models.v1.ControlledVocabulary")
                                                .terms(Arrays.asList(
                                                        GsrsVocabularyTermDTO.builder()
                                                                .id(43473L)
                                                                .version(1)
                                                                .created(new Date(1473443705000L))
                                                                .modified(new Date(1612668776000L))
                                                                .value("protected")
                                                                .display("PROTECTED")
                                                                .build(),
                                                        GsrsVocabularyTermDTO.builder()
                                                                .id(43474L)
                                                                .version(1)
                                                                .created(new Date(1473443705000L))
                                                                .modified(new Date(1612668776000L))
                                                                .value("admin")
                                                                .display("admin")
                                                                .build()
                                                ))
                                                .build();

        assertEquals(expected, sut);
    }

    @Test
    public void parseSingleDTOWithCodeSystemCVTerm() throws JsonProcessingException {
        String json ="{\"id\":1803,\"version\":1,\"created\":1473443705000,\"modified\":1612668777000,\"deprecated\":false,\"domain\":\"CODE_SYSTEM\",\"vocabularyTermType\":\"ix.ginas.models.v1.CodeSystemControlledVocabulary\",\"fields\":[\"codes.codeSystem\"],\"editable\":true,\"filterable\":false,\"terms\":[{\"id\":43669,\"version\":1,\"created\":1473443705000,\"modified\":1612668777000,\"deprecated\":false,\"value\":\"WHO-ATC\",\"display\":\"WHO-ATC\",\"description\":\"\",\"filters\":[],\"hidden\":false,\"selected\":false,\"systemCategory\":\"PHARMCLASS\"},{\"id\":43670,\"version\":1,\"created\":1473443705000,\"modified\":1612668777000,\"deprecated\":false,\"value\":\"ITIS\",\"display\":\"ITIS\",\"description\":\"\",\"filters\":[],\"hidden\":false,\"selected\":false,\"systemCategory\":\"ORGANISM\"}]}";

        AbstractGsrsControlledVocabularyDTO sut = mapper.readValue(json, AbstractGsrsControlledVocabularyDTO.class);

        assertTrue(sut instanceof GsrsCodeSystemControlledVocabularyDTO);

        assertEquals(sut, GsrsCodeSystemControlledVocabularyDTO.builder()
                                                                    .id(1803)
                                                                    .version(1)
                                                                    .created(new Date(1473443705000L))
                                                                    .modified(new Date(1612668777000L))
                                                                    .domain("CODE_SYSTEM")
                                                                    .vocabularyTermType("ix.ginas.models.v1.CodeSystemControlledVocabulary")
                                                                    .fields(Arrays.asList("codes.codeSystem"))
                                                                    .editable(true)
                                                                    .filterable(false)
                                                                    .terms(Arrays.asList(
                                                                            CodeSystemTermDTO.builder()
                                                                                    .id(43669L)
                                                                                    .version(1)
                                                                                    .created(new Date(1473443705000L))
                                                                                    .modified(new Date(1612668777000L))
                                                                                    .value("WHO-ATC")
                                                                                    .display("WHO-ATC")
                                                                                    .description("")
                                                                                    .systemCategory("PHARMCLASS")
                                                                                    .build(),
                                                                            CodeSystemTermDTO.builder()
                                                                                    .id(43670L)
                                                                                    .version(1)
                                                                                    .created(new Date(1473443705000L))
                                                                                    .modified(new Date(1612668777000L))
                                                                                    .value("ITIS")
                                                                                    .display("ITIS")
                                                                                    .description("")
                                                                                    .systemCategory("ORGANISM")
                                                                                    .build()

                                                                    ))
                                                                    .build());
    }

    @Test
    public void parseSingleDTOWithFragmentCvTerm() throws IOException {
        String json = "{\"id\":1799,\"version\":1,\"created\":1473443705000,\"modified\":1612668777000,\"deprecated\":false,\"domain\":\"AMINO_ACID_RESIDUE\",\"vocabularyTermType\":\"ix.ginas.models.v1.FragmentControlledVocabulary\",\"fields\":[],\"editable\":true,\"filterable\":false,\"terms\":[{\"id\":43623,\"version\":1,\"created\":1473443705000,\"modified\":1612668777000,\"deprecated\":false,\"value\":\"A\",\"display\":\"Alanine\",\"description\":\"Ala\",\"origin\":\"OF5P57N2ZX\",\"filters\":[],\"hidden\":false,\"selected\":false,\"fragmentStructure\":\"C[C@H](N[*])C([*])=O |$;;;_R1;;_R2;$|\",\"simplifiedStructure\":\"C[C@H](N)C(O)=O\"},{\"id\":43624,\"version\":1,\"created\":1473443705000,\"modified\":1612668777000,\"deprecated\":false,\"value\":\"C\",\"display\":\"Cysteine\",\"description\":\"Cys\",\"origin\":\"K848JZ4886\",\"filters\":[],\"hidden\":false,\"selected\":false,\"fragmentStructure\":\"[*]N[C@@H](CS[*])C([*])=O |$_R1;;;;;_R3;;_R2;$|\",\"simplifiedStructure\":\"N[C@@H](CS)C(O)=O\"}]}";

        AbstractGsrsControlledVocabularyDTO sut = mapper.readValue(json, AbstractGsrsControlledVocabularyDTO.class);

        assertTrue(sut instanceof GsrsFragmentControlledVocabularyDTO);


        assertEquals(sut, GsrsFragmentControlledVocabularyDTO.builder()
                .id(1803)
                .version(1)
                .created(new Date(1473443705000L))
                .modified(new Date(1612668777000L))
                .domain("AMINO_ACID_RESIDUE")
                .vocabularyTermType("ix.ginas.models.v1.FragmentControlledVocabulary")
                .fields(Collections.emptyList())
                .editable(true)
                .filterable(false)
                .terms(Arrays.asList(
                        FragmentTermDTO.builder()
                                .id(43624L)
                                .version(1)
                                .created(new Date(1473443705000L))
                                .modified(new Date(1612668777000L))
                                .value("A")
                                .display("Alanine")
                                .description("Ala")
                                .origin("OF5P57N2ZX")
                                .fragmentStructure("C[C@H](N[*])C([*])=O |$;;;_R1;;_R2;$|")
                                .simplifiedStructure("C[C@H](N)C(O)=O")
                        .build(),
                        FragmentTermDTO.builder()
                                .id(43623L)
                                .version(1)
                                .created(new Date(1473443705000L))
                                .modified(new Date(1612668777000L))
                                .value("C")
                                .display("Cysteine")
                                .description("Cys")
                                .origin("K848JZ4886")
                                .fragmentStructure("[*]N[C@@H](CS[*])C([*])=O |$_R1;;;;;_R3;;_R2;$|")
                                .simplifiedStructure("N[C@@H](CS)C(O)=O")
                                .build()
                ))
                .build());

    }
}
