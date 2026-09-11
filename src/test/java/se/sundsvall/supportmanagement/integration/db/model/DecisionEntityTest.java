package se.sundsvall.supportmanagement.integration.db.model;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Random;
import org.hamcrest.MatcherAssert;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import se.sundsvall.supportmanagement.integration.db.model.enums.DecisionMethod;
import se.sundsvall.supportmanagement.integration.db.model.enums.DecisionOutcome;
import se.sundsvall.supportmanagement.integration.db.model.enums.ItemStatus;

import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanConstructor;
import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanEqualsExcluding;
import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanHashCodeExcluding;
import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanToStringExcluding;
import static com.google.code.beanmatchers.BeanMatchers.hasValidGettersAndSetters;
import static com.google.code.beanmatchers.BeanMatchers.registerValueGenerator;
import static java.time.OffsetDateTime.now;
import static java.time.temporal.ChronoUnit.SECONDS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;
import static org.hamcrest.Matchers.allOf;

class DecisionEntityTest {

	// What the artefact points at rather than what it is. None of it identifies the artefact, and comparing it would
	// walk back into the errand the artefact already hangs on.
	private static final String[] RELATIONS = {
		"errandEntity", "investigationEntity", "terms", "attachments", "jsonParameterLinks"
	};

	@BeforeAll
	static void setup() {
		registerValueGenerator(() -> now().plusDays(new Random().nextInt()), OffsetDateTime.class);
		registerValueGenerator(() -> ItemStatus.values()[new Random().nextInt(ItemStatus.values().length)], ItemStatus.class);
		registerValueGenerator(() -> DecisionOutcome.values()[new Random().nextInt(DecisionOutcome.values().length)], DecisionOutcome.class);
		registerValueGenerator(() -> DecisionMethod.values()[new Random().nextInt(DecisionMethod.values().length)], DecisionMethod.class);
		registerValueGenerator(() -> LocalDate.now().plusDays(new Random().nextInt(1000)), LocalDate.class);
	}

	@Test
	void hasValidBean() {
		MatcherAssert.assertThat(DecisionEntity.class, allOf(
			hasValidBeanConstructor(),
			hasValidGettersAndSetters(),
			hasValidBeanHashCodeExcluding(RELATIONS),
			hasValidBeanEqualsExcluding(RELATIONS),
			hasValidBeanToStringExcluding(RELATIONS)));
	}

	@Test
	void hasValidBuilderMethods() {

		// Arrange
		final var id = "id";
		final var errandEntity = ErrandEntity.create().withId("errandId");
		final var municipalityId = "2281";
		final var namespace = "namespace";
		final var type = "type";
		final var status = ItemStatus.ACTIVE;
		final var title = "title";
		final var description = "description";
		final var dueAt = now().plusDays(10);
		final var completedAt = now().plusDays(20);
		final var createdBy = "createdBy";
		final var modifiedBy = "modifiedBy";
		final var created = now();
		final var modified = now();
		final var version = 1L;
		final var outcome = DecisionOutcome.APPROVAL;
		final var method = DecisionMethod.MANUAL;
		final var decidedBy = "jo12doe";
		final var decidedByRole = "DELEGATE";
		final var decidedAt = now().plusDays(1);
		final var legalBasis = "8 kap. 12 § alkohollagen";
		final var delegationReference = "3.2.1";
		final var justification = "justification";
		final var appealable = true;
		final var validFrom = LocalDate.of(2024, 3, 1);
		final var validTo = LocalDate.of(2025, 2, 28);
		final var investigationEntity = InvestigationEntity.create().withId("investigationId");
		final var errandProcessId = "errandProcessId";
		final var terms = List.of(DecisionTermEntity.create());
		final var attachments = List.of(DecisionAttachmentEntity.create());
		final var jsonParameterLinks = List.of(DecisionJsonParameterEntity.create());

		// Act
		final var result = DecisionEntity.create()
			.withId(id)
			.withErrandEntity(errandEntity)
			.withMunicipalityId(municipalityId)
			.withNamespace(namespace)
			.withType(type)
			.withStatus(status)
			.withTitle(title)
			.withDescription(description)
			.withDueAt(dueAt)
			.withCompletedAt(completedAt)
			.withCreatedBy(createdBy)
			.withModifiedBy(modifiedBy)
			.withCreated(created)
			.withModified(modified)
			.withVersion(version)
			.withOutcome(outcome)
			.withMethod(method)
			.withDecidedBy(decidedBy)
			.withDecidedByRole(decidedByRole)
			.withDecidedAt(decidedAt)
			.withLegalBasis(legalBasis)
			.withDelegationReference(delegationReference)
			.withJustification(justification)
			.withAppealable(appealable)
			.withValidFrom(validFrom)
			.withValidTo(validTo)
			.withInvestigationEntity(investigationEntity)
			.withErrandProcessId(errandProcessId)
			.withTerms(terms)
			.withAttachments(attachments)
			.withJsonParameterLinks(jsonParameterLinks);

		// Assert
		assertThat(result).hasNoNullFieldsOrProperties();
		assertThat(result.getId()).isEqualTo(id);
		assertThat(result.getErrandEntity()).isEqualTo(errandEntity);
		assertThat(result.getMunicipalityId()).isEqualTo(municipalityId);
		assertThat(result.getNamespace()).isEqualTo(namespace);
		assertThat(result.getType()).isEqualTo(type);
		assertThat(result.getStatus()).isEqualTo(status);
		assertThat(result.getTitle()).isEqualTo(title);
		assertThat(result.getDescription()).isEqualTo(description);
		assertThat(result.getDueAt()).isEqualTo(dueAt);
		assertThat(result.getCompletedAt()).isEqualTo(completedAt);
		assertThat(result.getCreatedBy()).isEqualTo(createdBy);
		assertThat(result.getModifiedBy()).isEqualTo(modifiedBy);
		assertThat(result.getCreated()).isEqualTo(created);
		assertThat(result.getModified()).isEqualTo(modified);
		assertThat(result.getVersion()).isEqualTo(version);
		assertThat(result.getOutcome()).isEqualTo(outcome);
		assertThat(result.getMethod()).isEqualTo(method);
		assertThat(result.getDecidedBy()).isEqualTo(decidedBy);
		assertThat(result.getDecidedByRole()).isEqualTo(decidedByRole);
		assertThat(result.getDecidedAt()).isEqualTo(decidedAt);
		assertThat(result.getLegalBasis()).isEqualTo(legalBasis);
		assertThat(result.getDelegationReference()).isEqualTo(delegationReference);
		assertThat(result.getJustification()).isEqualTo(justification);
		assertThat(result.getAppealable()).isEqualTo(appealable);
		assertThat(result.getValidFrom()).isEqualTo(validFrom);
		assertThat(result.getValidTo()).isEqualTo(validTo);
		assertThat(result.getInvestigationEntity()).isEqualTo(investigationEntity);
		assertThat(result.getErrandProcessId()).isEqualTo(errandProcessId);
		assertThat(result.getTerms()).isEqualTo(terms);
		assertThat(result.getAttachments()).isEqualTo(attachments);
		assertThat(result.getJsonParameterLinks()).isEqualTo(jsonParameterLinks);
	}

	@Test
	void onCreateSetsCreated() {
		final var entity = DecisionEntity.create();
		entity.onCreate();

		assertThat(entity.getCreated()).isCloseTo(now(), within(1, SECONDS));
		assertThat(entity).hasAllNullFieldsOrPropertiesExcept("created");
	}

	@Test
	void onUpdateSetsModified() {
		final var entity = DecisionEntity.create();
		entity.onUpdate();

		assertThat(entity.getModified()).isCloseTo(now(), within(1, SECONDS));
		assertThat(entity).hasAllNullFieldsOrPropertiesExcept("modified");
	}

	@Test
	void noDirtOnCreatedBean() {
		assertThat(DecisionEntity.create()).hasAllNullFieldsOrProperties();
		assertThat(new DecisionEntity()).hasAllNullFieldsOrProperties();
	}
}
