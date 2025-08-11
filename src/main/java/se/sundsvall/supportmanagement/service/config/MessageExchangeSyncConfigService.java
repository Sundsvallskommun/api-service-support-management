package se.sundsvall.supportmanagement.service.config;

import static org.zalando.problem.Status.NOT_FOUND;

import java.util.List;
import org.springframework.stereotype.Service;
import org.zalando.problem.Problem;
import se.sundsvall.supportmanagement.api.model.config.MessageExchangeSync;
import se.sundsvall.supportmanagement.integration.db.MessageExchangeSyncRepository;
import se.sundsvall.supportmanagement.service.mapper.MessageExchangeSyncMapper;

@Service
public class MessageExchangeSyncConfigService {

	private static final String ENTITY_NOT_FOUND = "No object found with municipalityId '%s' and id '%s'";

	private final MessageExchangeSyncRepository repository;
	private final MessageExchangeSyncMapper mapper;

	public MessageExchangeSyncConfigService(final MessageExchangeSyncRepository repository, final MessageExchangeSyncMapper mapper) {
		this.repository = repository;
		this.mapper = mapper;
	}

	public Long create(MessageExchangeSync config, String municipalityId) {
		return repository.save(mapper.toEntity(config, municipalityId)).getId();
	}

	public List<MessageExchangeSync> getAllByMunicipalityId(String municipalityId) {
		return repository.findByMunicipalityId(municipalityId).stream()
			.map(mapper::toMessageExchangeSync)
			.toList();
	}

	public void replace(MessageExchangeSync config, String municipalityId, Long id) {
		var entity = repository.findByIdAndMunicipalityId(id, municipalityId)
			.orElseThrow(() -> Problem.valueOf(NOT_FOUND, String.format(ENTITY_NOT_FOUND, municipalityId, id)));

		var replacement = mapper.toEntity(config, municipalityId)
			.withId(entity.getId());

		repository.save(replacement);
	}

	public void delete(String municipalityId, Long id) {
		var entity = repository.findByIdAndMunicipalityId(id, municipalityId)
			.orElseThrow(() -> Problem.valueOf(NOT_FOUND, String.format(ENTITY_NOT_FOUND, municipalityId, id)));

		repository.delete(entity);
	}
}
