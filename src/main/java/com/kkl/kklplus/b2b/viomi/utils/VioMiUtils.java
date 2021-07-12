package com.kkl.kklplus.b2b.viomi.utils;

import cn.hutool.core.date.DateUtil;
import cn.hutool.core.exceptions.ExceptionUtil;
import cn.hutool.core.util.StrUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Date;

public class VioMiUtils {
    public static final int SUCCESS_CODE = 0;
    public static final int FAILURE_CODE = 1;

    private static final Logger log = LoggerFactory.getLogger(VioMiUtils.class);

    public static Date parseDate(String dateStr, String format) {
        if (StrUtil.isEmpty(dateStr) || StrUtil.isEmpty(format)) {
            return null;
        }
        Date date = null;
        try {
            date = DateUtil.parse(dateStr, format);
        } catch (Exception e) {
            log.error("[VioMiUtils.parseDate] {}", ExceptionUtil.getMessage(e));
        }
        return date;
    }

    public static Long parseTimestamp(String dateStr, String format) {
        Date date = parseDate(dateStr, format);
        return date == null ? 0 : date.getTime();
    }
}
