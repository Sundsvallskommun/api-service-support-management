package se.sundsvall.supportmanagement.api.model.errand;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Random;
import org.hamcrest.MatcherAssert;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanConstructor;
import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanEquals;
import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanHashCode;
import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanToString;
import static com.google.code.beanmatchers.BeanMatchers.hasValidGettersAndSetters;
import static com.google.code.beanmatchers.BeanMatchers.registerValueGenerator;
import static java.time.OffsetDateTime.now;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.CoreMatchers.allOf;

class DecisionTest {

	@BeforeAll
	static void setup() {
		registerValueGenerator(() -> now().plusDays(new Random().nextInt()), OffsetDateTime.class);
		registerValueGenerator(() -> LocalDate.now().plusDays(new Random().nextInt(1000)), LocalDate.class);
	}

	@Test
	void bean() {
		MatcherAssert.assertThat(Decision.class, allOf(
			hasValidBeanConstructor(),
			hasValidGettersAndSetters(),
			hasValidBeanHashCode(),
			hasValidBeanEquals(),
			hasValidBeanToString()));
	}

	@Test
	void builder() {

		// Arrange
		final var id = "id";
		final var type = "PERMIT";
		final var status = "COMPLETED";
		final var title = "title";
		final var description = "description";
		final var dueAt = now().plusDays(10);
		final var completedAt = now().plusDays(20);
		final var outcome = "APPROVAL";
		final var method = "MANUAL";
		final var decidedBy = "decidedBy";
		final var decidedByRole = "DELEGATE";
		final var decidedAt = now().plusDays(1);
		final var legalBasis = "legalBasis";
		final var delegationReference = "3.2.1";
		final var justification = "justification";
		final var appealable = true;
		final var validFrom = LocalDate.of(2024, 3, 1);
		final var validTo = LocalDate.of(2025, 2, 28);
		final var investigationId = "investigationId";
		final var errandProcessId = "errandProcessId";
		final var terms = List.of(DecisionTerm.create());
		final var attachments = List.of(ArtefactAttachment.create());
		final var createdBy = "createdBy";
		final var modifiedBy = "modifiedBy";
		final var created = now();
		final var modified = now();
		final var version = 1L;

		// Act
		final var result = Decision.create()
			.withId(id)
			.withType(type)
			.withStatus(status)
			.withTitle(title)
			.withDescription(description)
			.withDueAt(dueAt)
			.withCompletedAt(completedAt)
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
			.withInvestigationId(investigationId)
			.withErrandProcessId(errandProcessId)
			.withTerms(terms)
			.withAttachments(attachments)
			.withCreatedBy(createdBy)
			.withModifiedBy(modifiedBy)
			.withCreated(created)
			.withModified(modified)
			.withVersion(version);

		// Assert
		assertThat(result).hasNoNullFieldsOrProperties();
		assertThat(result)
			.extracting(Decision::getId, Decision::getType, Decision::getStatus, Decision::getTitle, Decision::getDescription, Decision::getDueAt)
			.containsExactly(id, type, status, title, description, dueAt);
		assertThat(result)
			.extracting(Decision::getCompletedAt, Decision::getOutcome, Decision::getMethod, Decision::getDecidedBy, Decision::getDecidedByRole, Decision::getDecidedAt)
			.containsExactly(completedAt, outcome, method, decidedBy, decidedByRole, decidedAt);
		assertThat(result)
			.extracting(Decision::getLegalBasis, Decision::getDelegationReference, Decision::getJustification, Decision::getAppealable, Decision::getValidFrom, Decision::getValidTo)
			.containsExactly(legalBasis, delegationReference, justification, appealable, validFrom, validTo);
		assertThat(result)
			.extracting(Decision::getInvestigationId, Decision::getErrandProcessId, Decision::getTerms, Decision::getAttachments, Decision::getCreatedBy, Decision::getModifiedBy)
			.containsExactly(investigationId, errandProcessId, terms, attachments, createdBy, modifiedBy);
		assertThat(result)
			.extracting(Decision::getCreated, Decision::getModified, Decision::getVersion)
			.containsExactly(created, modified, version);
	}

	@Test
	void noDirtOnCreatedBean() {
		assertThat(Decision.create()).hasAllNullFieldsOrProperties();
		assertThat(new Decision()).hasAllNullFieldsOrProperties();
	}
}
