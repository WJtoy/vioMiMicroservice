package com.kkl.kklplus.b2b.viomi.service;

import com.kkl.kklplus.b2b.viomi.http.command.OperationCommand;
import com.kkl.kklplus.b2b.viomi.entity.VioMiOrderSnCode;
import com.kkl.kklplus.b2b.viomi.http.request.VioMiOrderSnCodeRequest;
import com.kkl.kklplus.b2b.viomi.http.response.ResponseBody;
import com.kkl.kklplus.b2b.viomi.http.response.ResponseData;
import com.kkl.kklplus.b2b.viomi.http.utils.OkHttpUtils;
import com.kkl.kklplus.b2b.viomi.utils.GsonUtils;
import com.kkl.kklplus.b2b.viomi.utils.QuarterUtils;
import com.kkl.kklplus.common.exception.MSErrorCode;
import com.kkl.kklplus.common.response.MSResponse;
import com.kkl.kklplus.entity.b2b.common.B2BProcessFlag;
import com.kkl.kklplus.entity.b2bcenter.rpt.B2BOrderProcesslog;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * @author wln
 */
@Service
@Slf4j
@Transactional(propagation = Propagation.NOT_SUPPORTED)
public class VioMiOrderSnCodeService {

    @Autowired
    private B2BProcesslogService b2BProcesslogService;

    @Autowired
    private SysLogService sysLogService;

    public MSResponse getGradeSnToVioMi(VioMiOrderSnCode vioMiOrderSnCode) {
        MSResponse msResponse = new MSResponse();
        String orderNumber = vioMiOrderSnCode.getOrderNumber();
        if (StringUtils.isBlank(orderNumber)) {
            msResponse.setErrorCode(MSErrorCode.FAILURE);
            msResponse.setMsg("工单编号不能为空");
            return msResponse;
        }
        if (StringUtils.isBlank(vioMiOrderSnCode.getSnCode())) {
            msResponse.setErrorCode(MSErrorCode.FAILURE);
            msResponse.setMsg("产品SN码不能为空");
            return msResponse;
        }

        // 生成发送Log
        B2BOrderProcesslog b2BProcesslog = new B2BOrderProcesslog();
        b2BProcesslog.setInterfaceName(OperationCommand.OperationCode.ORDER_GRADESN.apiUrl);
        b2BProcesslog.setProcessTime(0);
        b2BProcesslog.setCreateById(vioMiOrderSnCode.getCreateById());
        b2BProcesslog.setUpdateById(vioMiOrderSnCode.getCreateById());
        b2BProcesslog.preInsert();
        b2BProcesslog.setProcessFlag(0);
        b2BProcesslog.setQuarter(QuarterUtils.getQuarter(b2BProcesslog.getCreateDt()));
        VioMiOrderSnCodeRequest reqBody = new VioMiOrderSnCodeRequest();
        reqBody.setOrderNumber(vioMiOrderSnCode.getOrderNumber());
        reqBody.setSnCode(vioMiOrderSnCode.getSnCode());
        OperationCommand command = OperationCommand.newInstance(OperationCommand.OperationCode.ORDER_GRADESN, reqBody);
        ResponseBody<ResponseData> resBody = OkHttpUtils.postSyncGenericNew(command, ResponseData.class);
        // 生成要发送的json对象
        String infoJson = GsonUtils.getInstance().toUnderscoresJson(reqBody);
        b2BProcesslog.setInfoJson(infoJson);
        try {
            b2BProcesslogService.insert(b2BProcesslog);
            b2BProcesslog.setResultJson(resBody.getOriginalJson());
            ResponseData data = resBody.getData();
            if (resBody.getErrorCode() == ResponseBody.ErrorCode.SUCCESS.code) {
                // 0 -- 成功
                if (data.getErrorCode().equals(0)) {
                    b2BProcesslog.setProcessFlag(B2BProcessFlag.PROCESS_FLAG_SUCESS.value);
                    b2BProcesslogService.updateProcessFlag(b2BProcesslog);
                    msResponse.setMsg(data.getErrorMsg());
                    return msResponse;
                }
            }
            //Code: 2.请填写正确的产品SN码 3.该工单费用由客户支付，请注意收费 1000接口异常 4.该SN码产品为等级品，不提供退换服务及免费安装/维修服务
            Integer errorCode = data != null ? data.getErrorCode() : resBody.getErrorCode();
            //1：签名无效，2：请填写正确的产品SN码，3：该工单费用由客户支付，请注意收费 ，4：该SN码产品为等级品，不提供（转鉴定）退换服务及免费的安装/维修服务
            String errorStr = data != null ? data.getErrorMsg() : resBody.getErrorMsg();
            errorStr = StringUtils.left(errorStr, 255);
            b2BProcesslog.setProcessFlag(B2BProcessFlag.PROCESS_FLAG_FAILURE.value);
            b2BProcesslog.setProcessComment(errorStr);
            msResponse.setThirdPartyErrorCode(new MSErrorCode(errorCode, errorStr));
            //请求异常 或 签名无效、不存在条码时报错
            if(errorCode == 1 || errorCode == 2 || errorCode >= 90000300 ) {
                msResponse.setCode(errorCode);
                msResponse.setMsg(errorStr);
            }
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
            sysLogService.insert(1L, infoJson, e.getMessage(), "调用云米获取产品SN码验证接口失败", OperationCommand.OperationCode.ORDER_GRADESN.apiUrl, "POST");
            return msResponse;
        }
    }
}
