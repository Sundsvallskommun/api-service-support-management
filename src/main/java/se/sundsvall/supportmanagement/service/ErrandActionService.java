package se.sundsvall.supportmanagement.service;

import static org.zalando.problem.Status.BAD_REQUEST;
import static org.zalando.problem.Status.NOT_FOUND;
import static se.sundsvall.supportmanagement.service.mapper.ErrandActionMapper.toEntity;
import static se.sundsvall.supportmanagement.service.mapper.ErrandActionMapper.toMap;
import static se.sundsvall.supportmanagement.service.mapper.ErrandActionMapper.updateEntity;

import jakarta.transaction.Transactional;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.zalando.problem.Problem;
import se.sundsvall.supportmanagement.api.model.config.action.Config;
import se.sundsvall.supportmanagement.integration.db.ActionConfigRepository;
import se.sundsvall.supportmanagement.service.action.Action;
import se.sundsvall.supportmanagement.service.mapper.ErrandActionMapper;

@Service
@Transactional
public class ErrandActionService {

	private final ActionConfigRepository actionConfigRepository;
	private final Map<String, Action> actions;

	public ErrandActionService(ActionConfigRepository actionConfigRepository, List<Action> actions) {
		this.actionConfigRepository = actionConfigRepository;
		this.actions = new HashMap<>();
		actions.forEach(action -> {
			var result = this.actions.putIfAbsent(action.getName(), action);
			if (result != null) {
				throw new IllegalStateException("Duplicate action.name '%s'".formatted(action.getName()));
			}
		});
	}

	public List<Config> getActionConfigs(String municipalityId, String namespace) {
		return actionConfigRepository.findAllByNamespaceAndMunicipalityId(namespace, municipalityId).stream()
			.map(ErrandActionMapper::toActionConfig)
			.toList();
	}

	public String createActionConfig(String municipalityId, String namespace, Config config) {
		var action = actions.get(config.getName());

		if (action == null) {
			throw Problem.valueOf(BAD_REQUEST, "Could not find action with name '%s'".formatted(config.getName()));
		}

		action.validateConditions(municipalityId, namespace, toMap(config.getConditions()));
		action.validateParameters(municipalityId, namespace, toMap(config.getParameters()));

		return actionConfigRepository.save(toEntity(municipalityId, namespace, config)).getId();
	}

	public void updateActionConfig(String municipalityId, String namespace, String id, Config config) {
		var entity = actionConfigRepository.findByIdAndNamespaceAndMunicipalityId(id, namespace, municipalityId)
			.orElseThrow(() -> Problem.valueOf(NOT_FOUND, "Could not find action config with id '%s'".formatted(id)));

		var action = actions.get(config.getName());

		if (action == null) {
			throw Problem.valueOf(BAD_REQUEST, "Could not find action with name '%s'".formatted(config.getName()));
		}

		action.validateConditions(municipalityId, namespace, toMap(config.getConditions()));
		action.validateParameters(municipalityId, namespace, toMap(config.getParameters()));

		actionConfigRepository.save(updateEntity(entity, config));
	}

	public void deleteActionConfig(String municipalityId, String namespace, String id) {
		if (!actionConfigRepository.existsByIdAndNamespaceAndMunicipalityId(id, namespace, municipalityId)) {
			throw Problem.valueOf(NOT_FOUND, "Could not find action config with id '%s'".formatted(id));
		}

		actionConfigRepository.deleteByIdAndNamespaceAndMunicipalityId(id, namespace, municipalityId);
	}
}
