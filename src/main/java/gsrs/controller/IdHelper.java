package gsrs.controller;

/**
 * {@link IdHelper} is a way to programactically
 * set the regular expressions for entity IDs in GSRS routes.
 *
 * Different entities may have different ID types (UUID vs Long vs String etc)
 * so without some helper object, we could not auto generate the standard GSRS
 * route paths that contain IDs.
 *
 * This interface tells the code that auto generates the routes
 * how to set up the regular expressions for IDs.
 * The standardized Route will have a placeholder String
 * to mean "the regular expression for the ID goes here"
 * and that placeholder will be replaced by the regex for the particular ID type
 * by an IdHelper implementation.
 *
 * There are a few built in often used IdHelper implementations in {@link IdHelpers} enum
 * but clients can implement their own as well.
 */
public interface IdHelper {

    /**
     * Return the regular expression of the ID as a String.
     * @return a String regex, can not be null or empty.
     */
    String getRegexAsString();
    /**
     * Return a regular expression to mean "not an ID". Often
     * there are multiple routes with similar paths
     * and we want the route with the ID in the path to mean one thing
     * and other routes that don't meet the ID regular expression to do something else.
     *
     * @return a String regex to mean "not an ID". , can not be null or empty.
     */
    String getInverseRegexAsString();

    default String replaceId(String input, String idLiteral){
        return input.replace(idLiteral, getRegexAsString());
    }
    default String replaceInverseId(String input, String idLiteral){
        return input.replace(idLiteral, getInverseRegexAsString());
    }

}
