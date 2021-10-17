package ix.utils;

import java.lang.*;

public class LiteralReference<T>{
	
	private T o;
	

	public LiteralReference(T o){
		this.o=o;
	}
	
	public T get(){
		return o;
	}
	@Override
	public int hashCode(){
		return System.identityHashCode(o);
	}
	@Override
	public boolean equals(Object oref){
		if(oref==null)return false;
		if(oref instanceof LiteralReference){
			LiteralReference<?> or=(LiteralReference<?>)oref;
			return (this.o == or.o);
		}
		return false;
	}
	public static <T> LiteralReference<T> of(T t) {
		return new LiteralReference<T>(t);
	}
	
	public String toString(){
		return "Ref to:" + o.toString();
	}
}
