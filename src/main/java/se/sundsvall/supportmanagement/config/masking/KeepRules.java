package se.sundsvall.supportmanagement.config.masking;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * The entries of {@link PayloadMaskingProperties#keep()}, compiled into something a walk of a body can ask per field.
 * <p>
 * An entry is a path: the part of JSONPath that a field can be named by, and no more. {@code $} is the root,
 * {@code .field} a child, {@code ..field} a descendant at any depth, {@code *} any one field, and {@code [*]} the
 * elements of an array. A predicate, a slice, an index or a function is refused when the rules are compiled rather
 * than quietly never matching.
 * <p>
 * {@code $..errandNumber} therefore keeps that field wherever it occurs, and {@code $.content[*].title} keeps the
 * title of the errands of a page and nothing else. The leading {@code $} is required of every entry, so that the
 * difference between the two - which is the whole point of writing a path rather than a field name - is visible on
 * every line of a list that people maintain by hand.
 * <p>
 * Array nesting is transparent, so {@code [*]} may be left out: {@code $.content.title} and
 * {@code $.content[*].title} are the same rule.
 */
final class KeepRules {

	/** Stands for {@code ..} in a compiled rule: any number of fields, including none, before the next segment. */
	private static final String DESCENDANT = "..";

	/** Stands for {@code *} in a compiled rule: exactly one field, whatever it is called. */
	private static final String ANY = "*";

	/**
	 * Rules by the field they end in, so a field is only matched against the rules that could name it. Nearly every
	 * rule ends in a field, which is what keeps the cost of a rule set near the cost of a lookup.
	 */
	private final Map<String, List<String[]>> byField;

	/** Rules ending in {@code *}, which no field name can be indexed by. */
	private final List<String[]> endingInAny;

	private KeepRules(final Map<String, List<String[]>> byField, final List<String[]> endingInAny) {
		this.byField = byField;
		this.endingInAny = endingInAny;
	}

	static KeepRules of(final Collection<String> entries) {
		final var byField = new HashMap<String, List<String[]>>();
		final var endingInAny = new ArrayList<String[]>();

		// Compiled once, at startup, and only read from afterwards: the rules are shared by every thread that logs
		entries.stream().map(KeepRules::compile).forEach(rule -> {
			final var last = rule[rule.length - 1];
			if (ANY.equals(last)) {
				endingInAny.add(rule);
			} else {
				byField.computeIfAbsent(last, field -> new ArrayList<>()).add(rule);
			}
		});

		return new KeepRules(byField, endingInAny);
	}

	/**
	 * @param  path the fields from the root of the body down to and including the one being asked about. An array is
	 *              not a field of its own, so the strings of {@code recipients} are asked about as {@code recipients}.
	 * @return      whether the field at this path is kept as it is.
	 */
	boolean keeps(final List<String> path) {
		// The strings of an array at the root of a body are under no field at all, and no rule can name them
		if (path.isEmpty()) {
			return false;
		}

		final var rules = byField.get(path.getLast());
		if (rules != null) {
			for (final var rule : rules) {
				if (matches(rule, 0, path, 0)) {
					return true;
				}
			}
		}
		for (final var rule : endingInAny) {
			if (matches(rule, 0, path, 0)) {
				return true;
			}
		}
		return false;
	}

	private static boolean matches(final String[] rule, final int r, final List<String> path, final int p) {
		if (r == rule.length) {
			return p == path.size();
		}
		if (DESCENDANT.equals(rule[r])) {
			for (var skipped = p; skipped <= path.size(); skipped++) {
				if (matches(rule, r + 1, path, skipped)) {
					return true;
				}
			}
			return false;
		}
		return p < path.size()
			&& (ANY.equals(rule[r]) || rule[r].equals(path.get(p)))
			&& matches(rule, r + 1, path, p + 1);
	}

	/**
	 * @throws IllegalArgumentException when the entry is not a path within the supported syntax. Thrown when
	 *                                  the rules are compiled, which is when the service starts: a rule that cannot be
	 *                                  read is a field logged or withheld against what was meant, and neither is worth
	 *                                  discovering from the logs.
	 */
	private static String[] compile(final String entry) {
		if (entry == null || entry.isBlank()) {
			throw new IllegalArgumentException("A field to keep cannot be blank");
		}
		if (!entry.startsWith("$")) {
			throw new IllegalArgumentException(
				"'%s' is not a path. Write '$..%s' to keep it wherever it occurs, or anchor it from the root".formatted(entry, entry));
		}

		final var path = entry.substring(1).replace("[*]", "");
		refuse(path, entry);

		final var segments = new ArrayList<String>();
		var i = 0;
		while (i < path.length()) {
			if (path.startsWith(DESCENDANT, i)) {
				segments.add(DESCENDANT);
				i += 2;
			} else if (path.charAt(i) == '.') {
				i++;
			} else {
				throw new IllegalArgumentException("'%s' is not a path: a field must follow a '.'".formatted(entry));
			}
			final var end = indexOfDot(path, i);
			final var field = path.substring(i, end);
			if (field.isEmpty()) {
				throw new IllegalArgumentException("'%s' names an empty field".formatted(entry));
			}
			segments.add(field);
			i = end;
		}

		// A '..' is always followed by a field, since reading one is what the loop above does next, so the last segment
		// of a compiled rule is a field or '*' and never a '..'
		return segments.toArray(String[]::new);
	}

	private static int indexOfDot(final String path, final int from) {
		final var dot = path.indexOf('.', from);
		return dot < 0 ? path.length() : dot;
	}

	private static void refuse(final String path, final String entry) {
		if (path.indexOf('[') >= 0 || path.indexOf(']') >= 0) {
			throw new IllegalArgumentException(
				"'%s' uses a predicate, a slice or an index. Only '[*]' is supported, and it may be left out".formatted(entry));
		}
		if (path.indexOf('(') >= 0) {
			throw new IllegalArgumentException("'%s' uses a function, which is not supported".formatted(entry));
		}
		if (!path.startsWith(".")) {
			throw new IllegalArgumentException("'%s' must continue with '.' or '..' after the root".formatted(entry));
		}
	}
}
