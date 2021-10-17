package gsrs;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class MixInInterfaceTest {

    public static class MockMixin implements MixinInterface{
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
    
    @Test
    public void mixinCanSetAndGetAValue(){
        MockMixin mm = new MockMixin(5);
        mm._setField("test", "foo");
        String setValue = mm._getFieldOrDefault("test","fooDefault");
        assertEquals("foo", setValue);
    }

    @Test
    public void mixinCanClearAllValues(){
        MockMixin mm = new MockMixin(5);
        mm._setField("test", "foo");
        String setValue = mm._getFieldOrDefault("test","fooDefault");
        assertEquals("foo", setValue);
        mm._clearMixinStore();
        setValue=mm._getFieldOrDefault("test","fooDefault");
        assertEquals("fooDefault", setValue);
        
    }
    
    @Test
    public void mixinInstanceEqualToAnotherMixinInstanceDoesNotShareValues(){
        MockMixin mm1 = new MockMixin(5);
        MockMixin mm2 = new MockMixin(5);
        mm1._setField("test", "foo");
        String setValue = mm1._getFieldOrDefault("test","fooDefault");
        assertEquals("foo", setValue);
        String setValue2 = mm2._getFieldOrDefault("test","fooDefault");
        assertEquals("fooDefault", setValue2);
        
    }
    
    @Test
    public void mixinInstanceDoesNotLoseValuesWhenEqualsOrHashCodeChanges(){
        MockMixin mm = new MockMixin(5);
        mm._setField("test", "foo");
        String setValue = mm._getFieldOrDefault("test","fooDefault");
        assertEquals("foo", setValue);
        mm.o=10;
        setValue=mm._getFieldOrDefault("test","fooDefault");
        assertEquals("foo", setValue);
        
    }
    
    @Test
    public void mixinComputeIfAbsentDoesNotClobberIfSetAndDoesWriteIfNotSet(){
        MockMixin mm = new MockMixin(5);
        mm._setField("test", "foo");
        String setValue = mm._computeFieldIfAbsent("test", k->"fooAlt");
        assertEquals("foo", setValue);
        String setValue2 = mm._computeFieldIfAbsent("test2", k->"fooAlt");
        assertEquals("fooAlt", setValue2);
    }
    
    @Test
    public void mixinClearClearsSeveralSetFields(){
        MockMixin mm = new MockMixin(5);
        mm._setField("test1", "foo1");
        mm._setField("test2", "foo2");
        mm._setField("test3", "foo3");

        assertEquals("foo1",mm._getFieldOrDefault("test1", (String)null));
        assertEquals("foo2",mm._getFieldOrDefault("test2", (String)null));
        assertEquals("foo3",mm._getFieldOrDefault("test3", (String)null));
        
        mm._clearMixinStore();
        
        assertEquals(null,mm._getFieldOrDefault("test1", (String)null));
        assertEquals(null,mm._getFieldOrDefault("test2", (String)null));
        assertEquals(null,mm._getFieldOrDefault("test3", (String)null));
        
    }
    

    @Test
    public void mixinGetOrDefaultDoesNotSetValues(){
        MockMixin mm = new MockMixin(5);
        mm._getFieldOrDefault("test1", "foo1");
        mm._getFieldOrDefault("test2", "foo2");
        mm._getFieldOrDefault("test3", "foo3");
        assertEquals(null,mm._getFieldOrDefault("test1", (String)null));
        assertEquals(null,mm._getFieldOrDefault("test2", (String)null));
        assertEquals(null,mm._getFieldOrDefault("test3", (String)null));
    }
    
}
