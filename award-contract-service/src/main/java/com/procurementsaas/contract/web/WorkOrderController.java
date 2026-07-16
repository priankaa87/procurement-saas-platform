package com.procurementsaas.contract.web;

import com.procurementsaas.contract.dto.Dtos.AddLineRequest;
import com.procurementsaas.contract.dto.Dtos.CreateWorkOrderRequest;
import com.procurementsaas.contract.dto.Dtos.DeliveryLineDto;
import com.procurementsaas.contract.dto.Dtos.ExtendRequest;
import com.procurementsaas.contract.dto.Dtos.GoodsReceiptDto;
import com.procurementsaas.contract.dto.Dtos.ReceiveRequest;
import com.procurementsaas.contract.dto.Dtos.WorkOrderDto;
import com.procurementsaas.contract.service.WorkOrderService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
public class WorkOrderController {

    private final WorkOrderService workOrderService;

    public WorkOrderController(WorkOrderService workOrderService) {
        this.workOrderService = workOrderService;
    }

    @GetMapping("/work-orders")
    @PreAuthorize("hasAuthority('FEATURE_CONTRACT_VIEW')")
    public List<WorkOrderDto> list() {
        return workOrderService.list();
    }

    @GetMapping("/work-orders/{code}")
    @PreAuthorize("hasAuthority('FEATURE_CONTRACT_VIEW')")
    public WorkOrderDto get(@PathVariable String code) {
        return workOrderService.get(code);
    }

    /** Raised against an accepted award; rejected with 409 otherwise. */
    @PostMapping("/awards/{tenderCode}/work-orders")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAuthority('FEATURE_CONTRACT_MANAGE')")
    public WorkOrderDto create(@PathVariable String tenderCode,
                               @Valid @RequestBody CreateWorkOrderRequest request) {
        return workOrderService.create(tenderCode, request);
    }

    @GetMapping("/work-orders/{code}/lines")
    @PreAuthorize("hasAuthority('FEATURE_CONTRACT_VIEW')")
    public List<DeliveryLineDto> listLines(@PathVariable String code) {
        return workOrderService.listLines(code);
    }

    @PostMapping("/work-orders/{code}/lines")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAuthority('FEATURE_CONTRACT_MANAGE')")
    public DeliveryLineDto addLine(@PathVariable String code,
                                   @Valid @RequestBody AddLineRequest request) {
        return workOrderService.addLine(code, request);
    }

    @PostMapping("/work-orders/{code}/issue")
    @PreAuthorize("hasAuthority('FEATURE_CONTRACT_MANAGE')")
    public WorkOrderDto issue(@PathVariable String code) {
        return workOrderService.issue(code);
    }

    @PostMapping("/work-orders/{code}/cancel")
    @PreAuthorize("hasAuthority('FEATURE_CONTRACT_MANAGE')")
    public WorkOrderDto cancel(@PathVariable String code) {
        return workOrderService.cancel(code);
    }

    @PutMapping("/work-orders/{code}/lines/{lineNo}/extend")
    @PreAuthorize("hasAuthority('FEATURE_CONTRACT_MANAGE')")
    public DeliveryLineDto extend(@PathVariable String code, @PathVariable int lineNo,
                                  @Valid @RequestBody ExtendRequest request) {
        return workOrderService.extendLine(code, lineNo, request);
    }

    /**
     * Receiving goods is what authorises payment, so it is a warehouse privilege distinct
     * from managing the contract.
     */
    @PostMapping("/work-orders/{code}/lines/{lineNo}/receipts")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAuthority('FEATURE_GOODS_RECEIVE')")
    public DeliveryLineDto receive(@PathVariable String code, @PathVariable int lineNo,
                                   @Valid @RequestBody ReceiveRequest request) {
        return workOrderService.receive(code, lineNo, request);
    }

    @GetMapping("/work-orders/{code}/lines/{lineNo}/receipts")
    @PreAuthorize("hasAuthority('FEATURE_CONTRACT_VIEW')")
    public List<GoodsReceiptDto> listReceipts(@PathVariable String code, @PathVariable int lineNo) {
        return workOrderService.listReceipts(code, lineNo);
    }
}
