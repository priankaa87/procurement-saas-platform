package com.procurementsaas.contract.service;

import com.procurementsaas.common.web.NotFoundException;
import com.procurementsaas.contract.domain.Award;
import com.procurementsaas.contract.domain.AwardStatus;
import com.procurementsaas.contract.dto.Dtos.AwardDto;
import com.procurementsaas.contract.dto.Dtos.DeclineRequest;
import com.procurementsaas.contract.dto.Dtos.IssueAwardRequest;
import com.procurementsaas.contract.repo.AwardRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/** Issuing notices of award and recording the supplier's answer. */
@Service
@Transactional
public class AwardService {

    private static final Logger log = LoggerFactory.getLogger(AwardService.class);

    private final AwardRepository awardRepository;
    private final int acceptanceWindowDays;

    public AwardService(AwardRepository awardRepository,
                        @Value("${award.acceptance-window-days:14}") int acceptanceWindowDays) {
        this.awardRepository = awardRepository;
        this.acceptanceWindowDays = acceptanceWindowDays;
    }

    @Transactional(readOnly = true)
    public List<AwardDto> list(String status) {
        List<Award> awards = (status == null)
            ? awardRepository.findAll()
            : awardRepository.findByStatus(parseStatus(status));
        return awards.stream().map(ContractMapper::toDto).toList();
    }

    @Transactional(readOnly = true)
    public AwardDto getByTender(String tenderCode) {
        return ContractMapper.toDto(findAward(tenderCode));
    }

    public AwardDto issue(IssueAwardRequest request) {
        LocalDate respondBy = request.respondBy() != null
            ? request.respondBy()
            : LocalDate.now().plusDays(acceptanceWindowDays);
        return ContractMapper.toDto(
            create(request.tenderCode(), request.tenderTitle(), request.supplierCode(),
                request.amount(), request.currencyCode(), respondBy));
    }

    /**
     * Raises an award from a tender outcome, ignoring a tender already awarded here.
     *
     * <p>Used by the event listener: Kafka redelivery is routine, and the same tender
     * arriving twice must not issue two notices to the supplier.
     */
    public void createFromTenderIfAbsent(String tenderCode, String tenderTitle,
                                         String supplierCode, BigDecimal amount,
                                         String currencyCode) {
        if (awardRepository.existsByTenderCode(tenderCode)) {
            log.debug("Award already exists for tender {}; ignoring replay", tenderCode);
            return;
        }
        create(tenderCode, tenderTitle, supplierCode, amount, currencyCode,
            LocalDate.now().plusDays(acceptanceWindowDays));
        log.info("Raised award for tender {} to {}", tenderCode, supplierCode);
    }

    private Award create(String tenderCode, String tenderTitle, String supplierCode,
                         BigDecimal amount, String currencyCode, LocalDate respondBy) {
        if (awardRepository.existsByTenderCode(tenderCode)) {
            throw new IllegalArgumentException("Tender is already awarded: " + tenderCode);
        }
        return awardRepository.save(
            new Award(tenderCode, tenderTitle, supplierCode, amount, currencyCode, respondBy));
    }

    public AwardDto accept(String tenderCode) {
        Award award = findAward(tenderCode);
        award.accept();
        return ContractMapper.toDto(awardRepository.save(award));
    }

    public AwardDto decline(String tenderCode, DeclineRequest request) {
        Award award = findAward(tenderCode);
        award.decline(request.reason());
        return ContractMapper.toDto(awardRepository.save(award));
    }

    public AwardDto cancel(String tenderCode) {
        Award award = findAward(tenderCode);
        award.cancel();
        return ContractMapper.toDto(awardRepository.save(award));
    }

    /**
     * Lapses awards whose acceptance window has closed.
     *
     * <p>Exposed as an operation rather than left implicit, so an award's status is a fact
     * in the database rather than something each reader has to infer from a date.
     */
    public int expireLapsedAwards() {
        List<Award> lapsed = awardRepository.findByStatusAndRespondByBefore(
            AwardStatus.PENDING_ACCEPTANCE, LocalDate.now());
        lapsed.forEach(award -> {
            award.expire();
            awardRepository.save(award);
        });
        if (!lapsed.isEmpty()) {
            log.info("Expired {} lapsed award(s)", lapsed.size());
        }
        return lapsed.size();
    }

    Award findAward(String tenderCode) {
        return awardRepository.findByTenderCode(tenderCode)
            .orElseThrow(() -> new NotFoundException("Award not found for tender: " + tenderCode));
    }

    private static AwardStatus parseStatus(String status) {
        try {
            return AwardStatus.valueOf(status.toUpperCase());
        } catch (IllegalArgumentException ex) {
            throw new IllegalArgumentException("Unknown award status: " + status);
        }
    }
}
