package se.sundsvall.supportmanagement.service.util;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.sql.Blob;
import java.sql.SQLException;
import org.springframework.web.multipart.MultipartFile;

public class BlobMultipartFile implements MultipartFile {

	private final String name;
	private final String originalFilename;
	private final String contentType;
	private final long size;
	private final Blob blob;

	public BlobMultipartFile(final String name, final String originalFilename, final String contentType,
		final long size, final Blob blob) {
		this.name = name;
		this.originalFilename = originalFilename;
		this.contentType = contentType;
		this.size = size;
		this.blob = blob;
	}

	@Override
	public String getName() {
		return name;
	}

	@Override
	public String getOriginalFilename() {
		return originalFilename;
	}

	@Override
	public String getContentType() {
		return contentType;
	}

	@Override
	public boolean isEmpty() {
		return size == 0;
	}

	@Override
	public long getSize() {
		return size;
	}

	@Override
	public byte[] getBytes() throws IOException {
		try (final var inputStream = blob.getBinaryStream()) {
			return inputStream.readAllBytes();
		} catch (final SQLException e) {
			throw new IOException("Failed to read blob data", e);
		}
	}

	@Override
	public InputStream getInputStream() throws IOException {
		try {
			return blob.getBinaryStream();
		} catch (final SQLException e) {
			throw new IOException("Failed to get blob input stream", e);
		}
	}

	@Override
	public void transferTo(final File dest) throws IOException {
		try (final var inputStream = blob.getBinaryStream();
			final var outputStream = new java.io.FileOutputStream(dest)) {
			inputStream.transferTo(outputStream);
		} catch (final SQLException e) {
			throw new IOException("Failed to transfer blob data", e);
		}
	}
}
