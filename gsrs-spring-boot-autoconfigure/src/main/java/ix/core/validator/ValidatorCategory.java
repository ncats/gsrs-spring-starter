package ix.core.validator;

public class ValidatorCategory {
    private static final String RAW_DEFINITION = "DEFINITION";
    private static final String RAW_DUPLICATE_CHECK = "DUPLICATE_CHECK";
    private static final String RAW_ALL = "ALL";
    
    private final String keyword;
    
    public ValidatorCategory(String name) {
        this.keyword=name;
    }
    
    public static ValidatorCategory CATEGORY_DEFINITION() {
        return new ValidatorCategory(RAW_DEFINITION);
    }
    
    public static ValidatorCategory CATEGORY_DUPLICATE_CHECK() {
        return new ValidatorCategory(RAW_DUPLICATE_CHECK);
    }
    
    public static ValidatorCategory CATEGORY_ALL() {
        return new ValidatorCategory(RAW_ALL);
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((keyword == null) ? 0 : keyword.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        ValidatorCategory other = (ValidatorCategory) obj;
        if (keyword == null) {
            if (other.keyword != null)
                return false;
        } else if (!keyword.equals(other.keyword))
            return false;
        return true;
    }
    
    public static ValidatorCategory of(String n) {
        return new ValidatorCategory(n);
    }
    
    
}
