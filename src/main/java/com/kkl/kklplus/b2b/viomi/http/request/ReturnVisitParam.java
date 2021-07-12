package com.kkl.kklplus.b2b.viomi.http.request;

import lombok.Data;

/**
 * 回访
 */
@Data
public class ReturnVisitParam {
    /**
     * 安装费
     */
    private String installationFee;
    /**
     * 维修费
     */
    private String maintenanceCost;
    /**
     * 远程费
     */
    private String remoteFee;
    /**
     * 检测费
     */
    private String detectFee;
    /**
     * 扣罚费
     */
    private String fineFee;
    /**
     * 公里数
     */
    private String mileage;
}
