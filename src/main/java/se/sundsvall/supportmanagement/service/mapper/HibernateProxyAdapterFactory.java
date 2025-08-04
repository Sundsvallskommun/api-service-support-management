package se.sundsvall.supportmanagement.service.mapper;

import com.google.gson.Gson;
import com.google.gson.TypeAdapter;
import com.google.gson.TypeAdapterFactory;
import com.google.gson.reflect.TypeToken;
import org.hibernate.proxy.HibernateProxy;

public class HibernateProxyAdapterFactory implements TypeAdapterFactory {
	@Override
	public <T> TypeAdapter<T> create(Gson gson, TypeToken<T> typeToken) {
		Class<? super T> rawType = typeToken.getRawType();
		if (HibernateProxy.class.isAssignableFrom(rawType)) {
			return new HibernateProxyAdapter<>(gson);
		}
		return null;
	}
}
