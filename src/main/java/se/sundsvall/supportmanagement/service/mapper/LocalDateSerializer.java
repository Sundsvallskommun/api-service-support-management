package se.sundsvall.supportmanagement.service.mapper;

import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;
import java.lang.reflect.Type;
import java.time.LocalDate;

import static java.time.format.DateTimeFormatter.ISO_DATE;

/**
 * Writes a date as the date it is, and - more to the point - keeps Gson from reflecting into {@link LocalDate}.
 * <p>
 * Gson resolves the adapter for a field type when it binds the class, before any exclusion strategy has a say about
 * whether the field is written. A type it cannot reflect into therefore fails the whole snapshot even when the field
 * holding it is excluded, and {@code java.time} is closed to reflection on a modern JDK.
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
