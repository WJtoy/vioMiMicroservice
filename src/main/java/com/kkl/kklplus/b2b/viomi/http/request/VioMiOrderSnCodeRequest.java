package com.kkl.kklplus.b2b.viomi.http.request;

import com.kkl.kklplus.entity.b2b.common.B2BBase;
import lombok.Data;

/**
 * 产品SN码验证
 * @author wln
 */
@Data
public class VioMiOrderSnCodeRequest extends RequestParam {
    /**
     * CTC系统的工单编号
     */
    private String orderNumber;
    /**
     * 产品SN码
     */
    private String snCode;

}
