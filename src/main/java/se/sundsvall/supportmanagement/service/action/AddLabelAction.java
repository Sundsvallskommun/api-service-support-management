package se.sundsvall.supportmanagement.service.action;

import static org.zalando.problem.Status.UNPROCESSABLE_ENTITY;

import java.time.Duration;
import java.time.OffsetDateTime;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;
import org.zalando.problem.Problem;
import org.zalando.problem.ThrowableProblem;
import se.sundsvall.supportmanagement.api.model.config.action.Definition;
import se.sundsvall.supportmanagement.api.model.config.action.PossibleValue;
import se.sundsvall.supportmanagement.api.model.metadata.Label;
import se.sundsvall.supportmanagement.api.model.metadata.Status;
import se.sundsvall.supportmanagement.integration.db.ErrandsRepository;
import se.sundsvall.supportmanagement.integration.db.model.ActionConfigConditionEntity;
import se.sundsvall.supportmanagement.integration.db.model.ActionConfigEntity;
import se.sundsvall.supportmanagement.integration.db.model.ActionConfigParameterEntity;
import se.sundsvall.supportmanagement.integration.db.model.ErrandActionEntity;
import se.sundsvall.supportmanagement.integration.db.model.ErrandEntity;
import se.sundsvall.supportmanagement.integration.db.model.ErrandLabelEmbeddable;
import se.sundsvall.supportmanagement.service.MetadataService;

@Component
public class AddLabelAction implements Action {

	private static final String STATUS = "status";
	private static final String HAS_LABEL = "hasLabel";
	private static final String DURATION = "duration";
	private static final String LABEL = "label";

	private final MetadataService metadataService;
	private final ErrandsRepository errandsRepository;

	public AddLabelAction(final MetadataService metadataService, final ErrandsRepository errandsRepository) {
		this.metadataService = metadataService;
		this.errandsRepository = errandsRepository;
	}

	@Override
	public String getName() {
		return "ADD_LABEL";
	}

	@Override
	public String getDescription() {
		return "Adds a new label within a configurable amount of time if conditions are met";
	}

	@Override
	public List<Definition> getConditionDefinitions(String municipalityId, String namespace) {
		return List.of(
			Definition.create()
				.withKey(STATUS)
				.withMandatory(false)
				.withDescription("Errand status. If null action will execute for all statuses")
				.withPossibleValues(metadataService.findStatuses(namespace, municipalityId).stream()
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
	public List<Definition> getParameterDefinitions(String municipalityId, String namespace) {
		return List.of(
			Definition.create()
				.withKey(LABEL)
				.withMandatory(true)
				.withDescription("Label that will be added to Errand")
				.withPossibleValues(getLabelPossibleValues(municipalityId, namespace)),
			Definition.create()
				.withKey(DURATION)
				.withMandatory(false)
				.withDescription("Duration before label is added. Format follows java Duration ISO-8601 (PnDTnHnMn.nS). If null will be added directly"));
	}

	@Override
	public void validateConditions(String municipalityId, String namespace, Map<String, List<String>> conditions) throws ThrowableProblem {
		Set<String> validKeys = Set.of(STATUS, HAS_LABEL);
		conditions.keySet()
			.forEach(key -> {
				if (!validKeys.contains(key)) {
					throw Problem.valueOf(UNPROCESSABLE_ENTITY, String.format("Key '%s' is not valid", key));
				}
			});

		if (conditions.containsKey(STATUS)) {
			var validStatuses = metadataService.findStatuses(namespace, municipalityId).stream().map(Status::getName).toList();
			conditions.get(STATUS).forEach(value -> {
				if (!validStatuses.contains(value)) {
					throw Problem.valueOf(UNPROCESSABLE_ENTITY, String.format("Status '%s' is not valid for this namespace", value));
				}
			});
		}

		if (conditions.containsKey(HAS_LABEL)) {
			var validLabelIds = getValidLabelIds(municipalityId, namespace);
			conditions.get(HAS_LABEL).forEach(value -> {
				if (!validLabelIds.contains(value)) {
					throw Problem.valueOf(UNPROCESSABLE_ENTITY, String.format("Label ID '%s' is not valid", value));
				}
			});
		}
	}

	@Override
	public void validateParameters(String municipalityId, String namespace, Map<String, List<String>> parameters) throws ThrowableProblem {
		Set<String> validKeys = Set.of(DURATION, LABEL);
		parameters.keySet()
			.forEach(key -> {
				if (!validKeys.contains(key)) {
					throw Problem.valueOf(UNPROCESSABLE_ENTITY, String.format("Key '%s' is not valid", key));
				}
			});

		if (!parameters.containsKey(LABEL) || parameters.get(LABEL).isEmpty()) {
			throw Problem.valueOf(UNPROCESSABLE_ENTITY, String.format("Key '%s' is mandatory and cannot be empty", LABEL));
		}

		if (parameters.containsKey(DURATION)) {
			if (parameters.get(DURATION).size() > 1) {
				throw Problem.valueOf(UNPROCESSABLE_ENTITY, String.format("Cannot handle multiple values of key '%s' ", DURATION));
			}
			try {
				Duration.parse(parameters.get(DURATION).getFirst());
			} catch (DateTimeParseException e) {
				throw Problem.valueOf(UNPROCESSABLE_ENTITY, String.format("Could not parse duration '%s'", parameters.get(DURATION).getFirst()));
			}
		}

		var validLabelIds = getValidLabelIds(municipalityId, namespace);
		parameters.get(LABEL).forEach(value -> {
			if (!validLabelIds.contains(value)) {
				throw Problem.valueOf(UNPROCESSABLE_ENTITY, String.format("Label ID '%s' is not valid ", value));
			}
		});
	}

	@Override
	public boolean actionFulfilled(ErrandEntity errand, Map<String, List<String>> parameters) {
		return getErrandLabelIds(errand).containsAll(parameters.get(LABEL));
	}

	@Override
	public Optional<ErrandActionEntity> createAction(ErrandEntity errand, ActionConfigEntity actionConfigEntity) {

		var conditions = actionConfigEntity.getConditions().stream()
			.collect(Collectors.toMap(ActionConfigConditionEntity::getKey, ActionConfigConditionEntity::getValues));

		boolean fulfillsConditions = true;

		if (conditions.containsKey(STATUS)) {
			fulfillsConditions &= conditions.get(STATUS).contains(errand.getStatus());
		}

		if (conditions.containsKey(HAS_LABEL)) {
			fulfillsConditions &= getErrandLabelIds(errand).containsAll(conditions.get(HAS_LABEL));
		}

		if (fulfillsConditions && actionConfigEntity.getActive()) {
			var executeAfter = actionConfigEntity.getParameters().stream()
				.filter(parameterEntity -> parameterEntity.getKey().equals(DURATION))
				.findFirst()
				.map(ActionConfigParameterEntity::getValues)
				.map(List::getFirst)
				.map(Duration::parse)
				.map(duration -> OffsetDateTime.now().plus(duration))
				.orElse(OffsetDateTime.now());

			return Optional.of(ErrandActionEntity.create()
				.withActionConfigEntity(actionConfigEntity)
				.withErrandEntity(errand)
				.withExecuteAfter(executeAfter));
		} else {
			return Optional.empty();
		}
	}

	@Override
	public void executeAction(ErrandEntity errand, ActionConfigEntity actionConfigEntity) {
		var newLabels = actionConfigEntity.getParameters().stream()
			.filter(parameter -> parameter.getKey().equals(LABEL))
			.findFirst()
			.map(ActionConfigParameterEntity::getValues)
			.stream()
			.flatMap(List::stream)
			.map(labelId -> ErrandLabelEmbeddable.create().withMetadataLabelId(labelId))
			.filter(label -> !errand.getLabels().contains(label))
			.toList();

		errand.getLabels().addAll(newLabels);

		errandsRepository.save(errand);
	}

	private List<PossibleValue> getLabelPossibleValues(String municipalityId, String namespace) {
		return metadataService.findLabels(namespace, municipalityId).flatten().stream()
			.map(label -> PossibleValue.create()
				.withValue(label.getId())
				.withDisplayName(label.getDisplayName()))
			.toList();
	}

	private List<String> getValidLabelIds(String municipalityId, String namespace) {
		return metadataService.findLabels(namespace, municipalityId).flatten().stream()
			.map(Label::getId)
			.toList();
	}

	private Set<String> getErrandLabelIds(ErrandEntity errand) {
		return errand.getLabels().stream()
			.map(ErrandLabelEmbeddable::getMetadataLabelId)
			.collect(Collectors.toSet());
	}
}
