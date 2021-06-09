package gsrs;


import com.fasterxml.jackson.databind.node.BooleanNode;
import static gsrs.assertions.GsrsMatchers.*;
import ix.ginas.models.utils.JSONEntity;
import ix.utils.pojopatch.Change;
import ix.utils.pojopatch.PojoDiff;
import ix.utils.pojopatch.PojoPatch;
import org.junit.jupiter.api.Test;


import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.hamcrest.MatcherAssert.*;
import static org.hamcrest.Matchers.*;

public class PojoDiffReflectionTests {
    @Test
    public void getterFindsPrivateBooleanFieldCorrectly() throws Exception{
        BooleanFieldBug a = new BooleanFieldBug();

        a.setDefining(true);


        BooleanFieldBug b = new BooleanFieldBug();

        b.setDefining(false);

        PojoPatch patch =  PojoDiff.getDiff(a,b);

        List<Change> changes = patch.getChanges();


        assertEquals(1, changes.size());
        assertThat(changes.get(0), is(matchesExample(Change.builder()
                                                        .path("/defining")
                                                        .op("replace")
                                                        .newValue(BooleanNode.FALSE)
                .build())));
        System.out.println(patch.getChanges());

        patch.apply(a);

        assertFalse(a.isDefining());

    }

    public static class BooleanFieldBug{
        @JSONEntity(title = "Defining")
        private Boolean defining;

        public Boolean isDefining() {
            return defining;
        }

        public void setDefining(Boolean defining) {
            this.defining = defining;
        }
    }
}
