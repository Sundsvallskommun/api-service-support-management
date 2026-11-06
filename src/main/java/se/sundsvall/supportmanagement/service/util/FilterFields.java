package se.sundsvall.supportmanagement.service.util;

import com.turkraft.springfilter.parser.node.CollectionLikeNode;
import com.turkraft.springfilter.parser.node.FieldNode;
import com.turkraft.springfilter.parser.node.FilterNode;
import java.util.List;
import java.util.stream.Stream;

import static java.util.Objects.isNull;
import static java.util.Optional.ofNullable;

/**
 * The fields a filter names, as spring-filter parsed it, read the same way wherever the service looks into a filter.
 */
final class FilterFields {

	private FilterFields() {}

	/**
	 * @param  filter the parsed filter, or null
	 * @return        the name of every field anywhere in the tree, values being nodes of their own
	 */
	static Stream<String> fieldsOf(final FilterNode filter) {
		return nodesOf(filter)
			.filter(FieldNode.class::isInstance)
			.map(FieldNode.class::cast)
			.map(FieldNode::getName);
	}

	// The node itself and every node below it. The patterns of a like against a list, field ~ [..] and field ~~ [..], are
	// nodes of the tree as much as its left operand, although spring-filter leaves them out of the children of the node
	private static Stream<FilterNode> nodesOf(final FilterNode node) {
		if (isNull(node)) {
			return Stream.empty();
		}
		final var patterns = node instanceof final CollectionLikeNode collectionLikeNode ? collectionLikeNode.getPatterns() : null;

		return Stream.concat(Stream.of(node), Stream.concat(listOf(node.getChildren()), listOf(patterns)).flatMap(FilterFields::nodesOf));
	}

	private static Stream<FilterNode> listOf(final List<FilterNode> nodes) {
		return ofNullable(nodes).orElse(List.of()).stream();
	}
}
