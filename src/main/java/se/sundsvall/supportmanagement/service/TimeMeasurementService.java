package se.sundsvall.supportmanagement.service;

import java.util.List;
import org.springframework.stereotype.Service;
import se.sundsvall.supportmanagement.api.model.errand.TimeMeasurement;

import static generated.se.sundsvall.accessmapper.Access.AccessLevelEnum.R;
import static generated.se.sundsvall.accessmapper.Access.AccessLevelEnum.RW;
import static se.sundsvall.supportmanagement.service.mapper.TimeMeasurementMapper.toTimeMeasurements;

@Service
public class TimeMeasurementService {

	private final AccessControlService accessControlService;

	public TimeMeasurementService(final AccessControlService accessControlService) {
		this.accessControlService = accessControlService;
	}

	public List<TimeMeasurement> getErrandTimeMeasurements(final String namespace, final String municipalityId, final String errandId) {
		var errandEntity = accessControlService.getErrand(namespace, municipalityId, errandId, false, R, RW);
		return toTimeMeasurements(errandEntity.getTimeMeasures());
	}

}
