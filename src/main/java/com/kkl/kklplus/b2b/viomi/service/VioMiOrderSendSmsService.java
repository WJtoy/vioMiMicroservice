package com.kkl.kklplus.b2b.viomi.service;

import com.kkl.kklplus.b2b.viomi.http.command.OperationCommand;
import com.kkl.kklplus.b2b.viomi.http.request.SendSmsParam;
import com.kkl.kklplus.b2b.viomi.http.request.SmsParam;
import com.kkl.kklplus.b2b.viomi.http.response.ResponseBody;
import com.kkl.kklplus.b2b.viomi.http.response.ResponseData;
import com.kkl.kklplus.b2b.viomi.http.utils.OkHttpUtils;
import com.kkl.kklplus.b2b.viomi.utils.GsonUtils;
import com.kkl.kklplus.b2b.viomi.utils.QuarterUtils;
import com.kkl.kklplus.common.exception.MSErrorCode;
import com.kkl.kklplus.common.response.MSResponse;
import com.kkl.kklplus.entity.b2b.common.B2BProcessFlag;
import com.kkl.kklplus.entity.b2bcenter.rpt.B2BOrderProcesslog;
import com.kkl.kklplus.entity.viomi.sd.VioMiOrderSendSms;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * @author wln
 */
@Service
@Slf4j
public class VioMiOrderSendSmsService {

    @Autowired
    private B2BProcesslogService b2BProcesslogService;

    @Autowired
    private SysLogService sysLogService;

    public MSResponse sendSmsToVioMi(VioMiOrderSendSms vioMiOrderSendSms,String smsCode) {
        vioMiOrderSendSms.setCreateDt(System.currentTimeMillis());
        MSResponse msResponse = new MSResponse();
        String orderNumber = vioMiOrderSendSms.getOrderNumber();
        if (StringUtils.isBlank(orderNumber)) {
            msResponse.setErrorCode(MSErrorCode.FAILURE);
            msResponse.setMsg("工单编号不能为空");
            return msResponse;
        }
        if (StringUtils.isBlank(vioMiOrderSendSms.getPhone())) {
            msResponse.setErrorCode(MSErrorCode.FAILURE);
            msResponse.setMsg("电话号码不能为空");
            return msResponse;
        }
        // 生成发送Log
        B2BOrderProcesslog b2BProcesslog = new B2BOrderProcesslog();
        b2BProcesslog.setInterfaceName(OperationCommand.OperationCode.ORDER_SENDSMS.apiUrl);
        b2BProcesslog.setProcessTime(0);
        b2BProcesslog.setCreateById(vioMiOrderSendSms.getCreateById());
        b2BProcesslog.setUpdateById(vioMiOrderSendSms.getCreateById());
        b2BProcesslog.setCreateDt(vioMiOrderSendSms.getCreateDt());
        b2BProcesslog.preInsert();
        b2BProcesslog.setProcessFlag(0);
        b2BProcesslog.setQuarter(QuarterUtils.getQuarter(b2BProcesslog.getCreateDt()));

        //云米短信对象
        SendSmsParam sendSmsParam = new SendSmsParam();
        sendSmsParam.setPhone(vioMiOrderSendSms.getPhone());
        sendSmsParam.setSmsCode(smsCode);
        SmsParam smsParam = new SmsParam();
        smsParam.setOrderNumber(vioMiOrderSendSms.getOrderNumber());
        if(smsCode.equals("1003")){
            smsParam.setReason(vioMiOrderSendSms.getReason());
        }
        sendSmsParam.setSmsParam(smsParam);
        OperationCommand command = OperationCommand.newInstance(OperationCommand.OperationCode.ORDER_SENDSMS, sendSmsParam);
        ResponseBody<ResponseData> resBody = OkHttpUtils.postSyncGenericNew(command, ResponseData.class);

        // 生成要发送的json对象
        String infoJson = GsonUtils.getInstance().toUnderscoresJson(sendSmsParam);
        b2BProcesslog.setInfoJson(infoJson);
        try {
            b2BProcesslogService.insert(b2BProcesslog);
            b2BProcesslog.setResultJson(resBody.getOriginalJson());
            ResponseData data = resBody.getData();
            if (resBody.getErrorCode() == ResponseBody.ErrorCode.SUCCESS.code && data != null) {
                // 0 -- 成功
                if (data.getErrorCode().equals(0)) {
                    b2BProcesslog.setProcessFlag(B2BProcessFlag.PROCESS_FLAG_SUCESS.value);
                    b2BProcesslogService.updateProcessFlag(b2BProcesslog);
                    msResponse.setMsg(data.getErrorMsg());
                    return msResponse;
                }
            }
            // 1：签名无 效；2：参数有误	3.请求 失败
            Integer errorCode = data != null ? data.getErrorCode() : resBody.getErrorCode();
            String errorStr = data != null ? data.getErrorMsg() : resBody.getErrorMsg();
            errorStr = StringUtils.left(errorStr, 255);
            b2BProcesslog.setProcessFlag(B2BProcessFlag.PROCESS_FLAG_FAILURE.value);
            b2BProcesslog.setProcessComment(errorStr);
            msResponse.setThirdPartyErrorCode(new MSErrorCode(errorCode, errorStr));
            msResponse.setCode(errorCode);
            msResponse.setMsg(errorStr);
            b2BProcesslogService.updateProcessFlag(b2BProcesslog);
            return msResponse;
        } catch (Exception e) {
            String errorMsg = StringUtils.left(e.getMessage(), 255);
            msResponse.setErrorCode(MSErrorCode.FAILURE);
            msResponse.setMsg(errorMsg);
            b2BProcesslog.setProcessFlag(B2BProcessFlag.PROCESS_FLAG_FAILURE.value);
            b2BProcesslog.setProcessComment(errorMsg);
            b2BProcesslogService.updateProcessFlag(b2BProcesslog);
            log.error("errorStr:{}", e.getMessage());
            sysLogService.insert(1L, infoJson, e.getMessage(), "调用云米发送短信接口失败", OperationCommand.OperationCode.ORDER_SENDSMS.apiUrl, "POST");
            return msResponse;
        }
    }
}
