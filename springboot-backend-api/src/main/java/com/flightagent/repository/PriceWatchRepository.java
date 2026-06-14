package com.flightagent.repository;

import com.flightagent.entity.PriceWatchEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PriceWatchRepository extends JpaRepository<PriceWatchEntity, String> {
}
