package se.sundsvall.supportmanagement.integration.db.search;

import java.time.Clock;
import java.time.Instant;
import org.hibernate.search.engine.backend.document.DocumentElement;
import org.hibernate.search.engine.backend.document.IndexFieldReference;
import org.hibernate.search.mapper.pojo.bridge.TypeBridge;
import org.hibernate.search.mapper.pojo.bridge.binding.TypeBindingContext;
import org.hibernate.search.mapper.pojo.bridge.mapping.programmatic.TypeBinder;
import org.hibernate.search.mapper.pojo.bridge.runtime.TypeBridgeWriteContext;
import se.sundsvall.supportmanagement.integration.db.model.ErrandEntity;
import se.sundsvall.supportmanagement.model.ErrandIndex;

/**
 * Writes when the document was written, which is what a rebuild of a namespace tells the documents it wrote from the
 * ones it did not by.
 * <p>
 * Depends on nothing of the errand: the value is the moment of writing, whatever caused it.
 */
public class IndexedAtBinder implements TypeBinder {

	private final Clock clock;

	public IndexedAtBinder() {
		this(Clock.systemUTC());
	}

	IndexedAtBinder(final Clock clock) {
		this.clock = clock;
	}

	@Override
	public void bind(final TypeBindingContext context) {
		context.dependencies().useRootOnly();

		final var field = context.indexSchemaElement()
			.field(ErrandIndex.INDEXED_AT, f -> f.asInstant())
			.toReference();

		context.bridge(ErrandEntity.class, new Bridge(field, clock));
	}

	static final class Bridge implements TypeBridge<ErrandEntity> {

		private final IndexFieldReference<Instant> field;
		private final Clock clock;

		Bridge(final IndexFieldReference<Instant> field, final Clock clock) {
			this.field = field;
			this.clock = clock;
		}

		@Override
		public void write(final DocumentElement target, final ErrandEntity errand, final TypeBridgeWriteContext context) {
			target.addValue(field, clock.instant());
		}
	}
}
