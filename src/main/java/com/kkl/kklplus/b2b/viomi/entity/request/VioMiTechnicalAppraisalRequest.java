package com.kkl.kklplus.b2b.viomi.entity.request;

import lombok.Data;

/**
 * describe:
 *
 * @author chenxj
 * @date 2020/09/18
 */
@Data
public class VioMiTechnicalAppraisalRequest {
    private String orderNumber;
    private VioMiHandleRequest handle;
}
