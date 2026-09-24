package se.sundsvall.supportmanagement.service.search;

import com.google.gson.JsonObject;
import jakarta.persistence.EntityManager;
import java.util.ArrayList;
import org.hibernate.search.backend.elasticsearch.ElasticsearchExtension;
import org.hibernate.search.engine.search.query.SearchResult;
import org.hibernate.search.engine.search.sort.dsl.SearchSortFactory;
import org.hibernate.search.engine.search.sort.dsl.SortFinalStep;
import org.hibernate.search.engine.search.sort.dsl.SortOrder;
import org.hibernate.search.mapper.orm.Search;
import org.hibernate.search.util.common.SearchException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import se.sundsvall.dept44.problem.Problem;
import se.sundsvall.dept44.support.Identifier;
import se.sundsvall.supportmanagement.api.model.errand.Errand;
import se.sundsvall.supportmanagement.config.SearchProperties;
import se.sundsvall.supportmanagement.integration.db.model.ErrandEntity;
import se.sundsvall.supportmanagement.integration.db.search.ErrandIndex;
import se.sundsvall.supportmanagement.service.AccessControlService;
import se.sundsvall.supportmanagement.service.search.index.ErrandIndexModel;
import se.sundsvall.supportmanagement.service.search.index.SearchAvailability;

import static generated.se.sundsvall.accessmapper.Access.AccessLevelEnum.R;
import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static se.sundsvall.supportmanagement.service.mapper.ErrandMapper.toErrandsWithAccessControl;

/**
 * Searches errands in the OpenSearch index Hibernate Search keeps, and answers with the same errands, mapped with the
 * same field access, as the database backed listing does.
 */
@Service
public class ErrandSearchService {

	private static final String SCORE = "_score";

	static final String UNSUPPORTED_SORT = "Sorting on '%s' is not supported by search. Sortable properties are: %s";
	static final String BEYOND_RESULT_WINDOW = "Page %d of size %d reaches beyond the %d results a search can page through. Narrow the search instead";

	private final EntityManager entityManager;
	private final AccessControlService accessControlService;
	private final ErrandSearchAccess searchAccess;
	private final ErrandSearchPredicates predicates;
	private final SearchAvailability availability;
	private final SearchProperties properties;

	public ErrandSearchService(final EntityManager entityManager, final AccessControlService accessControlService, final ErrandSearchAccess searchAccess,
		final ErrandSearchPredicates predicates, final SearchAvailability availability, final SearchProperties properties) {
		this.entityManager = entityManager;
		this.accessControlService = accessControlService;
		this.searchAccess = searchAccess;
		this.predicates = predicates;
		this.availability = availability;
		this.properties = properties;
	}

	/**
	 * Searches the errands of a namespace the requesting user reaches.
	 *
	 * @param  namespace                                      namespace
	 * @param  municipalityId                                 municipality id
	 * @param  query                                          a Lucene query string, or blank for every errand
	 * @param  pageable                                       page, size and sort. Without a sort the best matches come
	 *                                                        first, newest first among equals
	 * @return                                                the page of matching errands the user reaches at full read,
	 *                                                        with what the requesting user may
	 *                                                        see of each
	 * @throws org.springframework.web.ErrorResponseException 403 when the query names a resource the user may not read,
	 *                                                        see {@link ErrandSearchAccess}
	 */
	@Transactional(readOnly = true)
	public Page<Errand> search(final String namespace, final String municipalityId, final String query, final Pageable pageable) {
		availability.verifyEnabled();
		verifyWithinResultWindow(pageable);
		verifySortable(pageable.getSort());

		final var user = Identifier.get();
		final var grant = accessControlService.namespaceGrant(namespace, municipalityId, user, R);
		final var plan = searchAccess.plan(query, pageable.getSort(), grant);

		final SearchResult<ErrandEntity> result;
		try {
			result = Search.session(entityManager).search(ErrandEntity.class)
				.where(f -> f.bool()
					.filter(predicates.tenant(f, namespace, municipalityId))
					.must(predicates.clauses(f, plan.clauses(), query, namespace, municipalityId)))
				.sort(f -> toSort(f, pageable.getSort()))
				// A query the index cannot answer within this is given up on, rather than held against everyone else
				.failAfter(properties.timeout().toMillis(), MILLISECONDS)
				.fetch((int) pageable.getOffset(), pageable.getPageSize());
		} catch (final SearchException e) {
			throw SearchProblems.toProblem(e, properties.timeout());
		}

		final var fieldResolver = accessControlService.roleBasedFieldResolver(namespace, municipalityId, user);
		return new PageImpl<>(toErrandsWithAccessControl(result.hits(), fieldResolver), pageable, result.total().hitCount());
	}

	/**
	 * OpenSearch pages through at most so many results of a search. Said up front rather than left to a failed query.
	 */
	private void verifyWithinResultWindow(final Pageable pageable) {
		if (pageable.getOffset() + pageable.getPageSize() > properties.maxResultWindow()) {
			throw Problem.valueOf(BAD_REQUEST, BEYOND_RESULT_WINDOW.formatted(pageable.getPageNumber(), pageable.getPageSize(), properties.maxResultWindow()));
		}
	}

	/**
	 * The sort as JSON rather than through the sort DSL, since the date fields are mapped natively (see
	 * {@link se.sundsvall.supportmanagement.integration.db.search.OffsetDateTimeBinder}) and the DSL knows nothing about
	 * them. One way for every field keeps it simple.
	 */
	/**
	 * Said up front, before the index is asked, rather than from inside the sort being built.
	 */
	private static void verifySortable(final Sort sort) {
		sort.forEach(order -> toIndexField(order.getProperty()));
	}

	private static SortFinalStep toSort(final SearchSortFactory f, final Sort sort) {
		final var clauses = new ArrayList<JsonObject>();

		if (sort.isUnsorted()) {
			clauses.add(clause(SCORE, SortOrder.DESC));
			clauses.add(clause(ErrandIndex.CREATED, SortOrder.DESC));
		} else {
			sort.forEach(order -> clauses.add(clause(toIndexField(order.getProperty()), order.isAscending() ? SortOrder.ASC : SortOrder.DESC)));
		}

		var step = f.extension(ElasticsearchExtension.get()).fromJson(clauses.getFirst());
		for (final var clause : clauses.subList(1, clauses.size())) {
			step = step.then().extension(ElasticsearchExtension.get()).fromJson(clause);
		}
		return step;
	}

	private static JsonObject clause(final String field, final SortOrder order) {
		final var options = new JsonObject();
		options.addProperty("order", order.name().toLowerCase());
		if (!SCORE.equals(field)) {
			options.addProperty("missing", "_last");
		}
		final var clause = new JsonObject();
		clause.add(field, options);
		return clause;
	}

	private static String toIndexField(final String property) {
		return ErrandIndexModel.sortField(property)
			.orElseThrow(() -> Problem.valueOf(BAD_REQUEST, UNSUPPORTED_SORT.formatted(property, ErrandIndexModel.sortableProperties())));
	}
}
