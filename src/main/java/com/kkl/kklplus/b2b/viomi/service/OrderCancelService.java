package com.kkl.kklplus.b2b.viomi.service;

import com.google.gson.reflect.TypeToken;
import com.kkl.kklplus.b2b.viomi.entity.VioMiMessageLog;
import com.kkl.kklplus.b2b.viomi.entity.VioMiMessageProcessFlagEnum;
import com.kkl.kklplus.b2b.viomi.entity.VioMiOrderInfo;
import com.kkl.kklplus.b2b.viomi.entity.request.VioMiCommonRequest;
import com.kkl.kklplus.b2b.viomi.entity.request.VioMiHandleRequest;
import com.kkl.kklplus.b2b.viomi.entity.request.VioMiOrderCancelRequest;
import com.kkl.kklplus.b2b.viomi.entity.response.VioMiResponse;
import com.kkl.kklplus.b2b.viomi.http.command.OperationCommand;
import com.kkl.kklplus.b2b.viomi.http.config.B2BVioMiProperties;
import com.kkl.kklplus.b2b.viomi.http.request.HandleParam;
import com.kkl.kklplus.b2b.viomi.http.request.OrderCancelRequestParam;
import com.kkl.kklplus.b2b.viomi.http.response.ResponseBody;
import com.kkl.kklplus.b2b.viomi.http.utils.OkHttpUtils;
import com.kkl.kklplus.b2b.viomi.mapper.OrderCancelMapper;
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
import com.kkl.kklplus.entity.b2bcenter.sd.B2BOrderTransferResult;
import com.kkl.kklplus.entity.viomi.sd.VioMiApiLog;
import com.kkl.kklplus.entity.viomi.sd.VioMiOrderCancel;
import com.kkl.kklplus.entity.viomi.sd.VioMiOrderHandle;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;

/**
 *工单撤销Service
 * @author chenxj
 * @date 2020/09/21
 */
@Slf4j
@Service
@Transactional(propagation = Propagation.NOT_SUPPORTED)
public class OrderCancelService {

    @Autowired
    private B2BProcesslogService b2BProcesslogService;

    @Autowired
    private OrderInfoService orderInfoService;

    @Autowired
    private SysLogService sysLogService;

    @Autowired
    private B2BCenterOrderProcessMQSend orderProcessMQSend;

    @Autowired
    private B2BVioMiProperties vioMiProperties;

    @Resource
    private OrderCancelMapper orderCancelMapper;

    @Autowired
    private VioMiOrderHandleService orderHandleService;

    @Autowired
    private VioMiMessageLogService vioMiMessageLogService;

    @Autowired
    private ViomiApiLogService viomiApiLogService;



    public VioMiResponse cancelOrder(String json) {
        VioMiCommonRequest<VioMiOrderCancelRequest> request = GsonUtils.getInstance().fromUnderscoresJson
                (json, new TypeToken<VioMiCommonRequest<VioMiOrderCancelRequest>>() {
                }.getType());
        if(!vioMiProperties.getDataSourceConfig().getKey().equals(request.getKey())){
            return new VioMiResponse(1,"签名异常");
        }
        VioMiOrderCancelRequest data = request.getData();
        VioMiResponse response = validateOrderCancel(data);

        if(response.getRes() == 0){
            VioMiOrderInfo orderInfo = orderInfoService.getOrderByOrderNumber(data.getOrderNumber());
            if(orderInfo == null){
                response.setRes(VioMiUtils.FAILURE_CODE);
                response.setMsg("没找到对应单据");
                return response;
            }

            Long kklOrderId = orderInfo.getKklOrderId();
            VioMiOrderCancel cancel = parseEntity(data);
            cancel.setB2bOrderId(orderInfo.getId());
            cancel.setCode("");
            orderCancelMapper.insert(cancel);
            if(!"投诉".equals(orderInfo.getType()) && kklOrderId != null && kklOrderId > 0) {
                sendOrderCancelMQ(cancel,kklOrderId);
            }else{
                B2BOrderTransferResult result = new B2BOrderTransferResult();
                result.setId(orderInfo.getId());
                result.setProcessComment("云米主动撤销");
                result.setProcessFlag(5);
                result.setUpdater("1");
                result.setUpdateDt(System.currentTimeMillis());
                orderInfoService.cancalOrder(result);
            }
        }
        return response;
    }

    private void sendOrderCancelMQ(VioMiOrderCancel cancel,Long kklOrderId) {
        MQB2BOrderProcessMessage.B2BOrderProcessMessage processMessage =
                MQB2BOrderProcessMessage.B2BOrderProcessMessage.newBuilder()
                        .setMessageId(cancel.getId())
                        .setB2BOrderNo(cancel.getOrderNumber())
                        .setKklOrderId(kklOrderId)
                        .setB2BOrderId(cancel.getB2bOrderId())
                        .setActionType(B2BOrderActionEnum.RETURN_APPLY.value)
                        .setDataSource(B2BDataSourceEnum.VIOMI.id)
                        .setRemarks(StringUtils.trimToEmpty(cancel.getRemarks())).build();
        orderProcessMQSend.send(processMessage);
    }

    private VioMiOrderCancel parseEntity(VioMiOrderCancelRequest data) {
        VioMiOrderCancel vioMiOrderCancel = new VioMiOrderCancel();
        vioMiOrderCancel.setType(1);
        vioMiOrderCancel.setOperator(data.getHandle().getOperator());
        vioMiOrderCancel.setReason(data.getHandle().getReason());
        vioMiOrderCancel.setRemarks(data.getHandle().getRemarks());
        vioMiOrderCancel.setOrderNumber(data.getOrderNumber());
        vioMiOrderCancel.setCreateById(1L);
        vioMiOrderCancel.preInsert();
        vioMiOrderCancel.setQuarter(QuarterUtils.getQuarter(vioMiOrderCancel.getCreateDt()));
        return vioMiOrderCancel;
    }

    private VioMiResponse validateOrderCancel(VioMiOrderCancelRequest data) {
        VioMiResponse response = new VioMiResponse();
        String orderNumber = data.getOrderNumber();
        if(StringUtils.isBlank(orderNumber)){
            response.setRes(VioMiUtils.FAILURE_CODE);
            response.setMsg("工单编号不能为空");
            return response;
        }
        VioMiHandleRequest handle = data.getHandle();
        if(handle == null){
            response.setRes(VioMiUtils.FAILURE_CODE);
            response.setMsg("办理方式不能为空");
            return response;
        }
        String reason = handle.getReason();
        if(StringUtils.isBlank(reason)){
            response.setRes(VioMiUtils.FAILURE_CODE);
            response.setMsg("工单撤销原因不能为空");
            return response;
        }
        return response;
    }

    public MSResponse cancelApiRequest(VioMiOrderCancel cancel) {
        MSResponse msResponse = new MSResponse();
        Long b2bOrderId = cancel.getB2bOrderId();
        if(cancel == null || b2bOrderId == null || b2bOrderId == 0) {
            msResponse.setErrorCode(MSErrorCode.FAILURE);
            msResponse.setMsg("信息不能为空!");
            return msResponse;
        }
        VioMiMessageLog vioMiMessageLog = vioMiMessageLogService.insert(cancel.getCreateById(), cancel.getCreateDt(), cancel.getUniqueId());
        if(vioMiMessageLog.getProcessFlag() >= 20){
            msResponse.setErrorCode(MSErrorCode.SUCCESS);
            return msResponse;
        }
        VioMiOrderInfo vioMiOrderInfo = orderInfoService.getOrderStatusByOrderId(cancel.getB2bOrderId());
        if(vioMiOrderInfo.getType().equals("换货")){
            VioMiOrderHandle vioMiOrderHandle = new VioMiOrderHandle();
            vioMiOrderHandle.setUniqueId(cancel.getUniqueId());
            vioMiOrderHandle.setOrderNumber(cancel.getOrderNumber());
            vioMiOrderHandle.setB2bOrderId(cancel.getB2bOrderId());
            vioMiOrderHandle.setCreateById(cancel.getCreateById());
            vioMiOrderHandle.setCreateDt(cancel.getCreateDt());
            vioMiOrderHandle.setStatus(B2BOrderActionEnum.CONVERTED_CANCEL.value);
            vioMiOrderHandle.setOperator(cancel.getOperator());
            vioMiOrderHandle.setRemarks(cancel.getReason()+StringUtils.trimToEmpty(cancel.getRemarks()));
            return orderHandleService.orderConfirm(vioMiOrderHandle);
        }
        msResponse.setErrorCode(MSErrorCode.SUCCESS);
        B2BOrderProcesslog b2BProcesslog = new B2BOrderProcesslog();
        b2BProcesslog.setInterfaceName(OperationCommand.OperationCode.OTHER_CANCEL_ORDER.apiUrl);
        b2BProcesslog.setProcessFlag(B2BProcessFlag.PROCESS_FLAG_FAILURE.value);
        b2BProcesslog.setProcessTime(0);
        b2BProcesslog.setCreateById(cancel.getCreateById());
        b2BProcesslog.setUpdateById(cancel.getCreateById());
        b2BProcesslog.preInsert();
        b2BProcesslog.setQuarter(QuarterUtils.getQuarter(b2BProcesslog.getCreateDt()));
        cancel.setQuarter(QuarterUtils.getQuarter(cancel.getCreateDt()));
        OrderCancelRequestParam reqBody = new OrderCancelRequestParam();
        reqBody.setOrderNumber(cancel.getOrderNumber());
        HandleParam handleParam = new HandleParam();
        handleParam.setWay("工单撤销");
        handleParam.setOperator(cancel.getOperator());
        handleParam.setReason(cancel.getReason());
        String code = cancel.getCode();
        if(StringUtils.isNotBlank(code)){
            handleParam.setCode(code);
        }else{
            cancel.setCode("");
        }
        handleParam.setRemarks(cancel.getRemarks());
        reqBody.setHandle(handleParam);
        OperationCommand command = OperationCommand.newInstance
                (OperationCommand.OperationCode.OTHER_CANCEL_ORDER, reqBody);
        ResponseBody<ResponseBody> resBody = OkHttpUtils.postSyncGenericNew(command, ResponseBody.class);
        String infoJson = GsonUtils.getInstance().toUnderscoresJson(reqBody);
        b2BProcesslog.setInfoJson(infoJson);
        try {
            b2BProcesslogService.insert(b2BProcesslog);
            b2BProcesslog.setResultJson(resBody.getOriginalJson());
            orderCancelMapper.insert(cancel);
            ResponseBody data = resBody.getData();
            if( resBody.getErrorCode() == ResponseBody.ErrorCode.SUCCESS.code && data != null){
                // 0 -- 成功
                if (data.getErrorCode().equals(VioMiUtils.SUCCESS_CODE)) {
                    if("维修".equals(vioMiOrderInfo.getType())) {
                        sendOrderCancelApprove(cancel, vioMiOrderInfo, 0, "");
                    }
                    vioMiMessageLog.setProcessFlag(VioMiMessageProcessFlagEnum.PROCESS_FLAG_SUCCESS.value);
                    vioMiMessageLogService.updateProcessFlag(vioMiMessageLog);

                    saveApiLog(b2bOrderId, B2BOrderActionEnum.CONVERTED_CANCEL.value, cancel.getCreateById(), infoJson, 0, resBody.getOriginalJson(), data.getErrorMsg(), B2BProcessFlag.PROCESS_FLAG_SUCESS.value);

                    b2BProcesslog.setProcessFlag(B2BProcessFlag.PROCESS_FLAG_SUCESS.value);
                    b2BProcesslogService.updateProcessFlag(b2BProcesslog);
                    cancel.setProcessFlag(B2BProcessFlag.PROCESS_FLAG_SUCESS.value);
                    cancel.preUpdate();
                    orderCancelMapper.updateProcessFlag(cancel);
                    msResponse.setMsg(data.getErrorMsg());
                    // 更新订单状态
                    VioMiOrderInfo updateVioMiOrderInfo = new VioMiOrderInfo();
                    updateVioMiOrderInfo.setId(vioMiOrderInfo.getId());
                    updateVioMiOrderInfo.setOrderStatus(B2BOrderActionEnum.CONVERTED_CANCEL.value);
                    updateVioMiOrderInfo.setViomiStatus("工单撤销");
                    updateVioMiOrderInfo.setViomiSubStatus("工单撤销");
                    updateVioMiOrderInfo.setUpdateById(cancel.getCreateById());
                    updateVioMiOrderInfo.setUpdateDt(cancel.getCreateDt());
                    orderInfoService.updateOrderStatus(updateVioMiOrderInfo);
                    return msResponse;
                }
            }
            // 1：签名无效 2：参数有误3：流程错误 4：办理方式错误 5：办理参数有误 6：验收码错误 7：SN码有误
            String errorStr = data != null ? data.getErrorMsg() : resBody.getErrorMsg();
            Integer errorCode = data != null ? data.getErrorCode() : resBody.getErrorCode();
            errorStr = StringUtils.left(errorStr,255);
            b2BProcesslog.setProcessFlag(B2BProcessFlag.PROCESS_FLAG_FAILURE.value);
            b2BProcesslog.setProcessComment(errorStr);
            cancel.setProcessFlag(B2BProcessFlag.PROCESS_FLAG_FAILURE.value);
            cancel.setProcessComment(errorStr);
            cancel.preUpdate();
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
            if("维修".equals(vioMiOrderInfo.getType())) {
                sendOrderCancelApprove(cancel, vioMiOrderInfo, 1, errorStr);
            }
            vioMiMessageLogService.updateProcessFlag(vioMiMessageLog);

            if(!vioMiMessageLog.getDuplicateFlag() || errorCode < ResponseBody.ErrorCode.REQUEST_INVOCATION_FAILURE.code) {
                saveApiLog(b2bOrderId, B2BOrderActionEnum.CONVERTED_CANCEL.value, cancel.getCreateById(), infoJson, 1, resBody.getOriginalJson(), data.getErrorMsg(), B2BProcessFlag.PROCESS_FLAG_FAILURE.value);
            }

            b2BProcesslogService.updateProcessFlag(b2BProcesslog);
            orderCancelMapper.updateProcessFlag(cancel);
        }catch (Exception e) {
            msResponse.setErrorCode(MSErrorCode.FAILURE);
            msResponse.setMsg(StringUtils.left(e.getMessage(),200));
            b2BProcesslog.setProcessFlag(B2BProcessFlag.PROCESS_FLAG_FAILURE.value);
            b2BProcesslog.setProcessComment(e.getMessage());
            b2BProcesslogService.updateProcessFlag(b2BProcesslog);
            String errorStr = "工单撤销失败，原因是：";
            log.error(errorStr, e.getMessage());
            sysLogService.insert(1L,infoJson,errorStr + e.getMessage(),
                    errorStr, OperationCommand.OperationCode.OTHER_CANCEL_ORDER.apiUrl, "POST");
        }
        return msResponse;
    }

    private void sendOrderCancelApprove(VioMiOrderCancel cancel, VioMiOrderInfo vioMiOrderInfo,Integer status, String remarks) {
        MQB2BOrderProcessMessage.B2BOrderProcessMessage processMessage =
                MQB2BOrderProcessMessage.B2BOrderProcessMessage.newBuilder()
                        .setMessageId(cancel.getId())
                        .setB2BOrderNo(cancel.getOrderNumber())
                        .setKklOrderId(vioMiOrderInfo.getKklOrderId())
                        .setB2BOrderId(cancel.getB2bOrderId())
                        .setActionType(B2BOrderActionEnum.RETURN_APPROVE.value)
                        .setStatus(status)
                        .setDataSource(B2BDataSourceEnum.VIOMI.id)
                        .setRemarks(remarks).build();
        orderProcessMQSend.send(processMessage);
    }

    public void updateProcessFlag(Long id) {
        VioMiOrderCancel vioMiOrderCancel = new VioMiOrderCancel();
        vioMiOrderCancel.setId(id);
        vioMiOrderCancel.setProcessFlag(B2BProcessFlag.PROCESS_FLAG_SUCESS.value);
        vioMiOrderCancel.preUpdate();
        orderCancelMapper.updateProcessFlag(vioMiOrderCancel);
    }

    private void saveApiLog(Long b2bOrderId, Integer status, Long createById, String infoJson, Integer bFail, String resultJson, String processComment, Integer processFlag) {
        final Integer API_EXCEPTION_STATUS_NORMAL = 0;   // 正常状态
        final Integer API_EXCEPTION_STATUS_ABNORMAL = 1;  // 异常状态

        VioMiApiLog vioMiApiLog = new VioMiApiLog();
        vioMiApiLog.setB2bOrderId(b2bOrderId);
        vioMiApiLog.setOperatingStatus(status);
        vioMiApiLog.setInterfaceName(OperationCommand.OperationCode.OTHER_CANCEL_ORDER.apiUrl);
        vioMiApiLog.setInfoJson(infoJson);
        vioMiApiLog.setProcessFlag(processFlag);
        vioMiApiLog.setProcessTime(0);
        vioMiApiLog.setResultJson(resultJson);
        vioMiApiLog.setProcessComment(processComment);
        vioMiApiLog.setCreateBy(createById);
        vioMiApiLog.setUpdateBy(createById);
        long currentDate = System.currentTimeMillis();
        vioMiApiLog.setCreateDt(currentDate);
        vioMiApiLog.setUpdateDt(currentDate);
        vioMiApiLog.setQuarter(QuarterUtils.getQuarter(currentDate));

        viomiApiLogService.insert(vioMiApiLog);
        Long vioMiApiLogId = vioMiApiLog.getId();

        // 更新异常状态及首次异常日志中id
        if (bFail.equals(1)) {  //1-失败，0- 成功
            VioMiOrderInfo updateVioMiOrderInfo = new VioMiOrderInfo();
            updateVioMiOrderInfo.setId(b2bOrderId);
            updateVioMiOrderInfo.setApiExceptionStatus(API_EXCEPTION_STATUS_ABNORMAL);
            updateVioMiOrderInfo.setFirstExceptionId(vioMiApiLogId);
            updateVioMiOrderInfo.setUpdateById(createById);
            updateVioMiOrderInfo.setUpdateDt(currentDate);

            orderInfoService.updateException(updateVioMiOrderInfo);
        }
    }
}
