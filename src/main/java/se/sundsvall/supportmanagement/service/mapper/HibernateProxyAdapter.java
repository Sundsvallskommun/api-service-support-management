package se.sundsvall.supportmanagement.service.mapper;

import com.google.gson.Gson;
import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;
import java.io.IOException;
import org.hibernate.Hibernate;

public class HibernateProxyAdapter<T> extends TypeAdapter<T> {
	private final Gson gson;

	public HibernateProxyAdapter(Gson gson) {
		this.gson = gson;
	}

	@Override
	public void write(JsonWriter out, T value) throws IOException {
		Object unproxiedValue = Hibernate.unproxy(value);
		gson.toJson(unproxiedValue, unproxiedValue.getClass(), out);
	}

	@Override
	public T read(JsonReader in) throws IOException {
		throw new UnsupportedOperationException("Deserialization not supported");
	}
}
