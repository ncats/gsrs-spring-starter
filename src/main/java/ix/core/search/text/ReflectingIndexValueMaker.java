package ix.core.search.text;

import ix.core.models.Keyword;
import ix.core.util.EntityUtils;
import ix.core.util.EntityUtils.EntityWrapper;
import ix.core.util.pojopointer.PojoPointer;
import org.apache.lucene.document.LongField;
//import org.apache.lucene.document.LongPoint;
//import org.apache.lucene.document.StoredField;
import org.apache.lucene.document.StringField;
import org.apache.lucene.facet.FacetField;
import org.springframework.stereotype.Component;

import java.util.Objects;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

import static org.apache.lucene.document.Field.Store.NO;
import static org.apache.lucene.document.Field.Store.YES;

public class ReflectingIndexValueMaker implements IndexValueMaker<Object>{
	public static final String DIM_CLASS = "ix.Class";

    @Override
    public Class<Object> getIndexedEntityClass() {
        return Object.class;
    }

    public class IndexingFieldCreator implements BiConsumer<PathStack, EntityUtils.EntityWrapper>{
		Consumer<IndexableValue> toAdd;
		private EntityWrapper firstValue=null;
		
		public IndexingFieldCreator(Consumer<IndexableValue> toAdd) {
			Objects.requireNonNull(toAdd);
			this.toAdd = toAdd;  //where to put the fields
		}
	
		public void acceptWithGeneric(PathStack path, EntityUtils.EntityWrapper<Object> ew) {
			toAdd.accept(new IndexableValueDirect(new FacetField(DIM_CLASS, ew.getKind())));

				ew.getId().ifPresent(o -> {
					String internalIdField = ew.getInternalIdField();
					if (o instanceof Long) {
						toAdd.accept(new IndexableValueDirect(new LongField(ew.getInternalIdField(), (Long) o, YES)));
						//katzelda October 2020:
						//Looks like newer versions of lucene removed LongField.
						// There was a LegacyLongField but that's gone now too and now there is LongPoint
						//but that doesn't store so you need to make 2 fields with the 2nd one to store with the same id
//						long value = (Long)o;
//						toAdd.accept(new IndexableValueDirect(new LongPoint(internalIdField, value)));
//						toAdd.accept(new IndexableValueDirect(new StoredField(internalIdField, value)));
					} else {
						toAdd.accept(new IndexableValueDirect(new StringField(internalIdField, o.toString(), YES)));  //Only Special case
					}
					toAdd.accept(new IndexableValueDirect(new StringField(ew.getIdField(), o.toString(), NO)));
				}); //
	

				if(ew.getEntityClass().equals(Keyword.class)){
					if(path.getDepth()>0){
						try{
							toAdd.accept(new IndexableValueFromRaw(path.getFirst(), ew.at(PojoPointer.fromJsonPointer("/term")).map(ew1->ew1.getRawValue().toString()).orElse(null), path.toPath()));
						}catch(Exception e){
							//this shouldn't be possible, but being defensive
							e.printStackTrace();
						}
					}

				}

				// primitive fields only, they should all get indexed
				ew.streamFieldsAndValues(f -> f.isPrimitive()).forEach(fi -> {
					path.pushAndPopWith(fi.k().getName(), () -> {
						toAdd.accept(IndexableValueFromIndexable.of( path.getFirst(), fi.v(), path.toPath(),
								fi.k().getIndexable()));
					});
				}); //Primitive fields
	
				ew.getDynamicFacet().ifPresent(fv -> {
					path.pushAndPopWith(fv.k(), () -> {
						toAdd.accept(new IndexableValueFromRaw(fv.k(), fv.v(), path.toPath()).dynamic().suggestable());
					});
				}); //Dynamic Facets
				
				ew.streamMethodKeywordFacets().forEach(kw -> {
					path.pushAndPopWith(kw.label, () -> {
						toAdd.accept(new IndexableValueFromRaw(kw.label, kw.getValue(), path.toPath()).dynamic().suggestable());
					});
				}); //Method keywords
	
				ew.streamMethodsAndValues(m -> m.isArrayOrCollection()).forEach(t -> {
					path.pushAndPopWith(t.k().getName(), () -> {
						t.k().forEach(t.v(), (i, o) -> {
							path.pushAndPopWith(i + "", () -> {
								toAdd.accept(IndexableValueFromIndexable.of( path.getFirst(), o,
										path.toPath(), t.k().getIndexable()));
							});
						});
					});
				});// each array / collection
	
				ew.streamMethodsAndValues(m -> !m.isArrayOrCollection()).forEach(t -> {
					path.pushAndPopWith(t.k().getName(), () -> {
						toAdd.accept(IndexableValueFromIndexable.of(path.getFirst(), t.v(), path.toPath(),
								t.k().getIndexable()));
					});
				});// each non-array
	
				ew.streamFieldsAndValues(f -> (!f.isPrimitive() && !f.isArrayOrCollection())).forEach(fi -> {
					path.pushAndPopWith(fi.k().getName(), () -> {
						if (fi.k().isEntityType()) {
							if (fi.k().isExplicitlyIndexable()) {
								toAdd.accept(IndexableValueFromIndexable.of(path.getFirst(), fi.v(),
										path.toPath(), fi.k().getIndexable()));
							}
						} else { // treat as string
							toAdd.accept(IndexableValueFromIndexable.of(path.getFirst(), fi.v(),
									path.toPath(), fi.k().getIndexable()));
						}
					}); // for each field with value
				}); // foreach non-primitive field
		}
	
		
		//Just had some generic problems, so this delegates
		// TODO: clean up
		@Override
		public void accept(PathStack t, EntityUtils.EntityWrapper u) {
			if(firstValue ==null){
				firstValue = u;
			}
			acceptWithGeneric(t, u);
		}

	}

	@Override
	public void createIndexableValues(Object t, Consumer<IndexableValue> consumer) {
		EntityWrapper<Object> ew=EntityWrapper.of(t);
		IndexingFieldCreator ifc= new IndexingFieldCreator(consumer);
		ew.traverse().execute(ifc);
	}

}
