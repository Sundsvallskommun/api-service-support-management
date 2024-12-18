package se.sundsvall.supportmanagement.api.model;

import static io.swagger.v3.oas.annotations.media.Schema.AccessMode.READ_ONLY;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.Objects;

@Schema(description = "Metadata model")
public class MetaData {

	@Schema(description = "Current page", example = "5", accessMode = READ_ONLY)
	private int page;

	@Schema(description = "Displayed objects per page", example = "20", accessMode = READ_ONLY)
	private int limit;

	@Schema(description = "Displayed objects on current page", example = "13", accessMode = READ_ONLY)
	private int count;

	@Schema(description = "Total amount of hits based on provided search parameters", example = "98", accessMode = READ_ONLY)
	private long totalRecords;

	@Schema(description = "Total amount of pages based on provided search parameters", example = "23", accessMode = READ_ONLY)
	private int totalPages;

	public static MetaData create() {
		return new MetaData();
	}

	public long getTotalRecords() {
		return totalRecords;
	}

	public void setTotalRecords(final long totalRecords) {
		this.totalRecords = totalRecords;
	}

	public MetaData withTotalRecords(final long totalRecords) {
		this.totalRecords = totalRecords;
		return this;
	}

	public int getTotalPages() {
		return totalPages;
	}

	public void setTotalPages(final int totalPages) {
		this.totalPages = totalPages;
	}

	public MetaData withTotalPages(final int totalPages) {
		this.totalPages = totalPages;
		return this;
	}

	public int getPage() {
		return page;
	}

	public void setPage(final int page) {
		this.page = page;
	}

	public MetaData withPage(final int page) {
		this.page = page;
		return this;
	}

	public int getLimit() {
		return limit;
	}

	public void setLimit(final int limit) {
		this.limit = limit;
	}

	public MetaData withLimit(final int limit) {
		this.limit = limit;
		return this;
	}

	public int getCount() {
		return count;
	}

	public void setCount(final int count) {
		this.count = count;
	}

	public MetaData withCount(final int count) {
		this.count = count;
		return this;
	}

	@Override
	public int hashCode() {
		return Objects.hash(count, limit, page, totalPages, totalRecords);
	}

	@Override
	public boolean equals(final Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null) {
			return false;
		}
		if (getClass() != obj.getClass()) {
			return false;
		}
		final var other = (MetaData) obj;
		return count == other.count && limit == other.limit && page == other.page && totalPages == other.totalPages && totalRecords == other.totalRecords;
	}

	@Override
	public String toString() {
		return "MetaData{" +
			"page=" + page +
			", limit=" + limit +
			", count=" + count +
			", totalRecords=" + totalRecords +
			", totalPages=" + totalPages +
			'}';
	}
}
