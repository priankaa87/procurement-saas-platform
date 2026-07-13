package com.procurementsaas.masterdata.dto;

import jakarta.validation.constraints.NotBlank;

/** Request/response payloads for the Master Data API, grouped as records for brevity. */
public final class Dtos {

    private Dtos() {
    }

    public record UnitDto(Long id, String code, String name, String symbol) {
    }

    public record CurrencyDto(Long id, String code, String name, String symbol) {
    }

    public record ItemCategoryDto(Long id, String code, String name, String description) {
    }

    public record ItemDto(Long id, String code, String name, String description,
                          String categoryCode, String unitCode, boolean active) {
    }

    public record CountryDto(Long id, String iso2, String name) {
    }

    public record CityDto(Long id, String name, String countryIso2) {
    }

    public record CreateUnitRequest(@NotBlank String code, @NotBlank String name, String symbol) {
    }

    public record CreateCurrencyRequest(@NotBlank String code, @NotBlank String name, String symbol) {
    }

    public record CreateItemCategoryRequest(@NotBlank String code, @NotBlank String name, String description) {
    }

    public record CreateItemRequest(@NotBlank String code, @NotBlank String name, String description,
                                    @NotBlank String categoryCode, @NotBlank String unitCode) {
    }

    public record CreateCountryRequest(@NotBlank String iso2, @NotBlank String name) {
    }

    public record CreateCityRequest(@NotBlank String name, @NotBlank String countryIso2) {
    }
}
