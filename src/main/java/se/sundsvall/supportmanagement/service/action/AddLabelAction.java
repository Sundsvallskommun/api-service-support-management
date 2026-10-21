package se.sundsvall.supportmanagement.service.action;

import java.time.Clock;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;
import org.springframework.stereotype.Component;
import se.sundsvall.dept44.problem.Problem;
import se.sundsvall.dept44.problem.ThrowableProblem;
import se.sundsvall.supportmanagement.api.model.config.action.Definition;
import se.sundsvall.supportmanagement.integration.db.ErrandsRepository;
import se.sundsvall.supportmanagement.integration.db.model.ActionConfigEntity;
import se.sundsvall.supportmanagement.integration.db.model.ActionConfigParameterEntity;
import se.sundsvall.supportmanagement.integration.db.model.ErrandActionEntity;
import se.sundsvall.supportmanagement.integration.db.model.ErrandEntity;
import se.sundsvall.supportmanagement.integration.db.model.ErrandLabelEmbeddable;
import se.sundsvall.supportmanagement.integration.db.model.enums.OperationType;
import se.sundsvall.supportmanagement.service.ErrandLabelService;
import se.sundsvall.supportmanagement.service.MetadataService;
import se.sundsvall.supportmanagement.service.ProcessKeyGuard;

import static org.springframework.http.HttpStatus.UNPROCESSABLE_CONTENT;

@Component
public class AddLabelAction extends AbstractAction {

	private static final String LABEL = "label";
	private static final Set<OperationType> VALID_OPERATION_TYPES = Set.of(OperationType.CREATE, OperationType.UPDATE);

	private static final String LEFT_OFF = """
		The labels a scheduled action was to add were therefore left off the errand. Take the label off the action, or give \
		the action a condition that keeps it away from the errands it must not relabel.""";

	private final ErrandsRepository errandsRepository;
	private final ErrandLabelService errandLabelService;
	private final ProcessKeyGuard processKeyGuard;

	public AddLabelAction(final MetadataService metadataService, final ErrandsRepository errandsRepository, final ErrandLabelService errandLabelService, final ProcessKeyGuard processKeyGuard,
		final Clock clock) {
		super(metadataService, clock);
		this.errandsRepository = errandsRepository;
		this.errandLabelService = errandLabelService;
		this.processKeyGuard = processKeyGuard;
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

	/**
	 * Puts the labels of the action on the errand, unless doing so would leave it naming a process it does not belong to.
	 * <p>
	 * The labels the errand would wear, those it already wears included, are expanded to their ancestors before the guard
	 * is asked, so a process key carried by an ancestor is held to as well. The ancestors the errand lacks are put on with
	 * the labels.
	 * <p>
	 * A refused change leaves the labels off and answers false, and the guard writes what happened on the errand.
	 * Otherwise answers whether any label was added. The access labels of the errand are left as they are.
	 */
	@Override
	public boolean executeAction(ErrandEntity errand, ActionConfigEntity actionConfigEntity) {
		var newLabels = actionConfigEntity.getParameters().stream()
			.filter(parameter -> parameter.getKey().equals(LABEL))
			.findFirst()
			.map(ActionConfigParameterEntity::getValues)
			.stream()
			.flatMap(List::stream)
			.map(labelId -> ErrandLabelEmbeddable.create().withMetadataLabelId(labelId))
			.filter(label -> !errand.getLabels().contains(label))
			.distinct()
			.toList();

		var current = List.copyOf(errand.getLabels());
		var labelsAfter = errandLabelService.withAncestors(errand.getNamespace(), errand.getMunicipalityId(), Stream.concat(current.stream(), newLabels.stream()).toList());

		if (processKeyGuard.refusesLabelChange(errand.getId(), current, labelsAfter, LEFT_OFF)) {
			return false;
		}

		var added = labelsAfter.stream()
			.filter(label -> !current.contains(label))
			.toList();

		errand.getLabels().addAll(added);

		errandsRepository.save(errand);

		return !added.isEmpty();
	}

	@Override
	public boolean validForOperationType(OperationType operationType) {
		return VALID_OPERATION_TYPES.contains(operationType);
	}

	@Override
	public Set<OperationType> getValidOperationTypes() {
		return VALID_OPERATION_TYPES;
	}
}
