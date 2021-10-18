package gsrs;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class ForceUpdateDirtyMakerMixinTest {

    public static abstract class AbstractMockMixin implements ForceUpdateDirtyMakerMixin{
        int o;
        public AbstractMockMixin(int i) {
            o=i;
        }
    }
    
    public static class MockMixin implements ForceUpdateDirtyMakerMixin{
        int o;
        public MockMixin(int i) {
            o=i;
        }
        
        @Override
        public boolean equals(Object obj) {
            if(obj==null)return false;
            if(!(obj instanceof MockMixin))return false;
            MockMixin om = (MockMixin)obj;
            return om.o == this.o;
        }
        @Override
        public int hashCode() {
            return this.o;
        }
        
    }
    
    public static class MockMixinExtended extends MockMixin{

        int o2;
        
        public MockMixinExtended(int i, int j) {
            super(i);
            this.o2=j;
        }
        
        

    }
    

    public static class MockMixinAbstractExtended extends AbstractMockMixin{

        int o2;
        
        public MockMixinAbstractExtended(int i, int j) {
            super(i);
            this.o2=j;
        }
        
        

    }
    
    @Test
    public void settingForceUpdateMakesDirty(){
        MockMixin mm = new MockMixin(5);
        assertFalse(mm.isAllDirty());
        assertFalse(mm.isDirty());
        mm.forceUpdate();
        assertTrue(mm.isAllDirty());
        assertTrue(mm.isDirty());
        mm.clearDirtyFields();
        assertFalse(mm.isAllDirty());
        assertFalse(mm.isDirty());
    }
    

    @Test
    public void settingAFieldDirtyCountsAsDirtyButNotAllDirty(){
        MockMixin mm = new MockMixin(5);
        assertFalse(mm.isAllDirty());
        assertFalse(mm.isDirty());
        assertFalse(mm.isDirty("o"));
        mm.setIsDirty("o");
        assertFalse(mm.isAllDirty());
        assertTrue(mm.isDirty());
        assertTrue(mm.isDirty("o"));
        mm.clearDirtyFields();
        assertFalse(mm.isAllDirty());
        assertFalse(mm.isDirty());
        assertFalse(mm.isDirty("o"));
    }
    

    @Test
    public void settingOneFieldDirtyCountsAsDirtyButNotAllDirtyAndNotOtherFieldDirty(){
        MockMixin mm = new MockMixin(5);
        assertFalse(mm.isAllDirty());
        assertFalse(mm.isDirty());
        assertFalse(mm.isDirty("o"));
        assertFalse(mm.isDirty("o2"));
        mm.setIsDirty("o");
        assertFalse(mm.isAllDirty());
        assertTrue(mm.isDirty());
        assertTrue(mm.isDirty("o"));
        assertFalse(mm.isDirty("o2"));
        mm.clearDirtyFields();
        assertFalse(mm.isAllDirty());
        assertFalse(mm.isDirty());
        assertFalse(mm.isDirty("o"));
        assertFalse(mm.isDirty("o2"));
    }
    

    @Test
    public void settingTwoFieldsDirtyCountsAsDirtyButNotAllDirty(){
        MockMixin mm = new MockMixin(5);
        assertFalse(mm.isAllDirty());
        assertFalse(mm.isDirty());
        assertFalse(mm.isDirty("o"));
        assertFalse(mm.isDirty("o2"));
        mm.setIsDirty("o");
        mm.setIsDirty("o2");
        assertFalse(mm.isAllDirty());
        assertTrue(mm.isDirty());
        assertTrue(mm.isDirty("o"));
        assertTrue(mm.isDirty("o2"));
        

        assertEquals(2,mm.getDirtyFields().size());
        
        mm.clearDirtyFields();
        assertFalse(mm.isAllDirty());
        assertFalse(mm.isDirty());
        assertFalse(mm.isDirty("o"));
        assertFalse(mm.isDirty("o2"));
    }
    
    
    
    //*******************************
    

    @Test
    public void settingForceUpdateMakesDirtyExtendAbstract(){
        MockMixinAbstractExtended mm = new MockMixinAbstractExtended(5,10);
        assertFalse(mm.isAllDirty());
        assertFalse(mm.isDirty());
        mm.forceUpdate();
        assertTrue(mm.isAllDirty());
        assertTrue(mm.isDirty());
        mm.clearDirtyFields();
        assertFalse(mm.isAllDirty());
        assertFalse(mm.isDirty());
    }
    

    @Test
    public void settingAFieldDirtyCountsAsDirtyButNotAllDirtyExtendedAbstract(){
        MockMixinAbstractExtended mm = new MockMixinAbstractExtended(5,10);
        assertFalse(mm.isAllDirty());
        assertFalse(mm.isDirty());
        assertFalse(mm.isDirty("o"));
        mm.setIsDirty("o");
        assertFalse(mm.isAllDirty());
        assertTrue(mm.isDirty());
        assertTrue(mm.isDirty("o"));
        mm.clearDirtyFields();
        assertFalse(mm.isAllDirty());
        assertFalse(mm.isDirty());
        assertFalse(mm.isDirty("o"));
    }
    

    @Test
    public void settingOneFieldDirtyCountsAsDirtyButNotAllDirtyAndNotOtherFieldDirtyExtendedAbstract(){
        MockMixinAbstractExtended mm = new MockMixinAbstractExtended(5,10);
        assertFalse(mm.isAllDirty());
        assertFalse(mm.isDirty());
        assertFalse(mm.isDirty("o"));
        assertFalse(mm.isDirty("o2"));
        mm.setIsDirty("o");
        assertFalse(mm.isAllDirty());
        assertTrue(mm.isDirty());
        assertTrue(mm.isDirty("o"));
        assertFalse(mm.isDirty("o2"));
        mm.clearDirtyFields();
        assertFalse(mm.isAllDirty());
        assertFalse(mm.isDirty());
        assertFalse(mm.isDirty("o"));
        assertFalse(mm.isDirty("o2"));
    }
    

    @Test
    public void settingTwoFieldsDirtyCountsAsDirtyButNotAllDirtyExtendedAbstract(){
        MockMixinAbstractExtended mm = new MockMixinAbstractExtended(5,10);
        assertFalse(mm.isAllDirty());
        assertFalse(mm.isDirty());
        assertFalse(mm.isDirty("o"));
        assertFalse(mm.isDirty("o2"));
        mm.setIsDirty("o");
        mm.setIsDirty("o2");
        assertFalse(mm.isAllDirty());
        assertTrue(mm.isDirty());
        assertTrue(mm.isDirty("o"));
        assertTrue(mm.isDirty("o2"));
        

        assertEquals(2,mm.getDirtyFields().size());
        
        mm.clearDirtyFields();
        assertFalse(mm.isAllDirty());
        assertFalse(mm.isDirty());
        assertFalse(mm.isDirty("o"));
        assertFalse(mm.isDirty("o2"));
    }
    
    //===================================
    
    
    
    
    


    @Test
    public void settingForceUpdateMakesDirtyExtend(){
        MockMixinExtended mm = new MockMixinExtended(5,10);
        assertFalse(mm.isAllDirty());
        assertFalse(mm.isDirty());
        mm.forceUpdate();
        assertTrue(mm.isAllDirty());
        assertTrue(mm.isDirty());
        mm.clearDirtyFields();
        assertFalse(mm.isAllDirty());
        assertFalse(mm.isDirty());
    }
    

    @Test
    public void settingAFieldDirtyCountsAsDirtyButNotAllDirtyExtended(){
        MockMixinExtended mm = new MockMixinExtended(5,10);
        assertFalse(mm.isAllDirty());
        assertFalse(mm.isDirty());
        assertFalse(mm.isDirty("o"));
        mm.setIsDirty("o");
        assertFalse(mm.isAllDirty());
        assertTrue(mm.isDirty());
        assertTrue(mm.isDirty("o"));
        mm.clearDirtyFields();
        assertFalse(mm.isAllDirty());
        assertFalse(mm.isDirty());
        assertFalse(mm.isDirty("o"));
    }
    

    @Test
    public void settingOneFieldDirtyCountsAsDirtyButNotAllDirtyAndNotOtherFieldDirtyExtended(){
        MockMixinExtended mm = new MockMixinExtended(5,10);
        assertFalse(mm.isAllDirty());
        assertFalse(mm.isDirty());
        assertFalse(mm.isDirty("o"));
        assertFalse(mm.isDirty("o2"));
        mm.setIsDirty("o");
        assertFalse(mm.isAllDirty());
        assertTrue(mm.isDirty());
        assertTrue(mm.isDirty("o"));
        assertFalse(mm.isDirty("o2"));
        mm.clearDirtyFields();
        assertFalse(mm.isAllDirty());
        assertFalse(mm.isDirty());
        assertFalse(mm.isDirty("o"));
        assertFalse(mm.isDirty("o2"));
    }
    

    @Test
    public void settingTwoFieldsDirtyCountsAsDirtyButNotAllDirtyExtended(){
        MockMixinExtended mm = new MockMixinExtended(5,10);
        assertFalse(mm.isAllDirty());
        assertFalse(mm.isDirty());
        assertFalse(mm.isDirty("o"));
        assertFalse(mm.isDirty("o2"));
        mm.setIsDirty("o");
        mm.setIsDirty("o2");
        assertFalse(mm.isAllDirty());
        assertTrue(mm.isDirty());
        assertTrue(mm.isDirty("o"));
        assertTrue(mm.isDirty("o2"));
        

        assertEquals(2,mm.getDirtyFields().size());
        
        mm.clearDirtyFields();
        assertFalse(mm.isAllDirty());
        assertFalse(mm.isDirty());
        assertFalse(mm.isDirty("o"));
        assertFalse(mm.isDirty("o2"));
    }
    
    
}
