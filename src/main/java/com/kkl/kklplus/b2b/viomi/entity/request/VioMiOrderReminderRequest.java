package com.kkl.kklplus.b2b.viomi.entity.request;

import lombok.Data;

import java.io.Serializable;

/**
 * 催单接收
 * @author chenxj
 * @date 2020/09/22
 */
@Data
public class VioMiOrderReminderRequest implements Serializable {
    private String orderNumber;
    private VioMiHandleRequest handle;
}
