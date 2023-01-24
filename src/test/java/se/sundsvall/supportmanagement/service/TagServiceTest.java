package se.sundsvall.supportmanagement.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import se.sundsvall.supportmanagement.integration.db.TagRepository;
import se.sundsvall.supportmanagement.integration.db.model.TagEntity;
import se.sundsvall.supportmanagement.integration.db.model.TagType;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static se.sundsvall.supportmanagement.integration.db.model.TagType.CATEGORY;
import static se.sundsvall.supportmanagement.integration.db.model.TagType.CLIENT_ID;
import static se.sundsvall.supportmanagement.integration.db.model.TagType.STATUS;
import static se.sundsvall.supportmanagement.integration.db.model.TagType.TYPE;

@ExtendWith(MockitoExtension.class)
class TagServiceTest {

	@Mock
	private TagRepository tagRepositoryMock;

	@InjectMocks
	private TagService tagService;

	@Test
	void findAllStatusTags() {

		// Setup
		final var statusTagEntityList = List.of(
			TagEntity.create().withName("Status-1"),
			TagEntity.create().withName("Status-2"),
			TagEntity.create().withName("Status-3"));

		// Mock
		when(tagRepositoryMock.findByType(any(TagType.class))).thenReturn(statusTagEntityList);

		// Call
		final var result = tagService.findAllStatusTags();

		// Verifications
		assertThat(result).containsExactly("Status-1", "Status-2", "Status-3");
		verify(tagRepositoryMock).findByType(STATUS);
	}

	@Test
	void findAllCategoryTags() {

		// Setup
		final var statusTagEntityList = List.of(
			TagEntity.create().withName("Category-1"),
			TagEntity.create().withName("Category-2"),
			TagEntity.create().withName("Category-3"));

		// Mock
		when(tagRepositoryMock.findByType(any(TagType.class))).thenReturn(statusTagEntityList);

		// Call
		final var result = tagService.findAllCategoryTags();

		// Verifications
		assertThat(result).containsExactly("Category-1", "Category-2", "Category-3");
		verify(tagRepositoryMock).findByType(CATEGORY);
	}

	@Test
	void findAllTypeTags() {

		// Setup
		final var statusTagEntityList = List.of(
			TagEntity.create().withName("Type-1"),
			TagEntity.create().withName("Type-2"),
			TagEntity.create().withName("Type-3"));

		// Mock
		when(tagRepositoryMock.findByType(any(TagType.class))).thenReturn(statusTagEntityList);

		// Call
		final var result = tagService.findAllTypeTags();

		// Verifications
		assertThat(result).containsExactly("Type-1", "Type-2", "Type-3");
		verify(tagRepositoryMock).findByType(TYPE);
	}

	@Test
	void findAllOriginTags() {

		// Setup
		final var statusTagEntityList = List.of(
			TagEntity.create().withName("ClientId-1"),
			TagEntity.create().withName("ClientId-2"),
			TagEntity.create().withName("ClientId-3"));

		// Mock
		when(tagRepositoryMock.findByType(any(TagType.class))).thenReturn(statusTagEntityList);

		// Call
		final var result = tagService.findAllClientIdTags();

		// Verifications
		assertThat(result).containsExactly("ClientId-1", "ClientId-2", "ClientId-3");
		verify(tagRepositoryMock).findByType(CLIENT_ID);
	}

	@Test
	void findAllTags() {

		// Setup
		final var tagEntityList = List.of(
			TagEntity.create().withName("Status-1").withType(STATUS),
			TagEntity.create().withName("Status-2").withType(STATUS),
			TagEntity.create().withName("Status-3").withType(STATUS),
			TagEntity.create().withName("Category-1").withType(CATEGORY),
			TagEntity.create().withName("Category-2").withType(CATEGORY),
			TagEntity.create().withName("Category-3").withType(CATEGORY),
			TagEntity.create().withName("Type-1").withType(TYPE),
			TagEntity.create().withName("Type-2").withType(TYPE),
			TagEntity.create().withName("Type-3").withType(TYPE),
			TagEntity.create().withName("ClientId-1").withType(CLIENT_ID),
			TagEntity.create().withName("ClientId-2").withType(CLIENT_ID),
			TagEntity.create().withName("ClientId-3").withType(CLIENT_ID));

		// Mock
		when(tagRepositoryMock.findAll()).thenReturn(tagEntityList);

		// Call
		final var result = tagService.findAllTags();

		// Verifications
		assertThat(result).isNotNull();
		assertThat(result.getStatusTags()).containsExactly("Status-1", "Status-2", "Status-3");
		assertThat(result.getCategoryTags()).containsExactly("Category-1", "Category-2", "Category-3");
		assertThat(result.getClientIdTags()).containsExactly("ClientId-1", "ClientId-2", "ClientId-3");
		assertThat(result.getTypeTags()).containsExactly("Type-1", "Type-2", "Type-3");

		verify(tagRepositoryMock).findAll();
	}
}
