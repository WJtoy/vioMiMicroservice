package com.kkl.kklplus.b2b.viomi.entity.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 *
 * @author chenxj
 * @date 2020/09/18
 */
@NoArgsConstructor
@AllArgsConstructor
@Data
public class VioMiResponse<T> implements Serializable {

    private int res = 0;

    private String msg = "";

    private T data;

    public VioMiResponse(int res,String msg){
        this.res = res;
        this.msg = msg;
    }
}
