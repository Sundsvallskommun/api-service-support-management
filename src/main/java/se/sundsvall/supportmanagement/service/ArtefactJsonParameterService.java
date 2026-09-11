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
import se.sundsvall.dept44.support.Identifier;
import se.sundsvall.supportmanagement.api.model.errand.JsonParameter;
import se.sundsvall.supportmanagement.integration.db.model.ErrandEntity;
import se.sundsvall.supportmanagement.integration.db.model.JsonParameterEntity;
import se.sundsvall.supportmanagement.integration.db.model.JsonParameterLink;
import se.sundsvall.supportmanagement.integration.db.model.enums.ErrandField;
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
 * <b>What a caller may do with a key is decided as on the errand.</b> The parameter is one of the JSON parameters of
 * the errand, so a key the namespace keeps from a role is kept from it here too: left out of what is read, refused when
 * named, and refused when it would be changed - otherwise the artefact would be a way around what the errand upholds.
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

	private final AccessControlService accessControlService;
	private final EntityManager entityManager;

	ArtefactJsonParameterService(final AccessControlService accessControlService, final EntityManager entityManager) {
		this.accessControlService = accessControlService;
		this.entityManager = entityManager;
	}

	/**
	 * The parameters of the artefact whose keys the caller may see.
	 */
	public List<JsonParameter> readAll(final ErrandEntity errandEntity, final List<? extends JsonParameterLink> links) {
		final var readableKey = accessControlService.readableKeyPredicate(errandEntity.getNamespace(), errandEntity.getMunicipalityId(), Identifier.get(), errandEntity,
			ErrandField.JSON_PARAMETERS);

		return ofNullable(links).orElse(emptyList()).stream()
			.map(JsonParameterLink::getJsonParameterEntity)
			.filter(Objects::nonNull)
			.filter(entity -> readableKey.test(entity.getKey()))
			.sorted(Comparator.comparing(JsonParameterEntity::getKey))
			.map(ErrandMapper::toJsonParameter)
			.toList();
	}

	public JsonParameter read(final ErrandEntity errandEntity, final List<? extends JsonParameterLink> links, final String key) {
		accessControlService.verifyAccessibleKey(errandEntity.getNamespace(), errandEntity.getMunicipalityId(), errandEntity, ErrandField.JSON_PARAMETERS, key);
		return toJsonParameter(findParameterOrElseThrow(links, key));
	}

	/**
	 * Writes the parameter, creating it and its link when the artefact does not hold the key yet.
	 *
	 * @param jsonParameter the parameter to write, carrying its key.
	 * @param linkFactory   builds the link for the artefact from the parameter it is to point at.
	 */
	@Transactional
	public <L extends JsonParameterLink> UpsertResult upsert(final ErrandEntity errandEntity, final String ifMatch, final JsonParameter jsonParameter,
		final List<L> links, final Function<JsonParameterEntity, L> linkFactory, final JpaRepository<L, String> linkRepository) {

		final var key = jsonParameter.getKey();
		final var keyAccess = accessControlService.verifyJsonParameterAccess(errandEntity.getNamespace(), errandEntity.getMunicipalityId(), errandEntity, key, jsonParameter);
		final var writable = keyAccess.writableKey().test(key);

		return findParameter(links, key)
			.map(entity -> replace(entity, ifMatch, jsonParameter, writable))
			.orElseGet(() -> create(errandEntity, jsonParameter, links, linkFactory, linkRepository));
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
		accessControlService.verifyWritableKey(errandEntity.getNamespace(), errandEntity.getMunicipalityId(), errandEntity, ErrandField.JSON_PARAMETERS, key);

		final var entity = findParameterOrElseThrow(links, key);
		validateIfMatch(ifMatch, entity.getVersion());

		links.removeIf(link -> Objects.equals(ofNullable(link.getJsonParameterEntity()).map(JsonParameterEntity::getId).orElse(null), entity.getId()));
		entityManager.flush();

		removeParameters(errandEntity, Set.of(entity.getId()));
		entityManager.flush();
	}

	/**
	 * A caller who may not change the key only gets here by sending what is already stored, since anything else was
	 * refused. Writing it again would bump the version of a parameter they may not change, so it is answered as it stands.
	 */
	private UpsertResult replace(final JsonParameterEntity entity, final String ifMatch, final JsonParameter jsonParameter, final boolean writable) {
		validateIfMatch(ifMatch, entity.getVersion());

		if (!writable) {
			return new UpsertResult(toJsonParameter(entity), false);
		}

		entity.setSchemaId(jsonParameter.getSchemaId());
		entity.setValue(toJsonString(jsonParameter.getValue()));
		entityManager.flush();

		return new UpsertResult(toJsonParameter(entity), false);
	}

	private <L extends JsonParameterLink> UpsertResult create(final ErrandEntity errandEntity, final JsonParameter jsonParameter,
		final List<L> links, final Function<JsonParameterEntity, L> linkFactory, final JpaRepository<L, String> linkRepository) {

		verifyKeyIsFree(errandEntity, jsonParameter.getKey());

		final var entity = JsonParameterEntity.create()
			.withErrandEntity(errandEntity)
			.withKey(jsonParameter.getKey())
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
