package gsrs.config;

public class EntityContextLookup {

    public static String getContextFromEntityClass(String className) {
        String contextNameToUse =className;
        if(contextNameToUse.contains(".")) {
            String[] parts =contextNameToUse.split("\\.");
            contextNameToUse = parts[parts.length-1].toLowerCase() + "s";
        }
        return contextNameToUse;
    }
}
