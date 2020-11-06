package ix.utils.pojopatch;

import ix.ginas.models.utils.JSONEntity;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class PojoDiffReflectionTest {
    @Test
    public void getterFindsPrivateBooleanFieldCorrectly() throws Exception{
        BooleanFieldBug a = new BooleanFieldBug();

        a.setDefining(true);


        BooleanFieldBug b = new BooleanFieldBug();

        b.setDefining(false);

        PojoPatch patch =  PojoDiff.getDiff(a,b);

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
