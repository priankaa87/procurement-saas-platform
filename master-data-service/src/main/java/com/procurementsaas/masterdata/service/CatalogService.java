package com.procurementsaas.masterdata.service;

import com.procurementsaas.common.web.NotFoundException;
import com.procurementsaas.masterdata.domain.Item;
import com.procurementsaas.masterdata.domain.ItemCategory;
import com.procurementsaas.masterdata.domain.MeasurementUnit;
import com.procurementsaas.masterdata.dto.Dtos.CreateCurrencyRequest;
import com.procurementsaas.masterdata.dto.Dtos.CreateItemCategoryRequest;
import com.procurementsaas.masterdata.dto.Dtos.CreateItemRequest;
import com.procurementsaas.masterdata.dto.Dtos.CreateUnitRequest;
import com.procurementsaas.masterdata.dto.Dtos.CurrencyDto;
import com.procurementsaas.masterdata.dto.Dtos.ItemCategoryDto;
import com.procurementsaas.masterdata.dto.Dtos.ItemDto;
import com.procurementsaas.masterdata.dto.Dtos.UnitDto;
import com.procurementsaas.masterdata.domain.Currency;
import com.procurementsaas.masterdata.repo.CurrencyRepository;
import com.procurementsaas.masterdata.repo.ItemCategoryRepository;
import com.procurementsaas.masterdata.repo.ItemRepository;
import com.procurementsaas.masterdata.repo.MeasurementUnitRepository;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Read-heavy catalog reference data (units, currencies, item categories and items).
 * List reads are cached; mutations evict the relevant caches.
 */
@Service
@Transactional
public class CatalogService {

    private final MeasurementUnitRepository unitRepository;
    private final CurrencyRepository currencyRepository;
    private final ItemCategoryRepository categoryRepository;
    private final ItemRepository itemRepository;

    public CatalogService(MeasurementUnitRepository unitRepository,
                          CurrencyRepository currencyRepository,
                          ItemCategoryRepository categoryRepository,
                          ItemRepository itemRepository) {
        this.unitRepository = unitRepository;
        this.currencyRepository = currencyRepository;
        this.categoryRepository = categoryRepository;
        this.itemRepository = itemRepository;
    }

    // --- Units ---------------------------------------------------------------

    @Transactional(readOnly = true)
    @Cacheable("units")
    public List<UnitDto> listUnits() {
        return unitRepository.findAll().stream().map(MasterDataMapper::toDto).toList();
    }

    @CacheEvict(value = "units", allEntries = true)
    public UnitDto createUnit(CreateUnitRequest request) {
        if (unitRepository.existsByCode(request.code())) {
            throw new IllegalArgumentException("Unit code already exists: " + request.code());
        }
        MeasurementUnit unit = new MeasurementUnit(request.code(), request.name(), request.symbol());
        return MasterDataMapper.toDto(unitRepository.save(unit));
    }

    // --- Currencies ----------------------------------------------------------

    @Transactional(readOnly = true)
    @Cacheable("currencies")
    public List<CurrencyDto> listCurrencies() {
        return currencyRepository.findAll().stream().map(MasterDataMapper::toDto).toList();
    }

    @CacheEvict(value = "currencies", allEntries = true)
    public CurrencyDto createCurrency(CreateCurrencyRequest request) {
        if (currencyRepository.existsByCode(request.code())) {
            throw new IllegalArgumentException("Currency code already exists: " + request.code());
        }
        Currency currency = new Currency(request.code(), request.name(), request.symbol());
        return MasterDataMapper.toDto(currencyRepository.save(currency));
    }

    // --- Item categories -----------------------------------------------------

    @Transactional(readOnly = true)
    @Cacheable("itemCategories")
    public List<ItemCategoryDto> listCategories() {
        return categoryRepository.findAll().stream().map(MasterDataMapper::toDto).toList();
    }

    @CacheEvict(value = "itemCategories", allEntries = true)
    public ItemCategoryDto createCategory(CreateItemCategoryRequest request) {
        if (categoryRepository.existsByCode(request.code())) {
            throw new IllegalArgumentException("Category code already exists: " + request.code());
        }
        ItemCategory category = new ItemCategory(request.code(), request.name(), request.description());
        return MasterDataMapper.toDto(categoryRepository.save(category));
    }

    // --- Items ---------------------------------------------------------------

    @Transactional(readOnly = true)
    @Cacheable("items")
    public List<ItemDto> listItems() {
        return itemRepository.findAll().stream().map(MasterDataMapper::toDto).toList();
    }

    @Caching(evict = {@CacheEvict(value = "items", allEntries = true)})
    public ItemDto createItem(CreateItemRequest request) {
        if (itemRepository.existsByCode(request.code())) {
            throw new IllegalArgumentException("Item code already exists: " + request.code());
        }
        ItemCategory category = categoryRepository.findByCode(request.categoryCode())
            .orElseThrow(() -> new NotFoundException("Category not found: " + request.categoryCode()));
        MeasurementUnit unit = unitRepository.findByCode(request.unitCode())
            .orElseThrow(() -> new NotFoundException("Unit not found: " + request.unitCode()));
        Item item = new Item(request.code(), request.name(), request.description(), category, unit);
        return MasterDataMapper.toDto(itemRepository.save(item));
    }
}
