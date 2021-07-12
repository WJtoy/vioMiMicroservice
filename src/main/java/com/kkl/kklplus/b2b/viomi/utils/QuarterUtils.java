package com.kkl.kklplus.b2b.viomi.utils;

import java.util.Calendar;
import java.util.Date;

public class QuarterUtils {

    /**
     * 根据日期来获取分片标识
     * @param date
     * @return
     */
    public static String getQuarter(Date date) {
        if (date != null) {
            Calendar calendar = Calendar.getInstance();
            calendar.setTime(date);
            Integer year = calendar.get(Calendar.YEAR);
            Integer season = getSeason(date);
            return String.format("%s%s", year, season);
        }
        else {
            return null;
        }
    }

    public static String getQuarter(Long timestamp) {
        if (timestamp != null && timestamp > 0) {
            return getQuarter(new Date(timestamp));
        }
        else {
            return null;
        }
    }

    /**
     * 1 第一季度 2 第二季度 3 第三季度 4 第四季度
     *
     * @param date
     * @return
     */
    public static int getSeason(Date date) {

        int season = 0;

        Calendar calendar = Calendar.getInstance();
        calendar.setTime(date);
        int month = calendar.get(Calendar.MONTH);
        switch (month) {
            case Calendar.JANUARY:
            case Calendar.FEBRUARY:
            case Calendar.MARCH:
                season = 1;
                break;
            case Calendar.APRIL:
            case Calendar.MAY:
            case Calendar.JUNE:
                season = 2;
                break;
            case Calendar.JULY:
            case Calendar.AUGUST:
            case Calendar.SEPTEMBER:
                season = 3;
                break;
            case Calendar.OCTOBER:
            case Calendar.NOVEMBER:
            case Calendar.DECEMBER:
                season = 4;
                break;
            default:
                break;
        }
        return season;
    }
}
