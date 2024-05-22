package se.sundsvall.supportmanagement.integration.db;


import org.springframework.data.jpa.repository.JpaRepository;

import se.sundsvall.supportmanagement.integration.db.model.TimeMeasureEntity;

public interface TimeMeasureRepository extends JpaRepository<TimeMeasureEntity, Long> {

}
