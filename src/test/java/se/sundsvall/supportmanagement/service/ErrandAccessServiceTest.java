package se.sundsvall.supportmanagement.service;

import java.util.EnumMap;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import se.sundsvall.dept44.support.Identifier;
import se.sundsvall.supportmanagement.api.model.config.AccessLevel;
import se.sundsvall.supportmanagement.integration.db.model.ErrandEntity;
import se.sundsvall.supportmanagement.integration.db.model.enums.ErrandField;
import se.sundsvall.supportmanagement.integration.db.model.enums.ProtectedResource;
import se.sundsvall.supportmanagement.service.AccessControlService.ErrandAccessResolution;
import se.sundsvall.supportmanagement.service.AccessControlService.FieldGrant;

import static generated.se.sundsvall.accessmapper.Access.AccessLevelEnum.LR;
import static generated.se.sundsvall.accessmapper.Access.AccessLevelEnum.R;
import static generated.se.sundsvall.accessmapper.Access.AccessLevelEnum.RW;
import static java.util.UUID.randomUUID;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ErrandAccessServiceTest {

	private static final String NAMESPACE = "namespace";
	private static final String MUNICIPALITY_ID = "2281";
	private static final String ERRAND_ID = randomUUID().toString();
	private static final Identifier USER = Identifier.create().withType(Identifier.Type.AD_ACCOUNT).withValue("joe01doe");

	@Mock
	private AccessControlService accessControlServiceMock;

	@InjectMocks
	private ErrandAccessService service;

	/**
	 * The identifier is a thread local the request filter sets, so it is set and cleared here rather than left to
	 * whatever another test on the same thread happened to leave behind.
	 */
	@BeforeEach
	void setUp() {
		Identifier.set(USER);
	}

	@AfterEach
	void tearDown() {
		Identifier.remove();
	}

	@Test
	void readErrandAccess() {
		final var errandEntity = ErrandEntity.create().withId(ERRAND_ID);
		final Map<ErrandField, FieldGrant> fields = new EnumMap<>(ErrandField.class);
		fields.put(ErrandField.TITLE, new FieldGrant(null, null));

		when(accessControlServiceMock.getErrand(any(), any(), any(), eq(false), any(), any())).thenReturn(errandEntity);
		when(accessControlServiceMock.resolveErrandAccess(any(), any(), any(), any()))
			.thenReturn(new ErrandAccessResolution(RW, Map.of(ProtectedResource.COMMUNICATION, R), fields));

		final var result = service.readErrandAccess(NAMESPACE, MUNICIPALITY_ID, ERRAND_ID);

		assertThat(result.getLevel()).isEqualTo(AccessLevel.RW);
		assertThat(result.getFields()).extracting("field", "allKeys").containsExactly(tuple("title", null));
		assertThat(result.getResources()).extracting("resource", "level").containsExactly(tuple("errand/communication", AccessLevel.R));

		// Guarded on the errand itself at limited read, which is what every plain read of an errand asks for, and never
		// locked, since nothing is written.
		verify(accessControlServiceMock).getErrand(NAMESPACE, MUNICIPALITY_ID, ERRAND_ID, false, ProtectedResource.ERRAND, LR);
		// Resolved for the user the request carried, which is what the access mapper is asked about.
		verify(accessControlServiceMock).resolveErrandAccess(NAMESPACE, MUNICIPALITY_ID, USER, errandEntity);
		verifyNoMoreInteractions(accessControlServiceMock);
	}
}
