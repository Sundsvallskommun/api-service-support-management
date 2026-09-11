package se.sundsvall.supportmanagement.integration.db.model;

import com.google.code.beanmatchers.BeanMatchers;
import java.time.OffsetDateTime;
import java.util.Random;
import org.hamcrest.MatcherAssert;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanConstructor;
import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanEqualsExcluding;
import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanHashCodeExcluding;
import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanToStringExcluding;
import static com.google.code.beanmatchers.BeanMatchers.hasValidGettersAndSetters;
import static java.time.OffsetDateTime.now;
import static java.time.temporal.ChronoUnit.SECONDS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;
import static org.hamcrest.Matchers.allOf;

class InvestigationAttachmentEntityTest {

	// The link is identified by its own id and by what it says about the attachment - the order it is shown in. The two
	// sides it points at are not part of that, and walking into them would run back into the errand they both belong to.
	private static final String[] RELATIONS = {
		"investigationEntity", "attachmentEntity"
	};

	@BeforeAll
	static void setup() {
		BeanMatchers.registerValueGenerator(() -> now().plusDays(new Random().nextInt()), OffsetDateTime.class);
	}

	@Test
	void hasValidBean() {
		MatcherAssert.assertThat(InvestigationAttachmentEntity.class, allOf(
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
		final var owner = InvestigationEntity.create().withId("ownerId");
		final var attachmentEntity = AttachmentEntity.create().withId("attachmentId");
		final var sortOrder = 1;
		final var created = now();
		final var createdBy = "createdBy";

		// Act
		final var result = InvestigationAttachmentEntity.create()
			.withId(id)
			.withInvestigationEntity(owner)
			.withAttachmentEntity(attachmentEntity)
			.withSortOrder(sortOrder)
			.withCreated(created)
			.withCreatedBy(createdBy);

		// Assert
		assertThat(result).hasNoNullFieldsOrProperties();
		assertThat(result.getId()).isEqualTo(id);
		assertThat(result.getInvestigationEntity()).isEqualTo(owner);
		assertThat(result.getAttachmentEntity()).isEqualTo(attachmentEntity);
		assertThat(result.getSortOrder()).isEqualTo(sortOrder);
		assertThat(result.getCreated()).isEqualTo(created);
		assertThat(result.getCreatedBy()).isEqualTo(createdBy);
	}

	@Test
	void onCreateSetsCreated() {
		final var entity = new InvestigationAttachmentEntity();
		entity.onCreate();

		assertThat(entity.getCreated()).isCloseTo(now(), within(1, SECONDS));
		assertThat(entity).hasAllNullFieldsOrPropertiesExcept("created");
	}

	@Test
	void noDirtOnCreatedBean() {
		assertThat(InvestigationAttachmentEntity.create()).hasAllNullFieldsOrProperties();
		assertThat(new InvestigationAttachmentEntity()).hasAllNullFieldsOrProperties();
	}
}
