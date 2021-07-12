package com.kkl.kklplus.b2b.viomi.http.request;

import lombok.Data;

@Data
public class MiSnParam {
    /**
     * 云米sn
     */
    private String miSn;
    /**
     * 服务措施
     */
    private String serviceMeasures;
    /**
     * 是否故障
     */
    private String isFault;
    /**
     * 故障类型
     */
    private String faultType;
}
