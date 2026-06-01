package se.sundsvall.supportmanagement.service.action;

import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;
import se.sundsvall.dept44.problem.Problem;
import se.sundsvall.dept44.problem.ThrowableProblem;
import se.sundsvall.supportmanagement.api.model.config.action.Definition;
import se.sundsvall.supportmanagement.api.model.config.action.PossibleValue;
import se.sundsvall.supportmanagement.api.model.config.action.enums.OperationType;
import se.sundsvall.supportmanagement.integration.db.ErrandsRepository;
import se.sundsvall.supportmanagement.integration.db.model.ActionConfigEntity;
import se.sundsvall.supportmanagement.integration.db.model.ActionConfigParameterEntity;
import se.sundsvall.supportmanagement.integration.db.model.ErrandActionEntity;
import se.sundsvall.supportmanagement.integration.db.model.ErrandEntity;
import se.sundsvall.supportmanagement.integration.db.model.ErrandLabelEmbeddable;
import se.sundsvall.supportmanagement.service.MetadataService;

import static org.springframework.http.HttpStatus.UNPROCESSABLE_CONTENT;

@Component
public class AddLabelAction extends AbstractAction {

	private static final String LABEL = "label";
	private static final Set<OperationType> VALID_OPERATION_TYPES = Set.of(OperationType.CREATE, OperationType.UPDATE);

	private final ErrandsRepository errandsRepository;

	public AddLabelAction(final MetadataService metadataService, final ErrandsRepository errandsRepository) {
		super(metadataService);
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
		validateKeys(conditions, Set.of(STATUS, HAS_LABEL));
		validateStatuses(municipalityId, namespace, conditions);
		validateLabels(municipalityId, namespace, conditions);
	}

	@Override
	public void validateParameters(String municipalityId, String namespace, Map<String, List<String>> parameters) throws ThrowableProblem {
		Set<String> validKeys = Set.of(DURATION, LABEL);
		validateKeys(parameters, validKeys);

		if (!parameters.containsKey(LABEL) || parameters.get(LABEL).isEmpty()) {
			throw Problem.valueOf(UNPROCESSABLE_CONTENT, String.format("Key '%s' is mandatory and cannot be empty", LABEL));
		}

		validateDuration(parameters);

		var validLabelIds = getValidLabelIds(municipalityId, namespace);
		parameters.get(LABEL).forEach(value -> {
			if (!validLabelIds.contains(value)) {
				throw Problem.valueOf(UNPROCESSABLE_CONTENT, String.format("Label ID '%s' is not valid ", value));
			}
		});
	}

	@Override
	public boolean actionFulfilled(ErrandEntity errand, Map<String, List<String>> parameters) {
		return getErrandLabelIds(errand).containsAll(parameters.get(LABEL));
	}

	@Override
	public Optional<ErrandActionEntity> createAction(ErrandEntity errand, ActionConfigEntity actionConfigEntity) {
		var conditions = toConditionMap(actionConfigEntity);

		if (evaluateConditions(errand, conditions) && actionConfigEntity.getActive()) {
			var executeAfter = actionConfigEntity.getParameters().stream()
				.filter(parameterEntity -> parameterEntity.getKey().equals(DURATION))
				.findFirst()
				.map(ActionConfigParameterEntity::getValues)
				.map(List::getFirst)
				.map(Duration::parse)
				.map(duration -> OffsetDateTime.now().plus(duration))
				.orElse(OffsetDateTime.now());

			return buildErrandAction(errand, actionConfigEntity, executeAfter);
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

	@Override
	public boolean validForOperationType(OperationType operationType) {
		return VALID_OPERATION_TYPES.contains(operationType);
	}
}
