package org.example.springbatch.repository;

import org.example.springbatch.entity.BeforeEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BeforeRepository extends JpaRepository<BeforeEntity, Long> {
}
