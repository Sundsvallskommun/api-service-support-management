package se.sundsvall.supportmanagement.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;
import se.sundsvall.dept44.problem.ThrowableProblem;
import se.sundsvall.supportmanagement.api.model.handover.HandoverPreviewRequest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.springframework.http.HttpStatus.NOT_IMPLEMENTED;

@ExtendWith(MockitoExtension.class)
class HandoverPreviewServiceTest {

	private static final String NAMESPACE = "namespace";
	private static final String MUNICIPALITY_ID = "2281";
	private static final String ERRAND_ID = "errandId";

	@InjectMocks
	private HandoverPreviewService service;

	@Test
	void previewHandoverNotYetImplemented() {
		final var request = HandoverPreviewRequest.create()
			.withTargetNamespace("OTHER_NAMESPACE")
			.withTargetMunicipalityId(MUNICIPALITY_ID);

		final var e = assertThrows(ThrowableProblem.class, () -> service.previewHandover(NAMESPACE, MUNICIPALITY_ID, ERRAND_ID, request));

		assertThat(e.getStatus()).isEqualTo(NOT_IMPLEMENTED);
		assertThat(e.getMessage()).isEqualTo("Not Implemented: Handover preview is not yet implemented");
	}
}
