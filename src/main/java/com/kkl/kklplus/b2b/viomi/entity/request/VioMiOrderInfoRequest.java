package com.kkl.kklplus.b2b.viomi.entity.request;

import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * describe:
 *
 * @author chenxj
 * @date 2020/09/18
 */
@Data
public class VioMiOrderInfoRequest implements Serializable {

    private String orderNumber;
    private VioMiHandleRequest handle;
    private VioMiOrderRequest order;
    private VioMiProduct product;
    private List<VioMiKtProduct> ktProduct;

}
