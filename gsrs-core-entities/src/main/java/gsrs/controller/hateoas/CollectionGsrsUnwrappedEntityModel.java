package gsrs.controller.hateoas;


import com.fasterxml.jackson.annotation.JsonValue;

/**
 * Special version of {@link GsrsUnwrappedEntityModel} that wraps
 * a Collection of objects.  Since the GSRS REST API response
 * "unwraps" the collection and Jackson UnWrapped annotation
 * doesn't handle Collections we have to store it differently
 * with a different kind of annotation
 * to hide the obj reference to the collection
 * so that the response JSON look correct for the GSRS API Contract.
 * @param <T>
 */
class CollectionGsrsUnwrappedEntityModel<T> extends GsrsUnwrappedEntityModel<T> {

    public CollectionGsrsUnwrappedEntityModel(T obj, String view) {
        super(obj, view, false);
    }

    @Override
    @JsonValue // use JsonValue instead of JsonUnwrapped to make the collection unwrapped
    public T getObj() {
        return super.getObj();
    }
}
