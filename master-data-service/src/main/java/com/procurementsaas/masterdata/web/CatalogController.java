package com.procurementsaas.masterdata.web;

import com.procurementsaas.masterdata.dto.Dtos.CreateCurrencyRequest;
import com.procurementsaas.masterdata.dto.Dtos.CreateItemCategoryRequest;
import com.procurementsaas.masterdata.dto.Dtos.CreateItemRequest;
import com.procurementsaas.masterdata.dto.Dtos.CreateUnitRequest;
import com.procurementsaas.masterdata.dto.Dtos.CurrencyDto;
import com.procurementsaas.masterdata.dto.Dtos.ItemCategoryDto;
import com.procurementsaas.masterdata.dto.Dtos.ItemDto;
import com.procurementsaas.masterdata.dto.Dtos.UnitDto;
import com.procurementsaas.masterdata.service.CatalogService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** Catalog reference data: units, currencies, item categories, and items. */
@RestController
public class CatalogController {

    private final CatalogService catalogService;

    public CatalogController(CatalogService catalogService) {
        this.catalogService = catalogService;
    }

    @GetMapping("/units")
    @PreAuthorize("hasAuthority('FEATURE_MASTERDATA_VIEW')")
    public List<UnitDto> listUnits() {
        return catalogService.listUnits();
    }

    @PostMapping("/units")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAuthority('FEATURE_MASTERDATA_MANAGE')")
    public UnitDto createUnit(@Valid @RequestBody CreateUnitRequest request) {
        return catalogService.createUnit(request);
    }

    @GetMapping("/currencies")
    @PreAuthorize("hasAuthority('FEATURE_MASTERDATA_VIEW')")
    public List<CurrencyDto> listCurrencies() {
        return catalogService.listCurrencies();
    }

    @PostMapping("/currencies")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAuthority('FEATURE_MASTERDATA_MANAGE')")
    public CurrencyDto createCurrency(@Valid @RequestBody CreateCurrencyRequest request) {
        return catalogService.createCurrency(request);
    }

    @GetMapping("/item-categories")
    @PreAuthorize("hasAuthority('FEATURE_MASTERDATA_VIEW')")
    public List<ItemCategoryDto> listCategories() {
        return catalogService.listCategories();
    }

    @PostMapping("/item-categories")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAuthority('FEATURE_MASTERDATA_MANAGE')")
    public ItemCategoryDto createCategory(@Valid @RequestBody CreateItemCategoryRequest request) {
        return catalogService.createCategory(request);
    }

    @GetMapping("/items")
    @PreAuthorize("hasAuthority('FEATURE_MASTERDATA_VIEW')")
    public List<ItemDto> listItems() {
        return catalogService.listItems();
    }

    @PostMapping("/items")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAuthority('FEATURE_MASTERDATA_MANAGE')")
    public ItemDto createItem(@Valid @RequestBody CreateItemRequest request) {
        return catalogService.createItem(request);
    }
}
