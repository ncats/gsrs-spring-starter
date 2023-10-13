package gsrs.config;

public class EntityContextLookup {

    // TODO: This should eventually use the registry of services to make a 
    // map rather than doing this hack, but it works for the cases that
    // are currently being used.
    public static String getContextFromEntityClass(String className) {
        String contextNameToUse =className;
        if(contextNameToUse.contains(".")) {
            String[] parts =contextNameToUse.split("\\.");
            contextNameToUse = parts[parts.length-1].toLowerCase() + "s";
        }
        return contextNameToUse;
    }
}
