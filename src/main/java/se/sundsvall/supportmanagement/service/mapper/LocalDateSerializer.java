package se.sundsvall.supportmanagement.service.mapper;

import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;
import java.lang.reflect.Type;
import java.time.LocalDate;

import static java.time.format.DateTimeFormatter.ISO_DATE;

/**
 * Writes a {@link LocalDate} as an ISO date, and keeps Gson from reflecting into it.
 * <p>
 * Needed for a snapshot of any class holding a {@link LocalDate} field, including a field an exclusion strategy leaves
 * out.
 */
public class LocalDateSerializer implements JsonSerializer<LocalDate> {

	public static LocalDateSerializer create() {
		return new LocalDateSerializer();
	}

	@Override
	public JsonElement serialize(final LocalDate localDate, final Type type, final JsonSerializationContext context) {
		return new JsonPrimitive(ISO_DATE.format(localDate));
	}
}
