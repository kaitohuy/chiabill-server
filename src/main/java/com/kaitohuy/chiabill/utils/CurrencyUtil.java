package com.kaitohuy.chiabill.utils;

import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;

public class CurrencyUtil {
    /**
     * Format BigDecimal to String with thousands separators (comma) and no decimals.
     * Example: 1000000.0 -> 1,000,000
     */
    public static String format(BigDecimal amount) {
        if (amount == null) return "0";
        DecimalFormatSymbols symbols = new DecimalFormatSymbols(Locale.US);
        symbols.setGroupingSeparator(',');
        DecimalFormat df = new DecimalFormat("#,###", symbols);
        return df.format(amount);
    }
}
