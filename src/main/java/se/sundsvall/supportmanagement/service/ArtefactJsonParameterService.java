package se.sundsvall.supportmanagement.service;

import jakarta.persistence.EntityManager;
import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import se.sundsvall.dept44.problem.Problem;
import se.sundsvall.supportmanagement.api.model.errand.JsonParameter;
import se.sundsvall.supportmanagement.integration.db.model.AbstractArtefactJsonParameterEntity;
import se.sundsvall.supportmanagement.service.ErrandJsonParameterService.UpsertResult;

import static java.util.Collections.emptyList;
import static java.util.Optional.ofNullable;
import static org.springframework.http.HttpStatus.NOT_FOUND;
import static se.sundsvall.supportmanagement.service.mapper.ArtefactJsonParameterMapper.toJsonParameter;
import static se.sundsvall.supportmanagement.service.mapper.ArtefactJsonParameterMapper.toJsonParameters;
import static se.sundsvall.supportmanagement.service.mapper.ErrandMapper.toJsonString;
import static se.sundsvall.supportmanagement.service.util.ETagUtil.validateIfMatch;

/**
 * The JSON parameters of the handling artefacts, written once for all five owners that can hold them: statement,
 * investigation, investigation section, decision and measure.
 * <p>
 * The owner hands over its own collection, and the collection is what holds the parameters: one added to it is
 * persisted with the owner, one taken out of it is removed. Whether the caller may reach the artefact at all is settled
 * by the calling service before it gets here, by the protected resource of the artefact - nothing here asks about the
 * errand, since the parameters are not the errand's.
 * <p>
 * Keys are unique per owner, which the database enforces without regard to case, and they are looked up the same way.
 * Every write is flushed before it is answered, so that the version the response carries - which its ETag is taken
 * from - is the one just written.
 */
@Service
public class ArtefactJsonParameterService {

	private static final String PARAMETER_NOT_FOUND = "A JSON parameter with key '%s' could not be found on this artefact";

	private final EntityManager entityManager;

	ArtefactJsonParameterService(final EntityManager entityManager) {
		this.entityManager = entityManager;
	}

	public List<JsonParameter> readAll(final List<? extends AbstractArtefactJsonParameterEntity<?>> parameters) {
		return toJsonParameters(parameters);
	}

	public JsonParameter read(final List<? extends AbstractArtefactJsonParameterEntity<?>> parameters, final String key) {
		return toJsonParameter(findParameterOrElseThrow(parameters, key));
	}

	/**
	 * Writes the parameter, creating it when the owner does not hold the key yet.
	 *
	 * @param  parameters    the collection of the owner, which a new parameter is added to.
	 * @param  factory       creates a parameter already pointing at the owner.
	 * @param  ifMatch       the ETag the caller holds, held against a parameter already there.
	 * @param  jsonParameter the parameter to write, carrying its key.
	 * @return               the parameter as written, and whether it was created.
	 */
	@Transactional
	public <P extends AbstractArtefactJsonParameterEntity<P>> UpsertResult upsert(final List<P> parameters, final Supplier<P> factory, final String ifMatch,
		final JsonParameter jsonParameter) {

		final var existing = findParameter(parameters, jsonParameter.getKey());
		existing.ifPresent(entity -> validateIfMatch(ifMatch, entity.getVersion()));

		final var entity = existing.orElseGet(() -> {
			final var created = factory.get().withKey(jsonParameter.getKey());
			parameters.add(created);
			return created;
		});
		entity.setSchemaId(jsonParameter.getSchemaId());
		entity.setValue(toJsonString(jsonParameter.getValue()));
		entityManager.flush();

		return new UpsertResult(toJsonParameter(entity), existing.isEmpty());
	}

	/**
	 * Removes the parameter from the owner, which is left as it is otherwise.
	 */
	@Transactional
	public void delete(final List<? extends AbstractArtefactJsonParameterEntity<?>> parameters, final String key, final String ifMatch) {
		final var entity = findParameterOrElseThrow(parameters, key);
		validateIfMatch(ifMatch, entity.getVersion());

		parameters.remove(entity);
		entityManager.flush();
	}

	private static <P extends AbstractArtefactJsonParameterEntity<?>> P findParameterOrElseThrow(final List<P> parameters, final String key) {
		return findParameter(parameters, key)
			.orElseThrow(() -> Problem.valueOf(NOT_FOUND, PARAMETER_NOT_FOUND.formatted(key)));
	}

	private static <P extends AbstractArtefactJsonParameterEntity<?>> Optional<P> findParameter(final List<P> parameters, final String key) {
		return ofNullable(parameters).orElse(emptyList()).stream()
			.filter(entity -> key.equalsIgnoreCase(entity.getKey()))
			.findFirst();
	}
}
