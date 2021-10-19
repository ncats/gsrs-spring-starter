package gsrs;


import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;

import org.junit.Test;

import ix.utils.LiteralReference;

public class LiteralReferenceTest{
	
	public static class MockThing{
		int i=0;
		MockThing(int i){
			this.i=i;
		}
		@Override
		public boolean equals(Object o){
			if(!(o instanceof MockThing)){
				return false;
			}
			return ((MockThing)o).i==i;
		}
		
		@Override
        public int hashCode(){
            return i;
        }
		public static MockThing of(int i){
			return new MockThing(i);
		}
	}
	

	@Test
	public void sameLiteralReferenceIsEqualToItself(){
		LiteralReference<MockThing> lr = LiteralReference.of(MockThing.of(2));
		assertEquals(lr,lr);
	}
	
	
	@Test
	public void sameLiteralDifferentLiteralReferencesAreEqualToEachOther(){
		MockThing tst= MockThing.of(20);
		LiteralReference<MockThing> lr1 = LiteralReference.of(tst);
		LiteralReference<MockThing> lr2 = LiteralReference.of(tst);
		assertEquals(lr1.hashCode(),lr2.hashCode());
		assertEquals(lr1,lr2);
	}
	

	@Test
	public void differentInstancesOfEquivalentObjectsStillDifferent(){
		
		LiteralReference<MockThing> lr1 = LiteralReference.of(MockThing.of(5));
		LiteralReference<MockThing> lr2 = LiteralReference.of(MockThing.of(5));
		
		assertNotEquals(lr1,lr2);
	}
	
	@Test
	public void differentInstancesOfEquivalentObjectsStillSameWhenFetched(){
		
		LiteralReference<MockThing> lr1 = LiteralReference.of(MockThing.of(5));
		LiteralReference<MockThing> lr2 = LiteralReference.of(MockThing.of(5));
		
		assertEquals(lr1.get(),lr2.get());
	}
	
	@Test
    public void hashCodeForLiteralReferenceShouldNotChange(){
        MockThing mt = MockThing.of(5);
        LiteralReference<MockThing> lr1 = LiteralReference.of(mt);
        int hc1=lr1.hashCode();
        mt.i=7;
        LiteralReference<MockThing> lr2 = LiteralReference.of(mt);
        int hc2=lr2.hashCode();
        
        assertEquals(hc1,hc2);
        assertEquals(lr1,lr2);
        
    }
}
