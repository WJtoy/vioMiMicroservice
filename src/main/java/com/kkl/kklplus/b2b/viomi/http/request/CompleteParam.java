package com.kkl.kklplus.b2b.viomi.http.request;


import lombok.Data;

import java.util.List;

/**
 *
 */
@Data
public class CompleteParam extends RequestParam{
    /**
     * 服务措施(仅维修用)
     */
    private String serviceMeasures;
    /**
     * 故障类型(仅维修用)
     */
    private String faultType;

    /**
     * 购买时间
     */
    private String miPurchaseTime;
    /**
     * 产品SN码
     */
    private String miSn;
    /**
     * 空调内机信息
     */
    private List<MiSnParam> ktSn;
    /**
     * 鉴定图片
     */
    private List<String> attachment;
    /**
     * 是否故障
     */
    private String isFault;
    /**
     * 故障记录
     */
    private String workerErrorDesc;
    /**
     * 检验鉴定结果
     */
    private String checkValidateResult;
    /**
     * 检验鉴定结果详情
     */
    private String checkValidateDetail;
    /**
     * 包装鉴定
     */
    private String packValidate;
    /**
     * 包装鉴定详情
     */
    private String packValidateDetail;
    /**
     * 和客户预约上门的时间
     */
    private String timeOfAppointment;
    /**
     * 网点信息
     */
    private String networkInfo;
    /**
     * 师傅姓名
     */
    private String engineerName;
    /**
     * 师傅电话
     */
    private String engineerPhone;
    /**
     * 是否邀好评成功
     */
    private String praiseSuccess;
    /**
     * 客户好评截图
     */
    private List<String> praiseScreenshot;

    private String oldSn;
    private String backProductLogisticsCompany;
    private String backProductLogisticsNumber;
    private List<String> backProductPhone;
}
