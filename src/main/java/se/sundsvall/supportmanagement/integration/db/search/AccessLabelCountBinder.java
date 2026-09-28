package se.sundsvall.supportmanagement.integration.db.search;

import org.hibernate.search.engine.backend.document.DocumentElement;
import org.hibernate.search.engine.backend.document.IndexFieldReference;
import org.hibernate.search.engine.backend.types.Aggregable;
import org.hibernate.search.mapper.pojo.bridge.TypeBridge;
import org.hibernate.search.mapper.pojo.bridge.binding.TypeBindingContext;
import org.hibernate.search.mapper.pojo.bridge.mapping.programmatic.TypeBinder;
import org.hibernate.search.mapper.pojo.bridge.runtime.TypeBridgeWriteContext;
import se.sundsvall.supportmanagement.integration.db.model.ErrandEntity;

import static java.util.Objects.isNull;

/**
 * Writes how many access labels an errand carries, which is what the label filter of a search counts against.
 * <p>
 * The filter asks that every access label of the errand is among the labels the user holds. An index can ask how many
 * of
 * a field's values lie within a set, but not whether all of them do, so the number to reach is written beside them and
 * the question becomes "at least this many of them", which is the same question. Said this way rather than as "none of
 * the labels you lack", which needed the labels of the namespace to be known and therefore fresh, and let a label
 * nobody had heard of yet pass for allowed.
 * <p>
 * A count that disagreed with the labels beside it would be worse than the filter it replaces, so the binder declares
 * the labels as what it is derived from: an errand whose access labels change is reindexed, count and all.
 */
public class AccessLabelCountBinder implements TypeBinder {

	@Override
	public void bind(final TypeBindingContext context) {
		context.dependencies().use("accessLabels");

		// Aggregable for the doc values alone: the filter counts against this field, and a count is read from doc values
		// rather than from the index, which is what the covering query asks for
		final var field = context.indexSchemaElement()
			.field(ErrandIndex.ACCESS_LABEL_COUNT, f -> f.asInteger().aggregable(Aggregable.YES))
			.toReference();

		context.bridge(ErrandEntity.class, new Bridge(field));
	}

	static final class Bridge implements TypeBridge<ErrandEntity> {

		private final IndexFieldReference<Integer> field;

		Bridge(final IndexFieldReference<Integer> field) {
			this.field = field;
		}

		@Override
		public void write(final DocumentElement target, final ErrandEntity errand, final TypeBridgeWriteContext context) {
			target.addValue(field, isNull(errand.getAccessLabels()) ? 0 : errand.getAccessLabels().size());
		}
	}
}
