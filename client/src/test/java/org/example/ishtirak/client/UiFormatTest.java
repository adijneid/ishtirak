package org.example.ishtirak.client;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.YearMonth;

import static org.junit.jupiter.api.Assertions.*;

class UiFormatTest {
    @Test
    void formatsLebanesePounds() {
        assertEquals("18,265,832 LBP", Ui.lbp(new BigDecimal("18265832")));
        assertEquals("130.5", Ui.kwh(new BigDecimal("130.5")));
        assertEquals("August 2026", Ui.month(YearMonth.of(2026, 8)));
    }

    @Test
    void parsesAmountsTypedWithSeparators() {
        assertEquals(0, new BigDecimal("1500000").compareTo(Ui.decimal("1,500,000")));
        assertNull(Ui.decimal("  "));
        assertThrows(NumberFormatException.class, () -> Ui.decimal("abc"));
    }
}
