package com.kkl.kklplus.b2b.viomi.entity;

import com.kkl.kklplus.b2b.viomi.entity.request.VioMiKtProduct;
import com.kkl.kklplus.b2b.viomi.entity.request.VioMiProduct;
import com.kkl.kklplus.entity.b2b.common.B2BBase;
import lombok.Data;

import java.util.List;

@Data
public class VioMiOrderInfo extends B2BBase<VioMiOrderInfo>{

    private Long kklOrderId;
    private String kklOrderNo;

    private String orderNumber;
    private String operator;

    private String contacts;
    private String contactsPhone;
    private String address;
    private String addressDetail;
    private String source;
    private String paymentObject;
    private Long expectedServiceTime;
    private String type;
    private String subType;
    private String complainOrderNumber;
    private String complainNetworkNumber;
    private String uniqueid;
    //产品
    private String productName;
    private String productModel;
    private String productType;
    private String productBigType;
    private String product69Code;
    private String miSn;
    private String miPurchaseChannel;
    private String miOrderNumber;
    private Long miPurchaseTime;
    private String innerGuarantee;
    //空调内机
    private List<VioMiKtProduct> ktProduct;
    private String ktProductJson;
    private Integer orderStatus;
    private String viomiStatus;
    private String viomiSubStatus;
    private String sendCustomerCourierCompany;
    private String sendCustomerLogisticsNumber;
    private Integer apiExceptionStatus;
    private Long firstExceptionId;

}
