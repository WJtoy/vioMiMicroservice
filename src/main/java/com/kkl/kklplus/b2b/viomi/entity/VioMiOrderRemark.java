package com.kkl.kklplus.b2b.viomi.entity;

import com.kkl.kklplus.entity.b2b.common.B2BBase;
import lombok.Data;

/**
 *备注
 * @author chenxj
 * @date 2020/09/21
 */
@Data
public class VioMiOrderRemark extends B2BBase<VioMiOrderRemark> {

    private Long kklOrderId;

    private String orderNumber;

    private String operator;
    /**
     * 数据类型：0推送1接收
     */
    private Integer type = 0;
}
