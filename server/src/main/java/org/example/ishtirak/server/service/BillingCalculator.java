package org.example.ishtirak.server.service;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * The ministry billing formula (proposal, section 3) in ONE pure class.
 *
 * consumption = current reading - previous reading (or initial_reading for the first month)
 * amount_due  = consumption x price_per_kwh x (1 + uplift/100 if the zone is rural) + fixedFee
 * fixedFee(5 A) = fee_5a, fixedFee(10 A) = fee_10a,
 * fixedFee(A > 10) = fee_10a + ((A - 10) / 5) x fee_per_extra_5a
 *
 * No database, no Spring: it only does arithmetic, so it is unit-tested in isolation.
 * Amounts are LBP and rounded once, at the end, to whole pounds (HALF_UP).
 */
public class BillingCalculator {

    private static final BigDecimal HUNDRED = new BigDecimal("100");

    /** The tariff values the formula needs (taken from tariff_month). */
    public record Rates(BigDecimal pricePerKwh,
                        BigDecimal fee5a,
                        BigDecimal fee10a,
                        BigDecimal feePerExtra5a,
                        BigDecimal ruralUpliftPct) {
    }

    /** Full breakdown of one bill. */
    public record Result(BigDecimal consumptionKwh,
                         BigDecimal energyCharge,
                         BigDecimal fixedFee,
                         BigDecimal amountDue) {
    }

    public BigDecimal consumption(BigDecimal previousReading, BigDecimal currentReading) {
        if (previousReading == null || currentReading == null) {
            throw new IllegalArgumentException("Both readings are required");
        }
        if (currentReading.compareTo(previousReading) < 0) {
            throw new IllegalArgumentException("Current reading is below the previous reading");
        }
        return currentReading.subtract(previousReading);
    }

    public BigDecimal fixedFee(int amperage, Rates rates) {
        if (amperage < 5 || amperage > 60 || amperage % 5 != 0) {
            throw new IllegalArgumentException("Amperage must be a multiple of 5 between 5 and 60: " + amperage);
        }
        if (amperage == 5) {
            return rates.fee5a();
        }
        if (amperage == 10) {
            return rates.fee10a();
        }
        int extraSteps = (amperage - 10) / 5;
        return rates.fee10a().add(rates.feePerExtra5a().multiply(BigDecimal.valueOf(extraSteps)));
    }

    public BigDecimal energyCharge(BigDecimal consumptionKwh, boolean rural, Rates rates) {
        BigDecimal charge = consumptionKwh.multiply(rates.pricePerKwh());
        if (rural) {
            BigDecimal factor = BigDecimal.ONE.add(rates.ruralUpliftPct().divide(HUNDRED));
            charge = charge.multiply(factor);
        }
        return charge;
    }

    public Result calculate(BigDecimal previousReading, BigDecimal currentReading,
                            int amperage, boolean rural, Rates rates) {
        BigDecimal consumption = consumption(previousReading, currentReading);
        BigDecimal energy = energyCharge(consumption, rural, rates);
        BigDecimal fee = fixedFee(amperage, rates);
        BigDecimal amountDue = energy.add(fee).setScale(0, RoundingMode.HALF_UP);
        return new Result(consumption, energy, fee, amountDue);
    }
}
