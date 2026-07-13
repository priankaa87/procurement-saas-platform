package com.procurementsaas.masterdata.repo;

import com.procurementsaas.masterdata.domain.City;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CityRepository extends JpaRepository<City, Long> {
    List<City> findByCountryIso2(String iso2);
}
