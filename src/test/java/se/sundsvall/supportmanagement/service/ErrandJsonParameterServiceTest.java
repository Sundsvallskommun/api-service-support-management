package se.sundsvall.supportmanagement.service;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import se.sundsvall.dept44.problem.ThrowableProblem;
import se.sundsvall.supportmanagement.api.model.errand.JsonParameter;
import se.sundsvall.supportmanagement.integration.db.ErrandsRepository;
import se.sundsvall.supportmanagement.integration.db.model.ErrandEntity;
import se.sundsvall.supportmanagement.integration.db.model.JsonParameterEntity;
import tools.jackson.databind.node.JsonNodeFactory;

import static generated.se.sundsvall.accessmapper.Access.AccessLevelEnum.R;
import static generated.se.sundsvall.accessmapper.Access.AccessLevelEnum.RW;
import static jakarta.persistence.LockModeType.OPTIMISTIC_FORCE_INCREMENT;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.http.HttpStatus.NOT_FOUND;

@ExtendWith(MockitoExtension.class)
class ErrandJsonParameterServiceTest {

	private static final String NAMESPACE = "namespace";
	private static final String MUNICIPALITY_ID = "municipalityId";
	private static final String ERRAND_ID = "errandId";
	private static final String KEY = "formData";

	@Mock
	private ErrandsRepository errandsRepositoryMock;

	@Mock
	private AccessControlService accessControlServiceMock;

	@Mock
	private jakarta.persistence.EntityManager entityManagerMock;

	@Captor
	private ArgumentCaptor<ErrandEntity> errandEntityCaptor;

	@InjectMocks
	private ErrandJsonParameterService service;

	@Test
	void readJsonParameter() {
		final var entity = buildEntityWithJsonParameter(KEY, "schema-1.0", "{\"name\":\"test\"}");
		when(accessControlServiceMock.getErrand(any(), any(), any(), anyBoolean(), any(), any())).thenReturn(entity);

		final var result = service.readJsonParameter(NAMESPACE, MUNICIPALITY_ID, ERRAND_ID, KEY);

		assertThat(result.getKey()).isEqualTo(KEY);
		assertThat(result.getSchemaId()).isEqualTo("schema-1.0");
		verify(accessControlServiceMock).getErrand(NAMESPACE, MUNICIPALITY_ID, ERRAND_ID, false, R, RW);
		verifyNoMoreInteractions(accessControlServiceMock, errandsRepositoryMock);
	}

	@Test
	void readJsonParameterNotFound() {
		final var entity = ErrandEntity.create().withId(ERRAND_ID).withJsonParameters(new ArrayList<>());
		when(accessControlServiceMock.getErrand(any(), any(), any(), anyBoolean(), any(), any())).thenReturn(entity);

		assertThatExceptionOfType(ThrowableProblem.class)
			.isThrownBy(() -> service.readJsonParameter(NAMESPACE, MUNICIPALITY_ID, ERRAND_ID, "missing"))
			.satisfies(p -> assertThat(p.getStatus()).isEqualTo(NOT_FOUND));
	}

	@Test
	void updateJsonParameterExisting() {
		final var entity = buildEntityWithJsonParameter(KEY, "schema-1.0", "{\"name\":\"old\"}");
		when(accessControlServiceMock.getErrand(any(), any(), any(), anyBoolean(), any())).thenReturn(entity);
		when(errandsRepositoryMock.save(any())).thenAnswer(i -> i.getArgument(0));

		final var request = JsonParameter.create()
			.withKey(KEY)
			.withSchemaId("schema-2.0")
			.withValue(JsonNodeFactory.instance.objectNode().put("name", "new"));

		final var result = service.updateJsonParameter(NAMESPACE, MUNICIPALITY_ID, ERRAND_ID, KEY, null, request);

		assertThat(result.getKey()).isEqualTo(KEY);
		assertThat(result.getSchemaId()).isEqualTo("schema-2.0");
		verify(accessControlServiceMock).getErrand(NAMESPACE, MUNICIPALITY_ID, ERRAND_ID, true, RW);
		verify(entityManagerMock).lock(same(entity), eq(OPTIMISTIC_FORCE_INCREMENT));
		verify(errandsRepositoryMock).save(entity);
		verifyNoMoreInteractions(accessControlServiceMock, errandsRepositoryMock);
	}

	@Test
	void updateJsonParameterNew() {
		final var entity = ErrandEntity.create().withId(ERRAND_ID).withJsonParameters(new ArrayList<>());
		when(accessControlServiceMock.getErrand(any(), any(), any(), anyBoolean(), any())).thenReturn(entity);
		when(errandsRepositoryMock.save(any())).thenAnswer(i -> i.getArgument(0));

		final var request = JsonParameter.create()
			.withKey("newKey")
			.withSchemaId("schema-1.0")
			.withValue(JsonNodeFactory.instance.objectNode().put("x", 1));

		service.updateJsonParameter(NAMESPACE, MUNICIPALITY_ID, ERRAND_ID, "newKey", null, request);

		verify(errandsRepositoryMock).save(errandEntityCaptor.capture());
		assertThat(errandEntityCaptor.getValue().getJsonParameters()).hasSize(1);
		assertThat(errandEntityCaptor.getValue().getJsonParameters().getFirst().getKey()).isEqualTo("newKey");
	}

	@Test
	void updateJsonParameterNullList() {
		final var entity = ErrandEntity.create().withId(ERRAND_ID).withJsonParameters(null);
		when(accessControlServiceMock.getErrand(any(), any(), any(), anyBoolean(), any())).thenReturn(entity);
		when(errandsRepositoryMock.save(any())).thenAnswer(i -> i.getArgument(0));

		final var request = JsonParameter.create()
			.withKey(KEY)
			.withSchemaId("schema-1.0")
			.withValue(JsonNodeFactory.instance.objectNode());

		service.updateJsonParameter(NAMESPACE, MUNICIPALITY_ID, ERRAND_ID, KEY, null, request);

		verify(errandsRepositoryMock).save(errandEntityCaptor.capture());
		assertThat(errandEntityCaptor.getValue().getJsonParameters()).isNotNull().hasSize(1);
	}

	@Test
	void deleteJsonParameter() {
		final var entity = buildEntityWithJsonParameter(KEY, "schema-1.0", "{\"name\":\"test\"}");
		when(accessControlServiceMock.getErrand(any(), any(), any(), anyBoolean(), any())).thenReturn(entity);
		when(errandsRepositoryMock.save(any())).thenAnswer(i -> i.getArgument(0));

		service.deleteJsonParameter(NAMESPACE, MUNICIPALITY_ID, ERRAND_ID, KEY, null);

		verify(errandsRepositoryMock).save(errandEntityCaptor.capture());
		assertThat(errandEntityCaptor.getValue().getJsonParameters()).isEmpty();
		verify(entityManagerMock).lock(same(entity), eq(OPTIMISTIC_FORCE_INCREMENT));
	}

	@Test
	void deleteJsonParameterNotFound() {
		final var entity = ErrandEntity.create().withId(ERRAND_ID).withJsonParameters(new ArrayList<>());
		when(accessControlServiceMock.getErrand(any(), any(), any(), anyBoolean(), any())).thenReturn(entity);

		assertThatExceptionOfType(ThrowableProblem.class)
			.isThrownBy(() -> service.deleteJsonParameter(NAMESPACE, MUNICIPALITY_ID, ERRAND_ID, "missing", null))
			.satisfies(p -> assertThat(p.getStatus()).isEqualTo(NOT_FOUND));
	}

	@Test
	void findJsonParameterEntityOrElseThrow_found() {
		final var entity = buildEntityWithJsonParameter(KEY, "schema-1.0", null);
		final var result = service.findJsonParameterEntityOrElseThrow(entity, KEY);
		assertThat(result.getKey()).isEqualTo(KEY);
	}

	@Test
	void findJsonParameterEntityOrElseThrow_notFound() {
		final var entity = ErrandEntity.create().withId(ERRAND_ID).withJsonParameters(new ArrayList<>());
		assertThatExceptionOfType(ThrowableProblem.class)
			.isThrownBy(() -> service.findJsonParameterEntityOrElseThrow(entity, "missing"))
			.satisfies(p -> assertThat(p.getStatus()).isEqualTo(NOT_FOUND));
	}

	private ErrandEntity buildEntityWithJsonParameter(final String key, final String schemaId, final String value) {
		final var jsonParam = JsonParameterEntity.create()
			.withId(UUID.randomUUID().toString())
			.withKey(key)
			.withSchemaId(schemaId)
			.withValue(value);
		return ErrandEntity.create()
			.withId(ERRAND_ID)
			.withJsonParameters(new ArrayList<>(List.of(jsonParam)));
	}
}
