package ix.utils;

import java.lang.ref.ReferenceQueue;
import java.lang.ref.SoftReference;

public class LiteralReference<T>{
	
	private SoftReference<T> sr;
	private int hashcode;

	public LiteralReference(T t){
		this.sr=new SoftReference<T>(t);
		this.hashcode=System.identityHashCode(t);
	}
	
	public LiteralReference(T t, ReferenceQueue rq){
        this.sr=new SoftReference<T>(t,rq);
        this.hashcode=System.identityHashCode(t);
    }
	
	public boolean isStale() {
	    //there are potentially more efficient ways to do this?
	    return this.get() == null;
	}
	
	public T get(){
		return sr.get();
	}
	@Override
	public int hashCode(){
		return this.hashcode;
	}
	@Override
	public boolean equals(Object oref){
		if(oref==null)return false;
		if(oref instanceof LiteralReference){
			LiteralReference<?> or=(LiteralReference<?>)oref;
			T thisT = this.get();
			if(thisT==null) return false;
			return (thisT == or.get());
		}
		return false;
	}
	public static <T> LiteralReference<T> of(T t) {
		return new LiteralReference<T>(t);
	}
	public static <T> LiteralReference<T> of(T t, ReferenceQueue rq) {
        return new LiteralReference<T>(t,rq);
    }
	
	public String toString(){
		return "Ref to:[" + sr.get().toString() + "]";
	}
}
