package se.sundsvall.supportmanagement.integration.db.search;

import org.hibernate.search.engine.backend.document.DocumentElement;
import org.hibernate.search.mapper.pojo.bridge.TypeBridge;
import org.hibernate.search.mapper.pojo.bridge.binding.TypeBindingContext;
import org.hibernate.search.mapper.pojo.bridge.mapping.programmatic.TypeBinder;
import org.hibernate.search.mapper.pojo.bridge.runtime.TypeBridgeWriteContext;
import se.sundsvall.supportmanagement.integration.db.model.ErrandEntity;

/**
 * Declares {@link ErrandIndex#NO_OPEN_FIELD} and writes it on nothing.
 * <p>
 * A query string is searched in a list of fields, and the list may not be empty. Where a route of a grant opens no
 * field of the errand to a word without a field, something has to stand in for the list, and whatever stands in is
 * searched: the field the errands are filtered on did the job until a user searching for the municipality id itself
 * was answered with every errand the route reaches. A field no errand carries answers no word, and a query that names
 * its fields still runs beside it - which is the whole point of standing in rather than refusing the clause.
 * <p>
 * Declared through a binder because the index holds it and the entity does not: there is no property to hang it on,
 * and nothing to write when the errand is indexed.
 */
public class NoOpenFieldBinder implements TypeBinder {

	@Override
	public void bind(final TypeBindingContext context) {
		// Nothing is read from the errand, so nothing about it can make this field stale
		context.dependencies().useRootOnly();

		context.indexSchemaElement().field(ErrandIndex.NO_OPEN_FIELD, f -> f.asString()).toReference();

		context.bridge(ErrandEntity.class, new Bridge());
	}

	static final class Bridge implements TypeBridge<ErrandEntity> {

		@Override
		public void write(final DocumentElement target, final ErrandEntity errand, final TypeBridgeWriteContext context) {
			// Deliberately empty: the field exists to be searched in vain
		}
	}
}
