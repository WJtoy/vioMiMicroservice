package com.kkl.kklplus.b2b.viomi.entity;

import lombok.Data;

/**
 * describe:
 *
 * @author chenxj
 * @date 2020/09/18
 */
@Data
public class VioMiKtServiceMessage {
    private String miSn;
    private String serviceMeasures;
    private String isFault;
    private String faultType;
    private String workerErrorDesc;
    private String checkValidateResult;
    private String checkValidateDetail;
    private String packValidate;
    private String packValidateDetail;
}
