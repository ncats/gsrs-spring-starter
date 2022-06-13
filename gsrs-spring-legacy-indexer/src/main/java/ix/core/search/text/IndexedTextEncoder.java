package ix.core.search.text;

public interface IndexedTextEncoder{
    public String encode(String s);

    default String encodeQuery(String s){
        return encode(s);
    }

    default IndexedTextEncoder combine(IndexedTextEncoder enc){
        IndexedTextEncoder _this=this;
        return new IndexedTextEncoder (){
            @Override
            public String encode(String s){
                return enc.encode(_this.encode(s));
            }

            @Override
            public String encodeQuery(String s){
                return enc.encodeQuery(_this.encodeQuery(s));
            }
        };
    }


}
