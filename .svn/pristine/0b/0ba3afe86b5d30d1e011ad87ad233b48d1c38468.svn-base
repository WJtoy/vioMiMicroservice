package com.kkl.kklplus.b2b.viomi.service;

import com.google.gson.reflect.TypeToken;
import com.kkl.kklplus.b2b.viomi.entity.VioMiMessageLog;
import com.kkl.kklplus.b2b.viomi.entity.VioMiMessageProcessFlagEnum;
import com.kkl.kklplus.b2b.viomi.entity.VioMiOrderInfo;
import com.kkl.kklplus.b2b.viomi.entity.request.VioMiCommonRequest;
import com.kkl.kklplus.b2b.viomi.entity.request.VioMiOrderRemarkRequest;
import com.kkl.kklplus.b2b.viomi.entity.response.VioMiResponse;
import com.kkl.kklplus.b2b.viomi.http.command.OperationCommand;
import com.kkl.kklplus.b2b.viomi.http.config.B2BVioMiProperties;
import com.kkl.kklplus.b2b.viomi.http.request.OrderRemarkRequestParam;
import com.kkl.kklplus.b2b.viomi.http.response.ResponseBody;
import com.kkl.kklplus.b2b.viomi.http.utils.OkHttpUtils;
import com.kkl.kklplus.b2b.viomi.mapper.OrderRemarkMapper;
import com.kkl.kklplus.b2b.viomi.mq.sender.B2BCenterOrderProcessMQSend;
import com.kkl.kklplus.b2b.viomi.utils.GsonUtils;
import com.kkl.kklplus.b2b.viomi.utils.QuarterUtils;
import com.kkl.kklplus.b2b.viomi.utils.VioMiUtils;
import com.kkl.kklplus.common.exception.MSErrorCode;
import com.kkl.kklplus.common.response.MSResponse;
import com.kkl.kklplus.entity.b2b.common.B2BProcessFlag;
import com.kkl.kklplus.entity.b2bcenter.md.B2BDataSourceEnum;
import com.kkl.kklplus.entity.b2bcenter.pb.MQB2BOrderProcessMessage;
import com.kkl.kklplus.entity.b2bcenter.rpt.B2BOrderProcesslog;
import com.kkl.kklplus.entity.b2bcenter.sd.B2BOrderActionEnum;
import com.kkl.kklplus.entity.viomi.sd.VioMiOrderRemark;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;

/**
 *工单备注事务Service
 * @author chenxj
 * @date 2020/09/21
 */
@Slf4j
@Service
@Transactional
public class OrderRemarkService {

    @Autowired
    private B2BProcesslogService b2BProcesslogService;

    @Autowired
    private OrderInfoService orderInfoService;

    @Autowired
    private SysLogService sysLogService;

    @Resource
    private OrderRemarkMapper orderRemarkMapper;

    @Autowired
    private B2BCenterOrderProcessMQSend orderProcessMQSend;

    @Autowired
    private B2BVioMiProperties vioMiProperties;

    @Autowired
    private VioMiMessageLogService vioMiMessageLogService;


    public VioMiResponse remarkOrder(String json) {
        VioMiCommonRequest<VioMiOrderRemarkRequest> request = GsonUtils.getInstance().fromUnderscoresJson
                (json, new TypeToken<VioMiCommonRequest<VioMiOrderRemarkRequest>>() {
        }.getType());
        if(!vioMiProperties.getDataSourceConfig().getKey().equals(request.getKey())){
            return new VioMiResponse(1,"签名异常");
        }
        VioMiOrderRemarkRequest data = request.getData();
        VioMiResponse response = validateOrderRemark(data);
        if(response.getRes() == 0){
            VioMiOrderInfo orderInfo = orderInfoService.getOrderByOrderNumber(data.getOrderNumber());
            if(orderInfo == null){
                response.setRes(VioMiUtils.FAILURE_CODE);
                response.setMsg("没找到对应单据");
                return response;
            }
            Long kklOrderId = orderInfo.getKklOrderId();
            VioMiOrderRemark remark = parseOrderEntity(data);
            remark.setB2bOrderId(orderInfo.getId());
            remark.setKklOrderId(kklOrderId);
            orderRemarkMapper.insert(remark);
            if(!"投诉".equals(orderInfo.getType()) && kklOrderId != null && kklOrderId > 0) {
                sendOrderRemarkMQ(remark);
            }
        }
        return response;
    }

    private void sendOrderRemarkMQ(VioMiOrderRemark remark) {
        MQB2BOrderProcessMessage.B2BOrderProcessMessage processMessage =
                MQB2BOrderProcessMessage.B2BOrderProcessMessage.newBuilder()
                        .setMessageId(remark.getId())
                        .setB2BOrderNo(remark.getOrderNumber())
                        .setKklOrderId(remark.getKklOrderId())
                        .setB2BOrderId(remark.getB2bOrderId())
                        .setActionType(B2BOrderActionEnum.LOG.value)
                        .setDataSource(B2BDataSourceEnum.VIOMI.id)
                        .setRemarks(remark.getRemarks()).build();
        orderProcessMQSend.send(processMessage);
    }

    private VioMiOrderRemark parseOrderEntity(VioMiOrderRemarkRequest data) {
        VioMiOrderRemark vioMiOrderRemark = new VioMiOrderRemark();
        vioMiOrderRemark.setType(1);
        vioMiOrderRemark.setOperator(data.getInputuser());
        vioMiOrderRemark.setOrderNumber(data.getOrderNumber());
        vioMiOrderRemark.setRemarks(data.getRemarks());
        vioMiOrderRemark.setCreateById(1L);
        vioMiOrderRemark.preInsert();
        vioMiOrderRemark.setQuarter(QuarterUtils.getQuarter(vioMiOrderRemark.getCreateDt()));
        return vioMiOrderRemark;
    }

    private VioMiResponse validateOrderRemark(VioMiOrderRemarkRequest data) {
        VioMiResponse response = new VioMiResponse();
        String orderNumber = data.getOrderNumber();
        if(StringUtils.isBlank(orderNumber)){
            response.setRes(VioMiUtils.FAILURE_CODE);
            response.setMsg("工单编号不能为空");
            return response;
        }
        String remarks = data.getRemarks();
        if(StringUtils.isBlank(remarks)){
            response.setRes(VioMiUtils.FAILURE_CODE);
            response.setMsg("工单备注不能为空");
            return response;
        }
        return response;
    }

    /**
     * 通知第三方api备注
     * @param remark
     * @return
     */
    public MSResponse remarkApiRequest(VioMiOrderRemark remark) {
        MSResponse msResponse = new MSResponse();
        Long kklOrderId = remark.getKklOrderId();
        if(remark == null || kklOrderId == null || kklOrderId == 0) {
            msResponse.setErrorCode(MSErrorCode.FAILURE);
            msResponse.setMsg("信息不能为空!");
            return msResponse;
        }
        msResponse.setErrorCode(MSErrorCode.SUCCESS);
        VioMiMessageLog vioMiMessageLog = vioMiMessageLogService.insert
                (remark.getCreateById(), remark.getCreateDt(), remark.getUniqueId());
        if(vioMiMessageLog.getProcessFlag() >= 20){
            return msResponse;
        }
        B2BOrderProcesslog b2BProcesslog = new B2BOrderProcesslog();
        b2BProcesslog.setInterfaceName(OperationCommand.OperationCode.OTHER_REMARK_ORDER.apiUrl);
        b2BProcesslog.setProcessFlag(B2BProcessFlag.PROCESS_FLAG_FAILURE.value);
        b2BProcesslog.setProcessTime(0);
        b2BProcesslog.setCreateById(remark.getCreateById());
        b2BProcesslog.setUpdateById(remark.getCreateById());
        b2BProcesslog.preInsert();
        b2BProcesslog.setQuarter(com.kkl.kklplus.utils.QuarterUtils.getQuarter(b2BProcesslog.getCreateDate()));
        remark.setQuarter(QuarterUtils.getQuarter(remark.getCreateDt()));
        VioMiOrderInfo orderInfo = orderInfoService.getOrderNumberByKklOrderId(remark.getKklOrderId());
        if(orderInfo == null){
            msResponse.setErrorCode(MSErrorCode.FAILURE);
            msResponse.setMsg("没有找到对应的工单号orderNo！");
            return msResponse;
        }
        remark.setOrderNumber(orderInfo.getOrderNumber());
        remark.setB2bOrderId(orderInfo.getId());
        OrderRemarkRequestParam reqBody = new OrderRemarkRequestParam();
        reqBody.setOrderNumber(remark.getOrderNumber());
        reqBody.setInputuser(remark.getOperator());
        reqBody.setRemarks(remark.getRemarks());
        OperationCommand command = OperationCommand.newInstance
                (OperationCommand.OperationCode.OTHER_REMARK_ORDER, reqBody);
        ResponseBody<ResponseBody> resBody = OkHttpUtils.postSyncGenericNew(command, ResponseBody.class);
        String infoJson = GsonUtils.getInstance().toUnderscoresJson(reqBody);
        b2BProcesslog.setInfoJson(infoJson);
        try {
            b2BProcesslogService.insert(b2BProcesslog);
            b2BProcesslog.setResultJson(resBody.getOriginalJson());
            orderRemarkMapper.insert(remark);
            ResponseBody data = resBody.getData();
            if( resBody.getErrorCode() == ResponseBody.ErrorCode.SUCCESS.code && data != null){
                // 0 -- 成功
                if (data.getErrorCode().equals(VioMiUtils.SUCCESS_CODE)) {
                    vioMiMessageLog.setProcessFlag(VioMiMessageProcessFlagEnum.PROCESS_FLAG_SUCCESS.value);
                    vioMiMessageLogService.updateProcessFlag(vioMiMessageLog);
                    b2BProcesslog.setProcessFlag(B2BProcessFlag.PROCESS_FLAG_SUCESS.value);
                    b2BProcesslogService.updateProcessFlag(b2BProcesslog);
                    remark.preUpdate();
                    remark.setProcessFlag(B2BProcessFlag.PROCESS_FLAG_SUCESS.value);
                    orderRemarkMapper.updateProcessFlag(remark);
                    msResponse.setMsg(data.getErrorMsg());
                    return msResponse;
                }
            }
            // 1：签名无效 2：参数有误3：流程错误 4：办理方式错误 5：办理参数有误 6：验收码错误 7：SN码有误
            String errorStr = data != null ? data.getErrorMsg() : resBody.getErrorMsg();
            Integer errorCode = data != null ? data.getErrorCode() : resBody.getErrorCode();
            errorStr = StringUtils.left(errorStr,255);
            b2BProcesslog.setProcessFlag(B2BProcessFlag.PROCESS_FLAG_FAILURE.value);
            b2BProcesslog.setProcessComment(errorStr);
            remark.setProcessFlag(B2BProcessFlag.PROCESS_FLAG_FAILURE.value);
            remark.setProcessComment(errorStr);
            remark.preUpdate();
            msResponse.setThirdPartyErrorCode(new MSErrorCode(errorCode,errorStr));
            //errorCode大于等于90000300，设置错误编码，便于调用方重试
            if(errorCode >= ResponseBody.ErrorCode.REQUEST_INVOCATION_FAILURE.code){
                vioMiMessageLog.setProcessFlag(VioMiMessageProcessFlagEnum.PROCESS_FLAG_REJECT.value);
                msResponse.setCode(errorCode);
                msResponse.setMsg(errorStr);
            }else{
                vioMiMessageLog.setProcessFlag(VioMiMessageProcessFlagEnum.PROCESS_FLAG_FAILURE.value);
            }
            vioMiMessageLog.setProcessComment(errorStr);
            vioMiMessageLogService.updateProcessFlag(vioMiMessageLog);
            b2BProcesslogService.updateProcessFlag(b2BProcesslog);
            orderRemarkMapper.updateProcessFlag(remark);
        }catch (Exception e) {
            msResponse.setErrorCode(MSErrorCode.FAILURE);
            msResponse.setMsg(StringUtils.left(e.getMessage(),200));
            b2BProcesslog.setProcessFlag(B2BProcessFlag.PROCESS_FLAG_FAILURE.value);
            b2BProcesslog.setProcessComment(e.getMessage());
            b2BProcesslogService.updateProcessFlag(b2BProcesslog);
            String errorStr = "工单日志同步失败，原因是：";
            log.error(errorStr, e.getMessage());
            sysLogService.insert(1L,infoJson,errorStr + e.getMessage(),
                    errorStr, OperationCommand.OperationCode.OTHER_REMARK_ORDER.apiUrl, "POST");
        }
        return msResponse;
    }
}
