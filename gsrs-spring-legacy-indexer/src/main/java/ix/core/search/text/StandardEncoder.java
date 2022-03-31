package ix.core.search.text;

public interface StandardEncoder{
    public static String encode(String s) {
        return null;
    }

    default StandardEncoder combine(StandardEncoder enc){
        StandardEncoder _this=this;
        return (s)->{
            String e=_this.encode(s);
            return enc.encode(e);
        };
    }
}

