package se.sundsvall.supportmanagement.integration.db;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import se.sundsvall.supportmanagement.integration.db.model.ParameterEntity;

@Repository
public interface ParameterRepository extends JpaRepository<ParameterEntity, String> {
}
