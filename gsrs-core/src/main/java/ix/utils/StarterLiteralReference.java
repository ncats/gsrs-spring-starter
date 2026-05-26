package ix.utils;

import java.lang.ref.ReferenceQueue;
import java.lang.ref.SoftReference;

public class StarterLiteralReference<T>{
	
	private SoftReference<T> sr;
	private int hashcode;

	public StarterLiteralReference(T t){
		this.sr=new SoftReference<T>(t);
		this.hashcode=System.identityHashCode(t);
	}
	
	public StarterLiteralReference(T t, ReferenceQueue<T> rq){
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
		if(oref instanceof StarterLiteralReference){
			StarterLiteralReference<?> or=(StarterLiteralReference<?>)oref;
			T thisT = this.get();
			if(thisT==null) return false;
			return (thisT == or.get());
		}
		return false;
	}
	public static <T> StarterLiteralReference<T> of(T t) {
		return new StarterLiteralReference<T>(t);
	}
	public static <T> StarterLiteralReference<T> of(T t, ReferenceQueue<T> rq) {
        return new StarterLiteralReference<T>(t,rq);
    }
	
	public String toString(){
		return "Ref to:[" + sr.get().toString() + "]";
	}
}
