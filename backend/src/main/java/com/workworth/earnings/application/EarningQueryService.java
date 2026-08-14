package com.workworth.earnings.application;

import com.workworth.earnings.exception.EarningNotFoundException;
import com.workworth.earnings.persistence.EarningCorrection;
import com.workworth.earnings.persistence.EarningCorrectionRepository;
import com.workworth.earnings.persistence.WorkdayEarning;
import com.workworth.earnings.persistence.WorkdayEarningRepository;
import com.workworth.identity.application.CurrentUserProvider;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

@Service
public class EarningQueryService {

    private final WorkdayEarningRepository earnings;
    private final EarningCorrectionRepository corrections;
    private final CurrentUserProvider currentUser;

    public EarningQueryService(WorkdayEarningRepository earnings, EarningCorrectionRepository corrections,
                               CurrentUserProvider currentUser) {
        this.earnings = earnings;
        this.corrections = corrections;
        this.currentUser = currentUser;
    }

    public EffectiveEarning byDate(LocalDate date) {
        WorkdayEarning earning = earnings.findByLocalDateAndWorkdayOwnerId(date, currentUser.currentUser().getId())
            .orElseThrow(() -> new EarningNotFoundException("No earning exists for this workday."));
        return new EffectiveEarning(earning, latestCorrection(earning.getId()));
    }

    public EarningHistoryPage history(int page, int size) {
        var result = earnings.findAllByWorkdayOwnerIdOrderByLocalDateDesc(currentUser.currentUser().getId(), PageRequest.of(page, size));
        Map<Long, EarningCorrection> latestCorrections = latestCorrections(result.getContent());
        List<EffectiveEarning> items = result.getContent().stream()
            .map(earning -> new EffectiveEarning(earning, latestCorrections.get(earning.getId())))
            .toList();
        return new EarningHistoryPage(items, result.getNumber(), result.getSize(), result.getTotalElements(),
            result.getTotalPages(), result.hasNext(), result.hasPrevious());
    }

    public List<EarningCorrection> corrections(LocalDate date) {
        WorkdayEarning earning = earnings.findByLocalDateAndWorkdayOwnerId(date, currentUser.currentUser().getId())
            .orElseThrow(() -> new EarningNotFoundException("No earning exists for this workday."));
        return corrections.findByEarningIdOrderBySequenceDesc(earning.getId());
    }

    private EarningCorrection latestCorrection(Long earningId) {
        return corrections.findByEarningIdOrderBySequenceDesc(earningId).stream().findFirst().orElse(null);
    }

    private Map<Long, EarningCorrection> latestCorrections(Collection<WorkdayEarning> pageEarnings) {
        List<Long> earningIds = pageEarnings.stream().map(WorkdayEarning::getId).toList();
        if (earningIds.isEmpty()) {
            return Map.of();
        }
        return corrections.findByEarningIdInOrderByEarningIdAscSequenceDesc(earningIds).stream()
            .collect(java.util.stream.Collectors.toMap(
                correction -> correction.getEarning().getId(),
                Function.identity(),
                (first, ignored) -> first));
    }
}
