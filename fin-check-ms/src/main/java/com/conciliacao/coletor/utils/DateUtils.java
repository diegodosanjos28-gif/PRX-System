package com.conciliacao.coletor.utils;

import java.time.LocalDate;

public class DateUtils {

    public static boolean validPeriod(LocalDate dataInicio, LocalDate dataFim) {
        return dataInicio != null && dataFim != null && !dataInicio.isAfter(dataFim);
    }


}
