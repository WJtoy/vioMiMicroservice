package com.kkl.kklplus.b2b.viomi.http.response;

import com.google.gson.annotations.SerializedName;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.io.Serializable;

@ToString
public class ResponseBody<T> implements Serializable {

    private static final long serialVersionUID = 1L;

    public enum ErrorCode {

        SUCCESS(0, "成功"),

        REQUEST_PARAMETER_FORMAT_ERROR(90000000, "请求参数格式不正确"),
        JSON_PARSE_FAILURE(90000100, "JSON解析失败"),
        DATA_PARSE_FAILURE(90000200, "数据解析失败"),

        REQUEST_INVOCATION_FAILURE(90000300, "请求调用失败"),
        HTTP_STATUS_CODE_ERROR(90000400, "HTTP状态码不在[200..300)范围内"),
        HTTP_RESPONSE_BODY_ERROR(90000500, "没有返回有效的HTTP响应正文");

        public int code;
        public String msg;

        private ErrorCode(int code, String msg) {
            this.code = code;
            this.msg = msg;
        }

    }

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
    private String originalJson;

    @Getter
    @Setter
    private Exception exception;

    @Getter
    @Setter
    private T data;

    public ResponseBody() {
    }

    public ResponseBody(ErrorCode errorCode) {
        this(errorCode, null);
    }

    public ResponseBody(ErrorCode errorCode, Exception exception) {
        this.errorCode = errorCode.code;
        this.errorMsg = errorCode.msg;
        this.exception = exception;
    }

    public String getErrorDetailMsg() {
        if (this.exception != null) {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append(exception.getClass().getName());
            if (exception.getMessage() != null && !exception.getMessage().isEmpty()) {
                stringBuilder.append("：").append(exception.getLocalizedMessage());
            }
            return stringBuilder.toString();
        } else {
            return "";
        }
    }
}
