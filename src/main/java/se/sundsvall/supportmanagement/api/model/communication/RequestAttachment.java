package se.sundsvall.supportmanagement.api.model.communication;

public interface RequestAttachment {
	String getFileName();

	String getBase64EncodedString();
}
