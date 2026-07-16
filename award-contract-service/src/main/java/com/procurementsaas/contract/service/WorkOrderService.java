package com.procurementsaas.contract.service;

import com.procurementsaas.common.web.NotFoundException;
import com.procurementsaas.contract.domain.Award;
import com.procurementsaas.contract.domain.DeliveryLine;
import com.procurementsaas.contract.domain.GoodsReceipt;
import com.procurementsaas.contract.domain.WorkOrder;
import com.procurementsaas.contract.dto.Dtos.AddLineRequest;
import com.procurementsaas.contract.dto.Dtos.CreateWorkOrderRequest;
import com.procurementsaas.contract.dto.Dtos.DeliveryLineDto;
import com.procurementsaas.contract.dto.Dtos.ExtendRequest;
import com.procurementsaas.contract.dto.Dtos.GoodsReceiptDto;
import com.procurementsaas.contract.dto.Dtos.ReceiveRequest;
import com.procurementsaas.contract.dto.Dtos.WorkOrderDto;
import com.procurementsaas.contract.repo.DeliveryLineRepository;
import com.procurementsaas.contract.repo.GoodsReceiptRepository;
import com.procurementsaas.contract.repo.WorkOrderRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

/** Raising work orders from accepted awards, and receiving goods against them. */
@Service
@Transactional
public class WorkOrderService {

    private final WorkOrderRepository workOrderRepository;
    private final DeliveryLineRepository lineRepository;
    private final GoodsReceiptRepository receiptRepository;
    private final AwardService awardService;

    public WorkOrderService(WorkOrderRepository workOrderRepository,
                            DeliveryLineRepository lineRepository,
                            GoodsReceiptRepository receiptRepository,
                            AwardService awardService) {
        this.workOrderRepository = workOrderRepository;
        this.lineRepository = lineRepository;
        this.receiptRepository = receiptRepository;
        this.awardService = awardService;
    }

    @Transactional(readOnly = true)
    public List<WorkOrderDto> list() {
        return workOrderRepository.findAll().stream().map(this::withLineCount).toList();
    }

    @Transactional(readOnly = true)
    public WorkOrderDto get(String code) {
        return withLineCount(findWorkOrder(code));
    }

    /** The award must have been accepted — the entity enforces it. */
    public WorkOrderDto create(String tenderCode, CreateWorkOrderRequest request) {
        Award award = awardService.findAward(tenderCode);
        if (workOrderRepository.existsByCode(request.code())) {
            throw new IllegalArgumentException("Work order code already exists: " + request.code());
        }
        WorkOrder workOrder = new WorkOrder(award, request.code());
        return withLineCount(workOrderRepository.save(workOrder));
    }

    // --- Delivery schedule ---------------------------------------------------

    @Transactional(readOnly = true)
    public List<DeliveryLineDto> listLines(String code) {
        WorkOrder workOrder = findWorkOrder(code);
        return lineRepository.findByWorkOrderIdOrderByLineNo(workOrder.getId()).stream()
            .map(ContractMapper::toDto).toList();
    }

    /** The schedule is fixed once the order is issued to the supplier. */
    public DeliveryLineDto addLine(String code, AddLineRequest request) {
        WorkOrder workOrder = findWorkOrder(code);
        if (!workOrder.isEditable()) {
            throw new IllegalStateException(
                "Delivery lines cannot be changed once the work order is issued: " + code);
        }
        if (lineRepository.findByWorkOrderIdAndLineNo(workOrder.getId(), request.lineNo()).isPresent()) {
            throw new IllegalArgumentException("Line number already used: " + request.lineNo());
        }
        DeliveryLine line = new DeliveryLine(workOrder, request.lineNo(), request.itemCode(),
            request.orderedQuantity(), request.unitCode(), request.dueDate());
        return ContractMapper.toDto(lineRepository.save(line));
    }

    public WorkOrderDto issue(String code) {
        WorkOrder workOrder = findWorkOrder(code);
        workOrder.issue(lineRepository.countByWorkOrderId(workOrder.getId()));
        return withLineCount(workOrderRepository.save(workOrder));
    }

    public WorkOrderDto cancel(String code) {
        WorkOrder workOrder = findWorkOrder(code);
        workOrder.cancel();
        return withLineCount(workOrderRepository.save(workOrder));
    }

    public DeliveryLineDto extendLine(String code, int lineNo, ExtendRequest request) {
        DeliveryLine line = findLine(code, lineNo);
        line.extendTo(request.newDueDate());
        return ContractMapper.toDto(lineRepository.save(line));
    }

    // --- Goods receipt -------------------------------------------------------

    @Transactional(readOnly = true)
    public List<GoodsReceiptDto> listReceipts(String code, int lineNo) {
        DeliveryLine line = findLine(code, lineNo);
        return receiptRepository.findByDeliveryLineIdOrderByReceivedAt(line.getId()).stream()
            .map(ContractMapper::toDto).toList();
    }

    /**
     * Records goods arriving. Over-delivery is refused by the line itself.
     *
     * <p>Once every line is fully delivered the work order completes on its own — an order
     * is finished when the goods are all in, not when someone remembers to say so.
     */
    public DeliveryLineDto receive(String code, int lineNo, ReceiveRequest request) {
        WorkOrder workOrder = findWorkOrder(code);
        if (!workOrder.acceptsReceipts()) {
            throw new IllegalStateException(
                "Work order " + code + " is not receiving goods (status " + workOrder.getStatus() + ")");
        }
        DeliveryLine line = findLine(code, lineNo);
        boolean late = LocalDate.now().isAfter(line.getDueDate());

        line.receive(request.quantity());
        lineRepository.save(line);
        receiptRepository.save(new GoodsReceipt(line, request.quantity(), request.receivedBy(),
            late, request.remarks()));

        workOrder.markInProgress();
        if (allLinesDelivered(workOrder.getId())) {
            workOrder.complete();
        }
        workOrderRepository.save(workOrder);

        return ContractMapper.toDto(line);
    }

    private boolean allLinesDelivered(Long workOrderId) {
        return lineRepository.findByWorkOrderIdOrderByLineNo(workOrderId).stream()
            .allMatch(DeliveryLine::isFullyDelivered);
    }

    private WorkOrder findWorkOrder(String code) {
        return workOrderRepository.findByCode(code)
            .orElseThrow(() -> new NotFoundException("Work order not found: " + code));
    }

    private DeliveryLine findLine(String code, int lineNo) {
        WorkOrder workOrder = findWorkOrder(code);
        return lineRepository.findByWorkOrderIdAndLineNo(workOrder.getId(), lineNo)
            .orElseThrow(() -> new NotFoundException(
                "Line " + lineNo + " not found on work order " + code));
    }

    private WorkOrderDto withLineCount(WorkOrder workOrder) {
        return ContractMapper.toDto(workOrder, lineRepository.countByWorkOrderId(workOrder.getId()));
    }
}
