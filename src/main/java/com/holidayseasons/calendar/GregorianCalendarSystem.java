package com.holidayseasons.calendar;

import java.time.LocalDate;
import java.time.YearMonth;

/** The real-world proleptic Gregorian calendar backed by {@link java.time}. Epoch day 0 = 1970-01-01. */
public final class GregorianCalendarSystem implements CalendarSystem {
    public static final GregorianCalendarSystem INSTANCE = new GregorianCalendarSystem();

    private GregorianCalendarSystem() {
    }

    @Override
    public String signature() {
        return "gregorian";
    }

    @Override
    public int monthLength(int year, int month) {
        return YearMonth.of(year, month).lengthOfMonth();
    }

    @Override
    public int yearLength(int year) {
        return java.time.Year.isLeap(year) ? 366 : 365;
    }

    @Override
    public long toEpochDay(CalendarDate date) {
        return LocalDate.of(date.year(), date.month(), date.day()).toEpochDay();
    }

    @Override
    public CalendarDate fromEpochDay(long epochDay) {
        LocalDate d = LocalDate.ofEpochDay(epochDay);
        return new CalendarDate(d.getYear(), d.getMonthValue(), d.getDayOfMonth());
    }

    @Override
    public int dayOfWeek(long epochDay) {
        return LocalDate.ofEpochDay(epochDay).getDayOfWeek().getValue();
    }

    @Override
    public boolean isGregorian() {
        return true;
    }

    @Override
    public int referenceGregorianYear(int year) {
        return year;
    }

    public static LocalDate toLocalDate(CalendarDate date) {
        return LocalDate.of(date.year(), date.month(), date.day());
    }

    public static CalendarDate fromLocalDate(LocalDate d) {
        return new CalendarDate(d.getYear(), d.getMonthValue(), d.getDayOfMonth());
    }
}
