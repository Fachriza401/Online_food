package com.foodordering.service;

import java.text.NumberFormat;
import java.util.Locale;

import org.springframework.stereotype.Component;

@Component("currencyFmt")
public class CurrencyFormatter {

    private final NumberFormat format = NumberFormat.getNumberInstance(new Locale("id", "ID"));

    public String format(long value) {
        return format.format(value);
    }
}