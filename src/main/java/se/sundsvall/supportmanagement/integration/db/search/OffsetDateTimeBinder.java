package se.sundsvall.supportmanagement.integration.db.search;

import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;
import java.time.OffsetDateTime;
import org.hibernate.search.backend.elasticsearch.ElasticsearchExtension;
import org.hibernate.search.mapper.pojo.bridge.ValueBridge;
import org.hibernate.search.mapper.pojo.bridge.binding.ValueBindingContext;
import org.hibernate.search.mapper.pojo.bridge.mapping.programmatic.ValueBinder;
import org.hibernate.search.mapper.pojo.bridge.runtime.ValueBridgeToIndexedValueContext;

import static java.time.format.DateTimeFormatter.ISO_OFFSET_DATE_TIME;

/**
 * Indexes a point in time as a date field that a query may address the way people write dates.
 * <p>
 * The date field Hibernate Search would map on its own accepts nothing but its full nanosecond timestamp with offset,
 * and the format of a field Hibernate Search maps cannot be changed, so a query like {@code created:[2025-01-01 TO
 * 2025-01-31]} would be refused. This maps the field natively instead, with a format that takes a date, a date with a
 * time, a full timestamp or epoch milliseconds, which is what a range in a query can then use. The price is that the
 * field is unknown to the sort DSL, so sorting on it is asked for as JSON, see the search service.
 */
public class OffsetDateTimeBinder implements ValueBinder {

	private static final String MAPPING = "{\"type\": \"date\", \"format\": \"strict_date_optional_time||epoch_millis\", \"doc_values\": true}";

	@Override
	public void bind(final ValueBindingContext<?> context) {
		context.bridge(OffsetDateTime.class, new Bridge(), context.typeFactory().extension(ElasticsearchExtension.get()).asNative().mapping(MAPPING));
	}

	static final class Bridge implements ValueBridge<OffsetDateTime, JsonElement> {

		@Override
		public JsonElement toIndexedValue(final OffsetDateTime value, final ValueBridgeToIndexedValueContext context) {
			return value == null ? null : new JsonPrimitive(value.format(ISO_OFFSET_DATE_TIME));
		}
	}
}
