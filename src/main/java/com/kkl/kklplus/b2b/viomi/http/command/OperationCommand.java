package com.kkl.kklplus.b2b.viomi.http.command;

import com.kkl.kklplus.b2b.viomi.http.request.*;
import com.kkl.kklplus.b2b.viomi.http.request.OrderRemarkRequestParam;
import com.kkl.kklplus.b2b.viomi.http.request.RequestParam;


import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@ToString
public class OperationCommand {

    public enum OperationCode {



        ORDER_HANDLE(1005, "工单处理", "handle_order", OrderHandleRequestParam.class),
        PRODUCT_PARTS(1010,"获取产品配件","product_bom", ProductPartsRequestParam.class),
        FAULT_TYPE(1011,"获取故障类别","get_fault_type",RequestParam.class),
        OTHER_CANCEL_ORDER(1004, "第三方工单取消", "cancel_order", OrderCancelRequestParam.class),
        OTHER_REMARK_ORDER(1014, "第三方工单备注", "remark_order", OrderRemarkRequestParam.class),
        ORDER_SENDSMS(1007,"发送工单验证短信","send_sms", SendSmsParam.class),
        ORDER_GRADESN(1009,"产品SN码验证","grade_sn", VioMiOrderSnCodeRequest.class);


        public int code;
        public String name;
        public String apiUrl;
        public Class reqBodyClass;

        private OperationCode(int code, String name, String apiUrl, Class reqBodyClass) {
            this.code = code;
            this.name = name;
            this.apiUrl = apiUrl;
            this.reqBodyClass = reqBodyClass;
        }
    }

    @Getter
    @Setter
    private OperationCode opCode;

    @Getter
    @Setter
    private RequestParam reqBody;

    public static OperationCommand newInstance(OperationCode opCode, RequestParam reqBody) {
        OperationCommand command = new OperationCommand();
        command.opCode = opCode;
        command.reqBody = reqBody;
        return command;
    }
}
