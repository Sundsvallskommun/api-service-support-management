package se.sundsvall.supportmanagement.service;

import jakarta.persistence.EntityManager;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.IntStream;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

/**
 * What these tests pin down is the bound the deleter exists to give: how much is handed over at a time, and that the
 * persistence context is emptied between chunks rather than only at the end. The chunk size is the class's own, so the
 * tests are written against how the chunks divide rather than against the number itself.
 */
@ExtendWith(MockitoExtension.class)
class ChunkedDeleterTest {

	@Mock
	private EntityManager entityManagerMock;

	@InjectMocks
	private ChunkedDeleter chunkedDeleter;

	@Test
	@DisplayName("Verification that every id is handed over, and that no chunk is larger than the one before it")
	void deleteInChunksCoversEveryId() {
		final var ids = idsNumbering(120);
		final var handedOver = new ArrayList<List<String>>();

		chunkedDeleter.deleteInChunks(ids, handedOver::add);

		assertThat(handedOver).isNotEmpty();
		assertThat(handedOver.stream().flatMap(List::stream).toList()).containsExactlyElementsOf(ids);
		assertThat(handedOver).allSatisfy(chunk -> assertThat(chunk).hasSizeLessThanOrEqualTo(handedOver.getFirst().size()));
	}

	@Test
	@DisplayName("Verification that the persistence context is emptied once per chunk, and flushed before it is, since clearing on its own would discard the removals")
	void deleteInChunksFlushesBeforeClearing() {
		final var ids = idsNumbering(120);
		final var chunks = new ArrayList<List<String>>();

		chunkedDeleter.deleteInChunks(ids, chunks::add);

		final InOrder inOrder = inOrder(entityManagerMock);
		chunks.forEach(_ -> {
			inOrder.verify(entityManagerMock).flush();
			inOrder.verify(entityManagerMock).clear();
		});
		inOrder.verifyNoMoreInteractions();
	}

	@Test
	@DisplayName("Verification that a chunk smaller than the chunk size is still handed over, rather than left behind as a remainder")
	void deleteInChunksHandsOverAPartialChunk() {
		final var ids = idsNumbering(1);

		chunkedDeleter.deleteInChunks(ids, chunk -> assertThat(chunk).isEqualTo(ids));

		verify(entityManagerMock).flush();
		verify(entityManagerMock).clear();
	}

	@Test
	@DisplayName("Verification that nothing to remove leaves the persistence context alone, since there is nothing to make room for")
	void deleteInChunksWithNothingToRemove() {
		chunkedDeleter.deleteInChunks(List.of(), _ -> {
			throw new AssertionError("Nothing should be handed over when there is nothing to remove");
		});

		verifyNoInteractions(entityManagerMock);
	}

	private static List<String> idsNumbering(final int count) {
		return IntStream.range(0, count)
			.mapToObj(String::valueOf)
			.toList();
	}
}
