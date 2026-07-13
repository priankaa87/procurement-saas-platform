package com.procurementsaas.masterdata.web;

import com.procurementsaas.masterdata.dto.Dtos.CityDto;
import com.procurementsaas.masterdata.dto.Dtos.CountryDto;
import com.procurementsaas.masterdata.dto.Dtos.CreateCityRequest;
import com.procurementsaas.masterdata.dto.Dtos.CreateCountryRequest;
import com.procurementsaas.masterdata.service.GeoService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** Geography reference data: countries and their cities. */
@RestController
public class GeoController {

    private final GeoService geoService;

    public GeoController(GeoService geoService) {
        this.geoService = geoService;
    }

    @GetMapping("/countries")
    @PreAuthorize("hasAuthority('FEATURE_MASTERDATA_VIEW')")
    public List<CountryDto> listCountries() {
        return geoService.listCountries();
    }

    @PostMapping("/countries")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAuthority('FEATURE_MASTERDATA_MANAGE')")
    public CountryDto createCountry(@Valid @RequestBody CreateCountryRequest request) {
        return geoService.createCountry(request);
    }

    @GetMapping("/countries/{iso2}/cities")
    @PreAuthorize("hasAuthority('FEATURE_MASTERDATA_VIEW')")
    public List<CityDto> listCities(@PathVariable String iso2) {
        return geoService.listCities(iso2);
    }

    @PostMapping("/cities")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAuthority('FEATURE_MASTERDATA_MANAGE')")
    public CityDto createCity(@Valid @RequestBody CreateCityRequest request) {
        return geoService.createCity(request);
    }
}
