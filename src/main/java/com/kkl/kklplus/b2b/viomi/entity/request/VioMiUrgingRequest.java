package com.kkl.kklplus.b2b.viomi.entity.request;

import lombok.Data;

import java.io.Serializable;

@Data
public class VioMiUrgingRequest implements Serializable {

    private String orderNumber;

    private VioMiHandleRequest handle;

}
