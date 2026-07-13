package com.procurementsaas.masterdata.service;

import com.procurementsaas.masterdata.domain.City;
import com.procurementsaas.masterdata.domain.Country;
import com.procurementsaas.masterdata.domain.Currency;
import com.procurementsaas.masterdata.domain.Item;
import com.procurementsaas.masterdata.domain.ItemCategory;
import com.procurementsaas.masterdata.domain.MeasurementUnit;
import com.procurementsaas.masterdata.dto.Dtos.CityDto;
import com.procurementsaas.masterdata.dto.Dtos.CountryDto;
import com.procurementsaas.masterdata.dto.Dtos.CurrencyDto;
import com.procurementsaas.masterdata.dto.Dtos.ItemCategoryDto;
import com.procurementsaas.masterdata.dto.Dtos.ItemDto;
import com.procurementsaas.masterdata.dto.Dtos.UnitDto;

/** Maps master-data entities to API DTOs. */
public final class MasterDataMapper {

    private MasterDataMapper() {
    }

    public static UnitDto toDto(MeasurementUnit u) {
        return new UnitDto(u.getId(), u.getCode(), u.getName(), u.getSymbol());
    }

    public static CurrencyDto toDto(Currency c) {
        return new CurrencyDto(c.getId(), c.getCode(), c.getName(), c.getSymbol());
    }

    public static ItemCategoryDto toDto(ItemCategory c) {
        return new ItemCategoryDto(c.getId(), c.getCode(), c.getName(), c.getDescription());
    }

    public static ItemDto toDto(Item i) {
        return new ItemDto(i.getId(), i.getCode(), i.getName(), i.getDescription(),
            i.getCategory().getCode(), i.getUnit().getCode(), i.isActive());
    }

    public static CountryDto toDto(Country c) {
        return new CountryDto(c.getId(), c.getIso2(), c.getName());
    }

    public static CityDto toDto(City c) {
        return new CityDto(c.getId(), c.getName(), c.getCountry().getIso2());
    }
}
