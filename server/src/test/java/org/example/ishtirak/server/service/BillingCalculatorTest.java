package org.example.ishtirak.server.service;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Pure unit tests of the billing formula, no Spring and no database.
 * Worked examples use the ministry's August 2026 tariff: 48,241 LBP/kWh, 385,000 (5 A), 685,000 (10 A),
 * +300,000 per extra 5 A, +10% in rural areas.
 */
class BillingCalculatorTest {

    private final BillingCalculator calculator = new BillingCalculator();
    private final BillingCalculator.Rates august2026 = new BillingCalculator.Rates(
            bd("48241"), bd("385000"), bd("685000"), bd("300000"), bd("10.0"));

    private static BigDecimal bd(String value) {
        return new BigDecimal(value);
    }

    private BigDecimal amount(String previous, String current, int amperage, boolean rural) {
        return calculator.calculate(bd(previous), bd(current), amperage, rural, august2026).amountDue();
    }

    @Test
    void fiveAmpsNonRural() {
        // 68 x 48,241 = 3,280,388 + 385,000
        assertEquals(bd("3665388"), amount("1262.0", "1330.0", 5, false));
    }

    @Test
    void tenAmpsNonRural() {
        // 100 x 48,241 = 4,824,100 + 685,000
        assertEquals(bd("5509100"), amount("0", "100", 10, false));
    }

    @Test
    void aboveTenAmpsAddsOneFeePerExtraFiveAmps() {
        assertEquals(bd("985000"), calculator.fixedFee(15, august2026));   // the ministry's own 15 A example
        assertEquals(bd("1285000"), calculator.fixedFee(20, august2026));
        assertEquals(bd("685000").add(bd("300000").multiply(bd("10"))), calculator.fixedFee(60, august2026));
        // 230 x 48,241 = 11,095,430 + 985,000
        assertEquals(bd("12080430"), amount("3210", "3440", 15, false));
    }

    @Test
    void ruralUpliftAppliesToEnergyOnlyNotToTheFixedFee() {
        // 100 x 48,241 x 1.10 = 5,306,510 + 685,000
        assertEquals(bd("5991510"), amount("0", "100", 10, true));
        // 320 x 48,241 x 1.10 = 16,980,832 + 1,285,000
        assertEquals(bd("18265832"), amount("2800", "3120", 20, true));
    }

    @Test
    void nonRuralHasNoUplift() {
        assertEquals(bd("4824100"), calculator.energyCharge(bd("100"), false, august2026));
        assertEquals(0, bd("5306510").compareTo(calculator.energyCharge(bd("100"), true, august2026)));
    }

    @Test
    void firstMonthUsesTheInitialReadingAsPrevious() {
        // subscription with initial_reading 850.5 and a first reading of 981.0 -> 130.5 kWh
        BillingCalculator.Result result = calculator.calculate(bd("850.5"), bd("981.0"), 10, false, august2026);
        assertEquals(0, bd("130.5").compareTo(result.consumptionKwh()));
        // 130.5 x 48,241 = 6,295,450.5 -> rounded half up = 6,295,451 ... + 685,000
        assertEquals(bd("6980451"), result.amountDue());
    }

    @Test
    void subsequentMonthUsesThePreviousReading() {
        BillingCalculator.Result result = calculator.calculate(bd("981.0"), bd("1125.5"), 10, false, august2026);
        assertEquals(0, bd("144.5").compareTo(result.consumptionKwh()));
        assertEquals(bd("7655825"), result.amountDue()); // 144.5 x 48,241 = 6,970,824.5 -> 6,970,825 + 685,000
    }

    @Test
    void zeroConsumptionStillPaysTheFixedFee() {
        assertEquals(bd("385000"), amount("500", "500", 5, true));
    }

    @Test
    void roundsOnceAtTheEndHalfUp() {
        // 12.3 x 48,241 x 1.1 = 652,700.73 -> 652,701 + 385,000
        assertEquals(bd("1037701"), amount("0", "12.3", 5, true));
    }

    @Test
    void customUpliftPercentageIsUsed() {
        BillingCalculator.Rates rates = new BillingCalculator.Rates(bd("1000"), bd("1"), bd("1"), bd("1"), bd("25"));
        assertEquals(bd("1251"), calculator.calculate(bd("0"), bd("1"), 10, true, rates).amountDue());
    }

    @Test
    void rejectsReadingBelowPrevious() {
        assertThrows(IllegalArgumentException.class, () -> amount("100", "99.9", 5, false));
    }

    @Test
    void rejectsInvalidAmperage() {
        assertThrows(IllegalArgumentException.class, () -> calculator.fixedFee(12, august2026));
        assertThrows(IllegalArgumentException.class, () -> calculator.fixedFee(0, august2026));
        assertThrows(IllegalArgumentException.class, () -> calculator.fixedFee(65, august2026));
    }
}
