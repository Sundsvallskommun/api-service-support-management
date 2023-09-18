package se.sundsvall.supportmanagement.integration.db;

import org.springframework.data.jpa.repository.JpaRepository;

import se.sundsvall.supportmanagement.integration.db.model.ErrandNumberSequenceEntity;

public interface ErrandNumberSequenceRepository extends JpaRepository<ErrandNumberSequenceEntity, Long> {

}
