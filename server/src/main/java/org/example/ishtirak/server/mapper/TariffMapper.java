package org.example.ishtirak.server.mapper;

import org.example.ishtirak.common.dto.TariffRequest;
import org.example.ishtirak.common.dto.TariffResponse;
import org.example.ishtirak.server.model.TariffMonth;
import org.example.ishtirak.server.service.BillingCalculator;
import org.springframework.stereotype.Component;

import java.time.YearMonth;

@Component
public class TariffMapper {
    public TariffMonth toEntity(TariffRequest request) {
        TariffMonth tariff = new TariffMonth();
        updateEntity(tariff, request);
        return tariff;
    }

    public void updateEntity(TariffMonth tariff, TariffRequest request) {
        tariff.setBillingMonth(request.billingMonth().atDay(1));
        tariff.setPricePerKwh(request.pricePerKwh());
        tariff.setFee5a(request.fee5a());
        tariff.setFee10a(request.fee10a());
        tariff.setFeePerExtra5a(request.feePerExtra5a());
        tariff.setRuralUpliftPct(request.ruralUpliftPct());
    }

    public TariffResponse toResponse(TariffMonth tariff, boolean locked) {
        return new TariffResponse(
                tariff.getId(),
                YearMonth.from(tariff.getBillingMonth()),
                tariff.getPricePerKwh(),
                tariff.getFee5a(),
                tariff.getFee10a(),
                tariff.getFeePerExtra5a(),
                tariff.getRuralUpliftPct(),
                locked
        );
    }

    public BillingCalculator.Rates toRates(TariffMonth tariff) {
        return new BillingCalculator.Rates(
                tariff.getPricePerKwh(),
                tariff.getFee5a(),
                tariff.getFee10a(),
                tariff.getFeePerExtra5a(),
                tariff.getRuralUpliftPct()
        );
    }
}
