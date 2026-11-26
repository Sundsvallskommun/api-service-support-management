package se.sundsvall.supportmanagement.integration.db.search;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import org.hibernate.search.engine.backend.document.DocumentElement;
import org.hibernate.search.engine.backend.document.IndexFieldReference;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import se.sundsvall.supportmanagement.integration.db.model.ErrandEntity;

import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class IndexedAtBinderTest {

	private static final Instant NOW = Instant.parse("2026-11-26T01:00:00Z");

	@Mock
	private IndexFieldReference<Instant> fieldMock;

	@Mock
	private DocumentElement documentMock;

	@Test
	void writesTheMomentOfWritingWhateverTheErrand() {
		new IndexedAtBinder.Bridge(fieldMock, Clock.fixed(NOW, ZoneOffset.UTC)).write(documentMock, ErrandEntity.create(), null);

		verify(documentMock).addValue(fieldMock, NOW);
	}
}
