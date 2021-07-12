package com.kkl.kklplus.b2b.viomi.entity;

import com.kkl.kklplus.entity.b2b.common.B2BBase;
import lombok.Data;

/**
 * describe:
 *
 * @author chenxj
 * @date 2020/09/22
 */
@Data
public class VioMiOrderReminder extends B2BBase<VioMiOrderReminder> {

    private String orderNumber;

    private String operator;

    private Long kklOrderId;
}
