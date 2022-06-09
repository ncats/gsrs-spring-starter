package ix.core.search.text;

public interface IndexedTextEncoder{
    public String encode(String s);

    default String encodeQuery(String s){
        IndexedTextEncoder _this=this;
        return _this.encode(s);
    }

    default IndexedTextEncoder combine(IndexedTextEncoder enc){
        IndexedTextEncoder _this=this;
        return (s)->{
            String e=_this.encode(s);
            return enc.encode(e);
        };
    }

}
