package se.sundsvall.supportmanagement.service.mapper;

import static org.assertj.core.api.Assertions.assertThat;

import org.apache.commons.lang3.reflect.FieldUtils;
import org.junit.jupiter.api.Test;

import com.google.gson.FieldAttributes;

import se.sundsvall.supportmanagement.integration.db.model.AttachmentEntity;
import se.sundsvall.supportmanagement.integration.db.model.ErrandEntity;
import se.sundsvall.supportmanagement.integration.db.model.StakeholderEntity;

class CircularReferenceExclusionStrategyTest {

	private static final CircularReferenceExclusionStrategy INSTANCE = CircularReferenceExclusionStrategy.create();

	@Test
	void create() {
		assertThat(INSTANCE).isNotNull().isInstanceOf(CircularReferenceExclusionStrategy.class);
	}

	@Test
	void shouldSkipClass() {
		assertThat(INSTANCE.shouldSkipClass(Object.class)).isFalse();
		assertThat(INSTANCE.shouldSkipClass(null)).isFalse();
	}

	@Test
	void shouldNotSkipFieldForNonDeclaredClass() {
		assertThat(INSTANCE.shouldSkipField(new FieldAttributes(FieldUtils.getField(ErrandEntity.class, "id", true)))).isFalse();
	}

	@Test
	void shouldNotSkipFieldForNonDeclaredMethodInDeclaredClass() {
		assertThat(INSTANCE.shouldSkipField(new FieldAttributes(FieldUtils.getField(AttachmentEntity.class, "id", true)))).isFalse();
		assertThat(INSTANCE.shouldSkipField(new FieldAttributes(FieldUtils.getField(StakeholderEntity.class, "id", true)))).isFalse();
	}

	@Test
	void shouldSkipFieldForDeclaredMethodInDeclaredClass() {
		assertThat(INSTANCE.shouldSkipField(new FieldAttributes(FieldUtils.getField(AttachmentEntity.class, "errandEntity", true)))).isTrue();
		assertThat(INSTANCE.shouldSkipField(new FieldAttributes(FieldUtils.getField(StakeholderEntity.class, "errandEntity", true)))).isTrue();
	}
}
