package se.sundsvall.supportmanagement.service.mapper;

import generated.se.sundsvall.accessmapper.Access;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;
import se.sundsvall.supportmanagement.api.model.access.ErrandFieldKeyAccess;
import se.sundsvall.supportmanagement.api.model.config.AccessLevel;
import se.sundsvall.supportmanagement.integration.db.model.enums.ErrandField;
import se.sundsvall.supportmanagement.integration.db.model.enums.ProtectedResource;
import se.sundsvall.supportmanagement.service.AccessControlService.ErrandAccessResolution;
import se.sundsvall.supportmanagement.service.AccessControlService.FieldGrant;

import static generated.se.sundsvall.accessmapper.Access.AccessLevelEnum.LR;
import static generated.se.sundsvall.accessmapper.Access.AccessLevelEnum.R;
import static generated.se.sundsvall.accessmapper.Access.AccessLevelEnum.RW;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

class ErrandAccessMapperTest {

	@Test
	void toErrandAccessHandlesNull() {
		assertThat(ErrandAccessMapper.toErrandAccess(null)).isNull();
	}

	@Test
	void toErrandAccessMapsEveryLevel() {
		final Map<ErrandField, FieldGrant> fields = new EnumMap<>(ErrandField.class);
		fields.put(ErrandField.TITLE, new FieldGrant(null, null));

		final var result = ErrandAccessMapper.toErrandAccess(new ErrandAccessResolution(LR, Map.of(ProtectedResource.COMMUNICATION, R), fields));

		assertThat(result.getLevel()).isEqualTo(AccessLevel.LR);
		assertThat(result.getResources()).extracting("resource", "level").containsExactly(tuple("errand/communication", AccessLevel.R));
		assertThat(result.getFields()).extracting("field").containsExactly("title");
	}

	/**
	 * A field holding no keyed collection carries neither, so a client is never handed an empty key list to read
	 * something into.
	 */
	@Test
	void toErrandAccessLeavesKeysOutOfAFieldHoldingNoKeyedCollection() {
		final Map<ErrandField, FieldGrant> fields = new EnumMap<>(ErrandField.class);
		fields.put(ErrandField.TITLE, new FieldGrant(null, null));

		final var field = ErrandAccessMapper.toErrandAccess(new ErrandAccessResolution(R, Map.of(), fields)).getFields().getFirst();

		assertThat(field.getAllKeys()).isNull();
		assertThat(field.getKeys()).isNull();
	}

	@Test
	void toErrandAccessMapsKeysInTheOrderTheyWereResolved() {
		final Map<String, Access.AccessLevelEnum> keys = new LinkedHashMap<>();
		keys.put("granted-key", RW);
		keys.put("readonly-key", R);
		final Map<ErrandField, FieldGrant> fields = new EnumMap<>(ErrandField.class);
		fields.put(ErrandField.PARAMETERS, new FieldGrant(false, keys));

		final var field = ErrandAccessMapper.toErrandAccess(new ErrandAccessResolution(RW, Map.of(), fields)).getFields().getFirst();

		assertThat(field.getAllKeys()).isFalse();
		assertThat(field.getKeys()).containsExactly(
			ErrandFieldKeyAccess.create().withKey("granted-key").withLevel(AccessLevel.RW),
			ErrandFieldKeyAccess.create().withKey("readonly-key").withLevel(AccessLevel.R));
	}
}
