package com.kkl.kklplus.b2b.viomi.entity.request;

import lombok.Data;

import java.io.Serializable;

@Data
public class VioMiOrderRequest implements Serializable{
    private String contacts;
    private String contactsPhone;
    private String address;
    private String addressDetail;
    private String source;
    private String paymentObject;
    private String expectedServiceTime;
    private String type;
    private String subType;
    private String complainOrderNumber;
    private String complainNetworkNumber;
    private String uniqueid;
    private String sendCustomerCourierCompany;
    private String sendCustomerLogisticsNumber;
    private String remarks;
}
