package com.procurementsaas.tender.web;

import com.procurementsaas.tender.dto.Dtos.AddItemRequest;
import com.procurementsaas.tender.dto.Dtos.AwardRequest;
import com.procurementsaas.tender.dto.Dtos.CreateTenderRequest;
import com.procurementsaas.tender.dto.Dtos.InviteRequest;
import com.procurementsaas.tender.dto.Dtos.ParticipantDto;
import com.procurementsaas.tender.dto.Dtos.TenderDto;
import com.procurementsaas.tender.dto.Dtos.TenderItemDto;
import com.procurementsaas.tender.service.TenderService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/tenders")
public class TenderController {

    private final TenderService tenderService;

    public TenderController(TenderService tenderService) {
        this.tenderService = tenderService;
    }

    @GetMapping
    @PreAuthorize("hasAuthority('FEATURE_TENDER_VIEW')")
    public List<TenderDto> list(@RequestParam(required = false) String status) {
        return tenderService.list(status);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('FEATURE_TENDER_VIEW')")
    public TenderDto get(@PathVariable Long id) {
        return tenderService.get(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAuthority('FEATURE_TENDER_MANAGE')")
    public TenderDto create(@Valid @RequestBody CreateTenderRequest request) {
        return tenderService.create(request);
    }

    @GetMapping("/{id}/items")
    @PreAuthorize("hasAuthority('FEATURE_TENDER_VIEW')")
    public List<TenderItemDto> listItems(@PathVariable Long id) {
        return tenderService.listItems(id);
    }

    @PostMapping("/{id}/items")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAuthority('FEATURE_TENDER_MANAGE')")
    public TenderItemDto addItem(@PathVariable Long id, @Valid @RequestBody AddItemRequest request) {
        return tenderService.addItem(id, request);
    }

    @GetMapping("/{id}/participants")
    @PreAuthorize("hasAuthority('FEATURE_TENDER_VIEW')")
    public List<ParticipantDto> listParticipants(@PathVariable Long id) {
        return tenderService.listParticipants(id);
    }

    @PostMapping("/{id}/participants")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAuthority('FEATURE_TENDER_MANAGE')")
    public ParticipantDto invite(@PathVariable Long id, @Valid @RequestBody InviteRequest request) {
        return tenderService.invite(id, request);
    }

    @PostMapping("/{id}/publish")
    @PreAuthorize("hasAuthority('FEATURE_TENDER_MANAGE')")
    public TenderDto publish(@PathVariable Long id) {
        return tenderService.publish(id);
    }

    @PostMapping("/{id}/open")
    @PreAuthorize("hasAuthority('FEATURE_TENDER_OPEN')")
    public TenderDto open(@PathVariable Long id) {
        return tenderService.open(id);
    }

    @PostMapping("/{id}/award")
    @PreAuthorize("hasAuthority('FEATURE_TENDER_AWARD')")
    public TenderDto award(@PathVariable Long id, @Valid @RequestBody AwardRequest request) {
        return tenderService.award(id, request);
    }

    @PostMapping("/{id}/cancel")
    @PreAuthorize("hasAuthority('FEATURE_TENDER_MANAGE')")
    public TenderDto cancel(@PathVariable Long id) {
        return tenderService.cancel(id);
    }
}
