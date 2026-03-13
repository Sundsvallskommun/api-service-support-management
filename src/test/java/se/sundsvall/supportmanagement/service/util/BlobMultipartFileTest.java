package se.sundsvall.supportmanagement.service.util;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.sql.Blob;
import java.sql.SQLException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BlobMultipartFileTest {

	@Mock
	private Blob blobMock;

	@Test
	void testGetters() {
		final var name = "name";
		final var originalFilename = "original.txt";
		final var contentType = "text/plain";
		final var size = 42L;

		final var file = new BlobMultipartFile(name, originalFilename, contentType, size, blobMock);

		assertThat(file.getName()).isEqualTo(name);
		assertThat(file.getOriginalFilename()).isEqualTo(originalFilename);
		assertThat(file.getContentType()).isEqualTo(contentType);
		assertThat(file.getSize()).isEqualTo(size);
		assertThat(file.isEmpty()).isFalse();
	}

	@Test
	void testIsEmptyWhenSizeIsZero() {
		final var file = new BlobMultipartFile("name", "file.txt", "text/plain", 0, blobMock);

		assertThat(file.isEmpty()).isTrue();
	}

	@Test
	void testGetBytes() throws Exception {
		final var content = "test content".getBytes();

		when(blobMock.getBinaryStream()).thenReturn(new java.io.ByteArrayInputStream(content));

		final var file = new BlobMultipartFile("name", "file.txt", "text/plain", content.length, blobMock);

		assertThat(file.getBytes()).isEqualTo(content);
	}

	@Test
	void testGetBytesThrowsIOExceptionOnSQLException() throws Exception {
		when(blobMock.getBinaryStream()).thenThrow(new SQLException("db error"));

		final var file = new BlobMultipartFile("name", "file.txt", "text/plain", 10, blobMock);

		assertThatThrownBy(file::getBytes).isInstanceOf(IOException.class)
			.hasMessageContaining("Failed to read blob data");
	}

	@Test
	void testGetInputStream() throws Exception {
		final var content = "test content".getBytes();

		when(blobMock.getBinaryStream()).thenReturn(new java.io.ByteArrayInputStream(content));

		final var file = new BlobMultipartFile("name", "file.txt", "text/plain", content.length, blobMock);

		assertThat(file.getInputStream().readAllBytes()).isEqualTo(content);
	}

	@Test
	void testGetInputStreamThrowsIOExceptionOnSQLException() throws Exception {
		when(blobMock.getBinaryStream()).thenThrow(new SQLException("db error"));

		final var file = new BlobMultipartFile("name", "file.txt", "text/plain", 10, blobMock);

		assertThatThrownBy(file::getInputStream).isInstanceOf(IOException.class)
			.hasMessageContaining("Failed to get blob input stream");
	}

	@Test
	void testTransferTo(@TempDir File tempDir) throws Exception {
		final var content = "test content".getBytes();
		final var dest = new File(tempDir, "output.txt");

		when(blobMock.getBinaryStream()).thenReturn(new java.io.ByteArrayInputStream(content));

		final var file = new BlobMultipartFile("name", "file.txt", "text/plain", content.length, blobMock);

		file.transferTo(dest);

		assertThat(Files.readAllBytes(dest.toPath())).isEqualTo(content);
	}

	@Test
	void testTransferToThrowsIOExceptionOnSQLException(@TempDir File tempDir) throws Exception {
		final var dest = new File(tempDir, "output.txt");

		when(blobMock.getBinaryStream()).thenThrow(new SQLException("db error"));

		final var file = new BlobMultipartFile("name", "file.txt", "text/plain", 10, blobMock);

		assertThatThrownBy(() -> file.transferTo(dest)).isInstanceOf(IOException.class)
			.hasMessageContaining("Failed to transfer blob data");
	}
}
