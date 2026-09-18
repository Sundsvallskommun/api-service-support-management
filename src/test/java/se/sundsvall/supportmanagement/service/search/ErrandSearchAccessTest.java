package se.sundsvall.supportmanagement.service.search;

import java.util.Set;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import se.sundsvall.dept44.problem.ThrowableProblem;
import se.sundsvall.dept44.support.Identifier;
import se.sundsvall.supportmanagement.integration.db.model.MetadataLabelEntity;
import se.sundsvall.supportmanagement.integration.db.model.enums.ProtectedResource;
import se.sundsvall.supportmanagement.service.AccessControlService;
import se.sundsvall.supportmanagement.service.AccessControlService.AccessScope;

import static generated.se.sundsvall.accessmapper.Access.AccessLevelEnum.R;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.http.HttpStatus.FORBIDDEN;

@ExtendWith(MockitoExtension.class)
class ErrandSearchAccessTest {

	private static final String NAMESPACE = "namespace";
	private static final String MUNICIPALITY_ID = "2281";
	private static final Set<MetadataLabelEntity> LABELS = Set.of(MetadataLabelEntity.create().withId("label"));

	@Mock
	private AccessControlService accessControlServiceMock;

	private Identifier user;

	@BeforeEach
	void setUp() {
		user = Identifier.create().withType(Identifier.Type.AD_ACCOUNT).withValue("joe01doe");
		Identifier.set(user);
	}

	@AfterEach
	void tearDown() {
		Identifier.remove();
	}

	private ErrandSearchAccess access() {
		return new ErrandSearchAccess(accessControlServiceMock);
	}

	@Test
	void everythingIsOpenWithoutAccessControl() {
		when(accessControlServiceMock.accessScope(NAMESPACE, MUNICIPALITY_ID, user, ProtectedResource.ERRAND, R)).thenReturn(new AccessScope(false, null, null));

		final var result = access().resolve(NAMESPACE, MUNICIPALITY_ID, user);

		assertThat(result.errand().enforced()).isFalse();
		assertThat(result.closedFields()).isEmpty();
		assertThat(access().searchableFields(result)).isEqualTo(ErrandSearchPredicates.DEFAULT_FIELDS);
		assertThatCode(() -> access().verifyQuery("communications.subject:x AND \\*.probability:3", result)).doesNotThrowAnyException();
	}

	@Test
	void errandsAreSearchedAtFullRead() {
		when(accessControlServiceMock.accessScope(any(), any(), any(), any(), any())).thenReturn(new AccessScope(true, LABELS, null));

		access().resolve(NAMESPACE, MUNICIPALITY_ID, user);

		org.mockito.Mockito.verify(accessControlServiceMock).accessScope(NAMESPACE, MUNICIPALITY_ID, user, ProtectedResource.ERRAND, R);
		ErrandSearchAccess.RESOURCE_FIELDS.keySet().forEach(resource -> org.mockito.Mockito.verify(accessControlServiceMock).accessScope(NAMESPACE, MUNICIPALITY_ID, user, resource, R));
	}

	@Test
	void resourcesTheLabelsDoNotReachAreClosed() {
		when(accessControlServiceMock.accessScope(any(), any(), any(), any(), any())).thenReturn(new AccessScope(true, LABELS, null));
		// Labels grant nothing on communications, and reach decisions only through reporting
		when(accessControlServiceMock.accessScope(NAMESPACE, MUNICIPALITY_ID, user, ProtectedResource.COMMUNICATION, R)).thenReturn(new AccessScope(true, null, null));
		when(accessControlServiceMock.accessScope(NAMESPACE, MUNICIPALITY_ID, user, ProtectedResource.DECISION, R)).thenReturn(new AccessScope(true, Set.of(), "joe01doe"));

		final var result = access().resolve(NAMESPACE, MUNICIPALITY_ID, user);

		assertThat(result.closedFields()).containsExactlyInAnyOrder("communications.", "decisions.");
		assertThat(access().searchableFields(result))
			.doesNotContain("communications.subject", "communications.messageBody", "decisions.title", "decisions.justification")
			.contains("title", "measures.title", "jsonParametersText");
	}

	@ParameterizedTest
	@ValueSource(strings = {
		"communications.subject:läcka",
		"title:vatten AND (status:new OR communications.messageBody:läcka)",
		"communications.sent:[2025-01-01 TO 2025-12-31]",
		"_exists_:communications.subject",
		"decisions.jsonParameters.form.answer.raw:yes"
	})
	void queryNamingAClosedResourceIsRefused(final String query) {
		when(accessControlServiceMock.accessScope(any(), any(), any(), any(), any())).thenReturn(new AccessScope(true, LABELS, null));
		when(accessControlServiceMock.accessScope(eq(NAMESPACE), eq(MUNICIPALITY_ID), eq(user), eq(ProtectedResource.COMMUNICATION), eq(R))).thenReturn(new AccessScope(true, null, null));
		when(accessControlServiceMock.accessScope(eq(NAMESPACE), eq(MUNICIPALITY_ID), eq(user), eq(ProtectedResource.DECISION), eq(R))).thenReturn(new AccessScope(true, null, null));
		final var result = access().resolve(NAMESPACE, MUNICIPALITY_ID, user);

		final var e = assertThrows(ThrowableProblem.class, () -> access().verifyQuery(query, result));

		assertThat(e.getStatus()).isEqualTo(FORBIDDEN);
		assertThat(e.getDetail()).matches("Resource 'errand/(communication|decision)' not searchable by user 'joe01doe'");
	}

	@ParameterizedTest
	@ValueSource(strings = {
		"jsonParameters.\\*.regNo:abc",
		"\\*.subject:x",
		"comm?nications.subject:x"
	})
	void wildcardFieldNamesAreRefusedWhileAResourceIsClosed(final String query) {
		when(accessControlServiceMock.accessScope(any(), any(), any(), any(), any())).thenReturn(new AccessScope(true, LABELS, null));
		when(accessControlServiceMock.accessScope(eq(NAMESPACE), eq(MUNICIPALITY_ID), eq(user), eq(ProtectedResource.COMMUNICATION), eq(R))).thenReturn(new AccessScope(true, null, null));
		final var result = access().resolve(NAMESPACE, MUNICIPALITY_ID, user);

		final var e = assertThrows(ThrowableProblem.class, () -> access().verifyQuery(query, result));

		assertThat(e.getStatus()).isEqualTo(FORBIDDEN);
		assertThat(e.getDetail()).isEqualTo("A wildcard in a field name is not available to user 'joe01doe', who may not search every resource of the errand");
	}

	@ParameterizedTest
	@ValueSource(strings = {
		"",
		"vatten läcka",
		"title:vatten AND measures.title:armatur",
		"\"communications.subject:inside a phrase is a phrase\"",
		"description:communications.subject",
		"stakeholders.lastName:berg* created:>=2025-01-01",
		"communications\\:literal"
	})
	void queriesThatStayWithinOpenResourcesPass(final String query) {
		when(accessControlServiceMock.accessScope(any(), any(), any(), any(), any())).thenReturn(new AccessScope(true, LABELS, null));
		when(accessControlServiceMock.accessScope(eq(NAMESPACE), eq(MUNICIPALITY_ID), eq(user), eq(ProtectedResource.COMMUNICATION), eq(R))).thenReturn(new AccessScope(true, null, null));
		final var result = access().resolve(NAMESPACE, MUNICIPALITY_ID, user);

		assertThatCode(() -> access().verifyQuery(query, result)).doesNotThrowAnyException();
	}
}
