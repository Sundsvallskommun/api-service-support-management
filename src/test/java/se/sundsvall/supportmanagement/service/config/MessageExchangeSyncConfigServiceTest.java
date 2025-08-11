package se.sundsvall.supportmanagement.service.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import se.sundsvall.supportmanagement.api.model.config.MessageExchangeSync;
import se.sundsvall.supportmanagement.integration.db.MessageExchangeSyncRepository;
import se.sundsvall.supportmanagement.integration.db.model.MessageExchangeSyncEntity;
import se.sundsvall.supportmanagement.service.mapper.MessageExchangeSyncMapper;

@ExtendWith(MockitoExtension.class)
class MessageExchangeSyncConfigServiceTest {

	@Mock
	private MessageExchangeSyncRepository messageExchangeSyncRepositoryMock;

	@Mock
	private MessageExchangeSyncMapper messageExchangeSyncMapperMock;

	@Captor
	private ArgumentCaptor<MessageExchangeSyncEntity> entityArgumentCaptor;

	@InjectMocks
	private MessageExchangeSyncConfigService syncService;

	@Test
	void create() {
		final var request = new MessageExchangeSync();
		final var municipalityId = "municipalityId";
		final var id = 99L;
		final var entity = new MessageExchangeSyncEntity().withId(id);

		when(messageExchangeSyncMapperMock.toEntity(any(), any())).thenReturn(entity);
		when(messageExchangeSyncRepositoryMock.save(any())).thenReturn(entity);

		var result = syncService.create(request, municipalityId);

		assertThat(result).isEqualTo(id);
		verify(messageExchangeSyncMapperMock).toEntity(same(request), eq(municipalityId));
		verify(messageExchangeSyncRepositoryMock).save(same(entity));
	}

	@Test
	void getAllByMunicipalityId() {
		final var municipalityId = "municipalityId";
		final var entity = new MessageExchangeSyncEntity();
		final var config = new MessageExchangeSync();

		when(messageExchangeSyncRepositoryMock.findByMunicipalityId(any())).thenReturn(List.of(entity));
		when(messageExchangeSyncMapperMock.toMessageExchangeSync(any())).thenReturn(config);

		var result = syncService.getAllByMunicipalityId(municipalityId);

		assertThat(result).hasSize(1).first().isSameAs(config);
		verify(messageExchangeSyncRepositoryMock).findByMunicipalityId(municipalityId);
		verify(messageExchangeSyncMapperMock).toMessageExchangeSync(same(entity));
	}

	@Test
	void replace() {
		final var municipalityId = "municipalityId";
		final var id = 123L;
		final var config = new MessageExchangeSync();
		final var existingEntity = new MessageExchangeSyncEntity().withId(id);
		final var replacementEntity = new MessageExchangeSyncEntity();

		when(messageExchangeSyncRepositoryMock.findByIdAndMunicipalityId(any(), any())).thenReturn(Optional.of(existingEntity));
		when(messageExchangeSyncMapperMock.toEntity(any(), any())).thenReturn(replacementEntity);

		syncService.replace(config, municipalityId, id);

		verify(messageExchangeSyncRepositoryMock).findByIdAndMunicipalityId(id, municipalityId);
		verify(messageExchangeSyncMapperMock).toEntity(same(config), eq(municipalityId));
		verify(messageExchangeSyncRepositoryMock).save(entityArgumentCaptor.capture());

		assertThat(entityArgumentCaptor.getValue()).isSameAs(replacementEntity);
		assertThat(entityArgumentCaptor.getValue().getId()).isEqualTo(id);
	}

	@Test
	void replaceNotFound() {
		final var municipalityId = "municipalityId";
		final var id = 123L;
		final var config = new MessageExchangeSync();

		when(messageExchangeSyncRepositoryMock.findByIdAndMunicipalityId(any(), any())).thenReturn(Optional.empty());

		assertThatThrownBy(() -> syncService.replace(config, municipalityId, id)).hasMessage("Not Found: No object found with municipalityId 'municipalityId' and id '123'");

		verify(messageExchangeSyncRepositoryMock).findByIdAndMunicipalityId(id, municipalityId);
		verifyNoInteractions(messageExchangeSyncMapperMock);
		verifyNoMoreInteractions(messageExchangeSyncRepositoryMock);
	}

	@Test
	void delete() {
		final var municipalityId = "municipalityId";
		final var id = 123L;
		final var entity = new MessageExchangeSyncEntity();

		when(messageExchangeSyncRepositoryMock.findByIdAndMunicipalityId(any(), any())).thenReturn(Optional.of(entity));

		syncService.delete(municipalityId, id);

		verify(messageExchangeSyncRepositoryMock).findByIdAndMunicipalityId(id, municipalityId);
		verify(messageExchangeSyncRepositoryMock).delete(same(entity));
	}

	@Test
	void deleteNotFound() {
		final var municipalityId = "municipalityId";
		final var id = 99L;

		when(messageExchangeSyncRepositoryMock.findByIdAndMunicipalityId(any(), any())).thenReturn(Optional.empty());

		assertThatThrownBy(() -> syncService.delete(municipalityId, id)).hasMessage("Not Found: No object found with municipalityId 'municipalityId' and id '99'");

		verify(messageExchangeSyncRepositoryMock).findByIdAndMunicipalityId(id, municipalityId);
		verifyNoInteractions(messageExchangeSyncMapperMock);
		verifyNoMoreInteractions(messageExchangeSyncRepositoryMock);
	}
}
