package se.sundsvall.supportmanagement.service;

import jakarta.persistence.EntityManager;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import se.sundsvall.dept44.problem.Problem;
import se.sundsvall.supportmanagement.api.model.errand.JsonParameter;
import se.sundsvall.supportmanagement.integration.db.model.ErrandEntity;
import se.sundsvall.supportmanagement.integration.db.model.JsonParameterEntity;
import se.sundsvall.supportmanagement.integration.db.model.JsonParameterLink;
import se.sundsvall.supportmanagement.service.ErrandJsonParameterService.UpsertResult;
import se.sundsvall.supportmanagement.service.mapper.ErrandMapper;

import static java.util.Collections.emptyList;
import static java.util.Optional.ofNullable;
import static org.springframework.http.HttpStatus.CONFLICT;
import static org.springframework.http.HttpStatus.NOT_FOUND;
import static se.sundsvall.supportmanagement.service.mapper.ErrandMapper.toJsonParameter;
import static se.sundsvall.supportmanagement.service.mapper.ErrandMapper.toJsonString;
import static se.sundsvall.supportmanagement.service.util.ArtefactJsonParameters.removeParameters;
import static se.sundsvall.supportmanagement.service.util.ETagUtil.validateIfMatch;

/**
 * The JSON parameters a handling artefact owns, written once for all five owners that can hold them.
 * <p>
 * The parameter row itself belongs to the errand and is stored in {@code json_parameter} like any other - what makes it
 * the content of an artefact is the link. That arrangement is why a parameter is both visible on the errand and removed
 * when its artefact is: the errand owns the row, the artefact owns the reason it exists.
 * <p>
 * <b>Keys are unique per errand</b>, which the database enforces - and compares without regard to case, which is why
 * they are compared that way here too. An artefact asking for a key another owner already holds is answered with a
 * conflict rather than by quietly taking it over. Where two artefacts of one errand need the same kind of content, the
 * caller qualifies the key.
 * <p>
 * <b>Removal always goes through the collection of the errand.</b> That collection cascades everything, so a row taken
 * out anywhere else is written back by the next flush - the attachment resurrection of DRAKEN-4801, one level down.
 */
@Service
public class ArtefactJsonParameterService {

	private static final String PARAMETER_NOT_FOUND = "A JSON parameter with key '%s' could not be found on this artefact";
	private static final String KEY_TAKEN = "A JSON parameter with key '%s' already exists in errand with id '%s' and belongs to something else";

	private final EntityManager entityManager;

	ArtefactJsonParameterService(final EntityManager entityManager) {
		this.entityManager = entityManager;
	}

	public List<JsonParameter> readAll(final List<? extends JsonParameterLink> links) {
		return ofNullable(links).orElse(emptyList()).stream()
			.map(JsonParameterLink::getJsonParameterEntity)
			.filter(Objects::nonNull)
			.sorted(Comparator.comparing(JsonParameterEntity::getKey))
			.map(ErrandMapper::toJsonParameter)
			.toList();
	}

	public JsonParameter read(final List<? extends JsonParameterLink> links, final String key) {
		return toJsonParameter(findParameterOrElseThrow(links, key));
	}

	/**
	 * Writes the parameter, creating it and its link when the artefact does not hold the key yet.
	 *
	 * @param linkFactory builds the link for the artefact from the parameter it is to point at.
	 */
	@Transactional
	public <L extends JsonParameterLink> UpsertResult upsert(final ErrandEntity errandEntity, final String key, final String ifMatch, final JsonParameter jsonParameter,
		final List<L> links, final Function<JsonParameterEntity, L> linkFactory, final JpaRepository<L, String> linkRepository) {

		return findParameter(links, key)
			.map(entity -> replace(entity, ifMatch, jsonParameter))
			.orElseGet(() -> create(errandEntity, key, jsonParameter, links, linkFactory, linkRepository));
	}

	/**
	 * Removes the parameter the artefact owns. The artefact itself is untouched.
	 * <p>
	 * The link goes first and the parameter second, and the order is not a matter of taste. Orphan removal on the
	 * artefact is what deletes the link row; the database also cascades it away when the parameter goes. Removing the
	 * parameter first would leave orphan removal deleting a row that is no longer there, which Hibernate reports as a
	 * stale write on the next flush and the caller sees as a 412 it can do nothing about.
	 */
	@Transactional
	public void delete(final ErrandEntity errandEntity, final List<? extends JsonParameterLink> links, final String key, final String ifMatch) {
		final var entity = findParameterOrElseThrow(links, key);
		validateIfMatch(ifMatch, entity.getVersion());

		links.removeIf(link -> Objects.equals(ofNullable(link.getJsonParameterEntity()).map(JsonParameterEntity::getId).orElse(null), entity.getId()));
		entityManager.flush();

		removeParameters(errandEntity, Set.of(entity.getId()));
		entityManager.flush();
	}

	private UpsertResult replace(final JsonParameterEntity entity, final String ifMatch, final JsonParameter jsonParameter) {
		validateIfMatch(ifMatch, entity.getVersion());

		entity.setSchemaId(jsonParameter.getSchemaId());
		entity.setValue(toJsonString(jsonParameter.getValue()));
		entityManager.flush();

		return new UpsertResult(toJsonParameter(entity), false);
	}

	private <L extends JsonParameterLink> UpsertResult create(final ErrandEntity errandEntity, final String key, final JsonParameter jsonParameter,
		final List<L> links, final Function<JsonParameterEntity, L> linkFactory, final JpaRepository<L, String> linkRepository) {

		verifyKeyIsFree(errandEntity, key);

		final var entity = JsonParameterEntity.create()
			.withErrandEntity(errandEntity)
			.withKey(key)
			.withSchemaId(jsonParameter.getSchemaId())
			.withValue(toJsonString(jsonParameter.getValue()));

		if (errandEntity.getJsonParameters() == null) {
			errandEntity.setJsonParameters(new ArrayList<>());
		}
		errandEntity.getJsonParameters().add(entity);

		// Flushed before the link is built, so that the parameter has the id the link is to point at.
		entityManager.flush();
		links.add(linkRepository.save(linkFactory.apply(entity)));

		return new UpsertResult(toJsonParameter(entity), true);
	}

	/**
	 * The key is unique per errand in the database. Checking it here turns what would surface as a constraint violation
	 * deep in the flush into the conflict it is, and says whose key it is.
	 */
	private void verifyKeyIsFree(final ErrandEntity errandEntity, final String key) {
		final var taken = ofNullable(errandEntity.getJsonParameters()).orElse(emptyList()).stream()
			.anyMatch(parameter -> key.equalsIgnoreCase(parameter.getKey()));

		if (taken) {
			throw Problem.valueOf(CONFLICT, KEY_TAKEN.formatted(key, errandEntity.getId()));
		}
	}

	private JsonParameterEntity findParameterOrElseThrow(final List<? extends JsonParameterLink> links, final String key) {
		return findParameter(links, key)
			.orElseThrow(() -> Problem.valueOf(NOT_FOUND, PARAMETER_NOT_FOUND.formatted(key)));
	}

	private Optional<JsonParameterEntity> findParameter(final List<? extends JsonParameterLink> links, final String key) {
		return ofNullable(links).orElse(emptyList()).stream()
			.map(JsonParameterLink::getJsonParameterEntity)
			.filter(Objects::nonNull)
			.filter(entity -> key.equalsIgnoreCase(entity.getKey()))
			.findFirst();
	}
}
