package com.kkl.kklplus.b2b.viomi.http.request;

import lombok.Data;

/**
 * describe:
 *
 * @author chenxj
 * @date 2020/09/21
 */
@Data
public class OrderCancelRequestParam extends RequestParam{

    private String orderNumber;

    private HandleParam handle;
}
