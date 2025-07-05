package com.snapsplit.backend.domain.country.repository;

import com.snapsplit.backend.domain.country.entity.Country;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CountryRepository extends JpaRepository<Country, Long> {
}