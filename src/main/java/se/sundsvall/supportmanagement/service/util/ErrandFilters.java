package se.sundsvall.supportmanagement.service.util;

import com.turkraft.springfilter.parser.node.FilterNode;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;
import org.springframework.data.core.PropertyPath;
import org.springframework.data.core.PropertyReferenceException;
import org.springframework.data.domain.Pageable;
import se.sundsvall.dept44.problem.Problem;
import se.sundsvall.supportmanagement.integration.db.model.AttachmentDataEntity;
import se.sundsvall.supportmanagement.integration.db.model.AttachmentEntity;
import se.sundsvall.supportmanagement.integration.db.model.DecisionEntity;
import se.sundsvall.supportmanagement.integration.db.model.DecisionJsonParameterEntity;
import se.sundsvall.supportmanagement.integration.db.model.DecisionParameterEntity;
import se.sundsvall.supportmanagement.integration.db.model.DecisionTermEntity;
import se.sundsvall.supportmanagement.integration.db.model.ErrandEntity;
import se.sundsvall.supportmanagement.integration.db.model.InvestigationEntity;
import se.sundsvall.supportmanagement.integration.db.model.InvestigationJsonParameterEntity;
import se.sundsvall.supportmanagement.integration.db.model.InvestigationParameterEntity;
import se.sundsvall.supportmanagement.integration.db.model.InvestigationSectionEntity;
import se.sundsvall.supportmanagement.integration.db.model.InvestigationSectionJsonParameterEntity;
import se.sundsvall.supportmanagement.integration.db.model.InvestigationSectionParameterEntity;
import se.sundsvall.supportmanagement.integration.db.model.JsonParameterEntity;
import se.sundsvall.supportmanagement.integration.db.model.MeasureEntity;
import se.sundsvall.supportmanagement.integration.db.model.MeasureJsonParameterEntity;
import se.sundsvall.supportmanagement.integration.db.model.NotificationEntity;
import se.sundsvall.supportmanagement.integration.db.model.ParameterEntity;
import se.sundsvall.supportmanagement.integration.db.model.StatementEntity;
import se.sundsvall.supportmanagement.integration.db.model.StatementJsonParameterEntity;
import se.sundsvall.supportmanagement.integration.db.model.TimeMeasurementEntity;
import se.sundsvall.supportmanagement.integration.db.model.communication.CommunicationAttachmentEntity;
import se.sundsvall.supportmanagement.integration.db.model.communication.CommunicationEmailHeaderEntity;
import se.sundsvall.supportmanagement.integration.db.model.communication.CommunicationEntity;
import se.sundsvall.supportmanagement.model.enums.ProtectedResource;

import static java.util.Map.entry;
import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.FORBIDDEN;
import static se.sundsvall.supportmanagement.model.enums.ProtectedResource.ATTACHMENT;
import static se.sundsvall.supportmanagement.model.enums.ProtectedResource.JSON_PARAMETER;
import static se.sundsvall.supportmanagement.model.enums.ProtectedResource.MEASURE;
import static se.sundsvall.supportmanagement.model.enums.ProtectedResource.NOTIFICATION;
import static se.sundsvall.supportmanagement.model.enums.ProtectedResource.PARAMETER;
import static se.sundsvall.supportmanagement.model.enums.ProtectedResource.TIME_MEASURE;
import static se.sundsvall.supportmanagement.service.util.ServiceUtil.getCallerIdentity;

/**
 * Guards the filter and the ordering of the errand listing against what hangs off an errand and is guarded on a
 * resource of its own: spring-filter would let a filter walk it just as readily as the stakeholders.
 * <p>
 * Decided by what a path reaches rather than by what it is called. Every child of an errand carries a reference back to
 * it and some carry references to each other, so stakeholders.errandEntity.communications and measures.decisionEntity
 * reach communications and decisions under names of their own. A path is therefore resolved the way Spring Data
 * resolves an ordering, segment by segment from the errand, and held to every entity it passes:
 * <ul>
 * <li>the decisions, statements, investigations and communications, which the errand holds for the search index alone,
 * are not filterable or sortable by anyone. Nothing reaches them through the errand otherwise, and a sort on one of
 * them is resolved into a join of the collection, which multiplies the errands of a page against the count beside
 * it</li>
 * <li>the attachments, parameters, JSON parameters, measures, time measures and notifications are filterable and
 * sortable by a user reaching their resource, which is what the search of the index holds them to as well</li>
 * </ul>
 * Each with the entities belonging to it, so that a reference straight to one of those is held as well. The filter is
 * looked into as parsed, which holds every filter parameter of a request, since spring-filter joins them into one tree.
 */
public final class ErrandFilters {

	static final String NOT_FILTERABLE = "Filtering on '%s' is not supported";
	static final String NOT_SORTABLE = "Sorting on '%s' is not supported";
	static final String NOT_FILTERABLE_BY_USER = "Resource '%s' not filterable by user '%s'";
	static final String NOT_SORTABLE_BY_USER = "Resource '%s' not sortable by user '%s'";

	private static final Set<Class<?>> INDEX_ONLY = Set.of(
		DecisionEntity.class, DecisionJsonParameterEntity.class, DecisionParameterEntity.class, DecisionTermEntity.class,
		StatementEntity.class, StatementJsonParameterEntity.class,
		InvestigationEntity.class, InvestigationJsonParameterEntity.class, InvestigationParameterEntity.class,
		InvestigationSectionEntity.class, InvestigationSectionJsonParameterEntity.class, InvestigationSectionParameterEntity.class,
		CommunicationEntity.class, CommunicationAttachmentEntity.class, CommunicationEmailHeaderEntity.class);

	private static final Map<Class<?>, ProtectedResource> GUARDED = Map.ofEntries(
		entry(AttachmentEntity.class, ATTACHMENT),
		entry(AttachmentDataEntity.class, ATTACHMENT),
		entry(ParameterEntity.class, PARAMETER),
		entry(JsonParameterEntity.class, JSON_PARAMETER),
		entry(MeasureEntity.class, MEASURE),
		entry(MeasureJsonParameterEntity.class, MEASURE),
		entry(TimeMeasurementEntity.class, TIME_MEASURE),
		entry(NotificationEntity.class, NOTIFICATION));

	private ErrandFilters() {}

	/**
	 * @param  filter                                         the parsed filter of the request, or null
	 * @param  reached                                        whether the user reaches sent in resource on every errand
	 *                                                        the listing may answer with
	 * @throws org.springframework.web.ErrorResponseException 400 when the filter reaches what is not filterable, or names
	 *                                                        a path that cannot be resolved, since what it would reach
	 *                                                        cannot be told. 403 when it reaches a resource the user does
	 *                                                        not reach
	 */
	public static void verifyFilterable(final FilterNode filter, final Predicate<ProtectedResource> reached) {
		FilterFields.fieldsOf(filter).forEach(path -> verify(resolve(path)
			.orElseThrow(() -> Problem.valueOf(BAD_REQUEST, NOT_FILTERABLE.formatted(path))), reached, NOT_FILTERABLE, NOT_FILTERABLE_BY_USER));
	}

	/**
	 * @param  pageable                                       the paging of the request, or null
	 * @param  reached                                        whether the user reaches sent in resource on every errand
	 *                                                        the listing may answer with
	 * @throws org.springframework.web.ErrorResponseException 400 when the ordering reaches what is not sortable, and 403
	 *                                                        when it reaches a resource the user does not reach
	 * @throws PropertyReferenceException                     when the ordering names a property the errand does not have,
	 *                                                        just as the repository would refuse it
	 */
	public static void verifySortable(final Pageable pageable, final Predicate<ProtectedResource> reached) {
		if (isNull(pageable)) {
			return;
		}
		pageable.getSort().forEach(order -> verify(segmentsOf(PropertyPath.from(order.getProperty(), ErrandEntity.class)), reached, NOT_SORTABLE, NOT_SORTABLE_BY_USER));
	}

	private static void verify(final List<Segment> segments, final Predicate<ProtectedResource> reached, final String notSupported, final String notByUser) {
		for (final var segment : segments) {
			if (INDEX_ONLY.contains(segment.type())) {
				throw Problem.valueOf(BAD_REQUEST, notSupported.formatted(segment.path()));
			}
			final var resource = GUARDED.get(segment.type());
			if (nonNull(resource) && !reached.test(resource)) {
				throw Problem.valueOf(FORBIDDEN, notByUser.formatted(resource.getPath(), getCallerIdentity()));
			}
		}
	}

	/**
	 * A segment of a path: the path up to and including it, and the type it reaches, which for a collection is its
	 * element and for a map its value.
	 */
	private record Segment(String path, Class<?> type) {}

	/**
	 * Resolves the path from the errand the way Spring Data resolves an ordering, which reads a property such as
	 * communicationsSubject as communications.subject as well.
	 */
	private static Optional<List<Segment>> resolve(final String path) {
		try {
			return Optional.of(segmentsOf(PropertyPath.from(path, ErrandEntity.class)));
		} catch (final PropertyReferenceException _) {
			return Optional.empty();
		}
	}

	private static List<Segment> segmentsOf(final PropertyPath resolved) {
		final var segments = new ArrayList<Segment>();
		var walked = "";
		for (var current = resolved; nonNull(current); current = current.hasNext() ? current.next() : null) {
			walked = walked.isEmpty() ? current.getSegment() : walked + "." + current.getSegment();
			segments.add(new Segment(walked, current.getType()));
		}
		return segments;
	}
}
