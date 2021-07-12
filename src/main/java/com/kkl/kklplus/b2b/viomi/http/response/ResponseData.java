package com.kkl.kklplus.b2b.viomi.http.response;

import com.google.gson.annotations.SerializedName;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.io.Serializable;

@ToString
public class ResponseData implements Serializable {

    private static final long serialVersionUID = 1L;

    @SerializedName("res")
    @Getter
    @Setter
    private Integer errorCode = 0;

    @SerializedName("msg")
    @Getter
    @Setter
    private String errorMsg = "";

    @Getter
    @Setter
    private Object data;

}
