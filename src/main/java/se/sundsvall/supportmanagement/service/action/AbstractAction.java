package se.sundsvall.supportmanagement.service.action;

import java.time.Clock;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.data.domain.Sort;
import se.sundsvall.dept44.problem.Problem;
import se.sundsvall.dept44.problem.ThrowableProblem;
import se.sundsvall.supportmanagement.api.model.config.action.Definition;
import se.sundsvall.supportmanagement.api.model.config.action.PossibleValue;
import se.sundsvall.supportmanagement.api.model.metadata.Label;
import se.sundsvall.supportmanagement.api.model.metadata.Status;
import se.sundsvall.supportmanagement.integration.db.model.ActionConfigConditionEntity;
import se.sundsvall.supportmanagement.integration.db.model.ActionConfigEntity;
import se.sundsvall.supportmanagement.integration.db.model.ActionConfigParameterEntity;
import se.sundsvall.supportmanagement.integration.db.model.ErrandActionEntity;
import se.sundsvall.supportmanagement.integration.db.model.ErrandEntity;
import se.sundsvall.supportmanagement.integration.db.model.ErrandLabelEmbeddable;
import se.sundsvall.supportmanagement.service.MetadataService;

import static org.springframework.http.HttpStatus.UNPROCESSABLE_CONTENT;

public abstract class AbstractAction implements Action {

	protected static final String STATUS = "status";
	protected static final String HAS_LABEL = "hasLabel";
	protected static final String DURATION = "duration";
	protected final MetadataService metadataService;
	protected final Clock clock;

	protected AbstractAction(final MetadataService metadataService, final Clock clock) {
		this.metadataService = metadataService;
		this.clock = clock;
	}

	@Override
	public List<Definition> getConditionDefinitions(String municipalityId, String namespace) {
		return List.of(
			Definition.create()
				.withKey(STATUS)
				.withMandatory(false)
				.withDescription("Errand status. If null action will execute for all statuses")
				.withPossibleValues(metadataService.findStatuses(namespace, municipalityId, Sort.unsorted()).stream()
					.map(status -> PossibleValue.create()
						.withValue(status.getName())
						.withDisplayName(status.getName()))
					.toList()),
			Definition.create()
				.withKey(HAS_LABEL)
				.withMandatory(false)
				.withDescription("Errand must have these labels for action to be added")
				.withPossibleValues(getLabelPossibleValues(municipalityId, namespace)));
	}

	@Override
	public void validateConditions(String municipalityId, String namespace, Map<String, List<String>> conditions) throws ThrowableProblem {
		validateKeys(conditions, Set.of(STATUS, HAS_LABEL));
		validateStatuses(municipalityId, namespace, conditions);
		validateLabels(municipalityId, namespace, conditions);
	}

	protected Optional<ErrandActionEntity> createActionWithDuration(ErrandEntity errand, ActionConfigEntity actionConfigEntity) {
		var conditions = toConditionMap(actionConfigEntity);

		if (evaluateConditions(errand, conditions) && actionConfigEntity.getActive()) {
			var executeAfter = actionConfigEntity.getParameters().stream()
				.filter(parameterEntity -> parameterEntity.getKey().equals(DURATION))
				.findFirst()
				.map(ActionConfigParameterEntity::getValues)
				.map(List::getFirst)
				.map(Duration::parse)
				.map(duration -> OffsetDateTime.now(clock).plus(duration))
				.orElse(OffsetDateTime.now(clock));

			return buildErrandAction(errand, actionConfigEntity, executeAfter);
		} else {
			return Optional.empty();
		}
	}

	protected List<PossibleValue> getLabelPossibleValues(String municipalityId, String namespace) {
		return metadataService.findLabels(namespace, municipalityId).flatten().stream()
			.map(label -> PossibleValue.create()
				.withValue(label.getId())
				.withDisplayName(label.getDisplayName()))
			.toList();
	}

	protected List<String> getValidLabelIds(String municipalityId, String namespace) {
		return metadataService.findLabels(namespace, municipalityId).flatten().stream()
			.map(Label::getId)
			.toList();
	}

	protected Set<String> getErrandLabelIds(ErrandEntity errand) {
		return errand.getLabels().stream()
			.map(ErrandLabelEmbeddable::getMetadataLabelId)
			.collect(Collectors.toSet());
	}

	protected void validateKeys(Map<String, List<String>> map, Set<String> validKeys) {
		map.keySet().forEach(key -> {
			if (!validKeys.contains(key)) {
				throw Problem.valueOf(UNPROCESSABLE_CONTENT, String.format("Key '%s' is not valid", key));
			}
		});
	}

	protected void validateStatuses(String municipalityId, String namespace, Map<String, List<String>> conditions) {
		if (conditions.containsKey(STATUS)) {
			var validStatuses = metadataService.findStatuses(namespace, municipalityId, Sort.unsorted()).stream().map(Status::getName).toList();
			conditions.get(STATUS).forEach(value -> {
				if (!validStatuses.contains(value)) {
					throw Problem.valueOf(UNPROCESSABLE_CONTENT, String.format("Status '%s' is not valid for this namespace", value));
				}
			});
		}
	}

	protected void validateLabels(String municipalityId, String namespace, Map<String, List<String>> conditions) {
		if (conditions.containsKey(HAS_LABEL)) {
			var validLabelIds = getValidLabelIds(municipalityId, namespace);
			conditions.get(HAS_LABEL).forEach(value -> {
				if (!validLabelIds.contains(value)) {
					throw Problem.valueOf(UNPROCESSABLE_CONTENT, String.format("Label ID '%s' is not valid", value));
				}
			});
		}
	}

	protected void validateDuration(Map<String, List<String>> map) {
		if (map.containsKey(DURATION)) {
			if (map.get(DURATION).size() > 1) {
				throw Problem.valueOf(UNPROCESSABLE_CONTENT, String.format("Cannot handle multiple values of key '%s'", DURATION));
			}
			try {
				Duration.parse(map.get(DURATION).getFirst());
			} catch (DateTimeParseException _) {
				throw Problem.valueOf(UNPROCESSABLE_CONTENT, String.format("Could not parse duration '%s'", map.get(DURATION).getFirst()));
			}
		}
	}

	protected boolean evaluateConditions(ErrandEntity errand, Map<String, List<String>> conditions) {
		boolean fulfillsConditions = true;

		if (conditions.containsKey(STATUS)) {
			fulfillsConditions &= conditions.get(STATUS).contains(errand.getStatus());
		}

		if (conditions.containsKey(HAS_LABEL)) {
			fulfillsConditions &= getErrandLabelIds(errand).containsAll(conditions.get(HAS_LABEL));
		}

		return fulfillsConditions;
	}

	protected Map<String, List<String>> toConditionMap(ActionConfigEntity actionConfigEntity) {
		return actionConfigEntity.getConditions().stream()
			.collect(Collectors.toMap(ActionConfigConditionEntity::getKey, ActionConfigConditionEntity::getValues));
	}

	protected Optional<ErrandActionEntity> buildErrandAction(ErrandEntity errand, ActionConfigEntity actionConfigEntity, OffsetDateTime executeAfter) {
		return Optional.of(ErrandActionEntity.create()
			.withActionConfigEntity(actionConfigEntity)
			.withErrandEntity(errand)
			.withExecuteAfter(executeAfter));
	}
}
