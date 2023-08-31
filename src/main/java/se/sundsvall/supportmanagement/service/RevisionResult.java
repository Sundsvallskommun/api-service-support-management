package se.sundsvall.supportmanagement.service;

import se.sundsvall.supportmanagement.api.model.revision.Revision;

public record RevisionResult(Revision previous, Revision latest) {

}
