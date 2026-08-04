package se.sundsvall.supportmanagement.service.mapper;

import com.google.gson.ExclusionStrategy;
import com.google.gson.FieldAttributes;

class VersionExclusionStrategy implements ExclusionStrategy {

	static ExclusionStrategy create() {
		return new VersionExclusionStrategy();
	}

	@Override
	public boolean shouldSkipField(final FieldAttributes f) {
		return "version".equals(f.getName());
	}

	@Override
	public boolean shouldSkipClass(final Class<?> clazz) {
		return false;
	}
}
