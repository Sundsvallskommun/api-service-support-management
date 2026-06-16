package se.sundsvall.supportmanagement.service.action;

import java.time.Clock;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.springframework.stereotype.Component;
import se.sundsvall.dept44.problem.Problem;
import se.sundsvall.dept44.problem.ThrowableProblem;
import se.sundsvall.supportmanagement.api.model.config.action.Definition;
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

	public AddLabelAction(final MetadataService metadataService, final ErrandsRepository errandsRepository, final Clock clock) {
		super(metadataService, clock);
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
		return createActionWithDuration(errand, actionConfigEntity);
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
