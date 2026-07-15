package com.procurementsaas.masterdata.service;

import com.procurementsaas.common.web.NotFoundException;
import com.procurementsaas.masterdata.domain.City;
import com.procurementsaas.masterdata.domain.Country;
import com.procurementsaas.masterdata.dto.Dtos.CityDto;
import com.procurementsaas.masterdata.dto.Dtos.CountryDto;
import com.procurementsaas.masterdata.dto.Dtos.CreateCityRequest;
import com.procurementsaas.masterdata.dto.Dtos.CreateCountryRequest;
import com.procurementsaas.masterdata.repo.CityRepository;
import com.procurementsaas.masterdata.repo.CountryRepository;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/** Geography reference data (countries and cities). Reads cached; mutations evict. */
@Service
@Transactional
public class GeoService {

    private final CountryRepository countryRepository;
    private final CityRepository cityRepository;

    public GeoService(CountryRepository countryRepository, CityRepository cityRepository) {
        this.countryRepository = countryRepository;
        this.cityRepository = cityRepository;
    }

    @Transactional(readOnly = true)
    @Cacheable("countries")
    public List<CountryDto> listCountries() {
        return countryRepository.findAll().stream().map(MasterDataMapper::toDto).toList();
    }

    @CacheEvict(value = "countries", allEntries = true)
    public CountryDto createCountry(CreateCountryRequest request) {
        if (countryRepository.existsByIso2(request.iso2())) {
            throw new IllegalArgumentException("Country already exists: " + request.iso2());
        }
        Country country = new Country(request.iso2(), request.name());
        return MasterDataMapper.toDto(countryRepository.save(country));
    }

    @Transactional(readOnly = true)
    public List<CityDto> listCities(String countryIso2) {
        return cityRepository.findByCountryIso2(countryIso2).stream()
            .map(MasterDataMapper::toDto).toList();
    }

    public CityDto createCity(CreateCityRequest request) {
        Country country = countryRepository.findByIso2(request.countryIso2())
            .orElseThrow(() -> new NotFoundException("Country not found: " + request.countryIso2()));
        City city = new City(request.name(), country);
        return MasterDataMapper.toDto(cityRepository.save(city));
    }
}
