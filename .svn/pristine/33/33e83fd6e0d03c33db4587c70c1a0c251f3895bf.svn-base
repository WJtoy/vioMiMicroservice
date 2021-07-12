package com.kkl.kklplus.b2b.viomi.service;

import com.kkl.kklplus.b2b.viomi.entity.*;
import com.kkl.kklplus.b2b.viomi.http.command.OperationCommand;
import com.kkl.kklplus.b2b.viomi.http.request.*;
import com.kkl.kklplus.b2b.viomi.http.response.ResponseBody;
import com.kkl.kklplus.b2b.viomi.http.utils.OkHttpUtils;
import com.kkl.kklplus.b2b.viomi.mapper.VioMiApiLogMapper;
import com.kkl.kklplus.b2b.viomi.utils.GsonUtils;
import com.kkl.kklplus.b2b.viomi.utils.QuarterUtils;
import com.kkl.kklplus.common.exception.MSErrorCode;
import com.kkl.kklplus.common.response.MSResponse;
import com.kkl.kklplus.entity.b2b.common.B2BActionType;
import com.kkl.kklplus.entity.b2b.common.B2BProcessFlag;
import com.kkl.kklplus.entity.b2bcenter.rpt.B2BOrderProcesslog;
import com.kkl.kklplus.entity.b2bcenter.sd.B2BOrderActionEnum;
import com.kkl.kklplus.entity.viomi.sd.VioMiApiLog;
import com.kkl.kklplus.entity.viomi.sd.VioMiOrderHandle;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.Optional;

@Slf4j
@Service
@Transactional(propagation = Propagation.NOT_SUPPORTED)
public class VioMiOrderReviewService {
    @Autowired
    private OrderInfoService orderInfoService;

    @Autowired
    private ViomiApiLogService viomiApiLogService;

    @Autowired
    private B2BProcesslogService b2BProcesslogService;

    @Autowired
    private SysLogService sysLogService;

    /**
     * 重送订单信息
     * @param apiLogId  apiLogId
     * @param operator 操作员
     * @param operatorId 操作员id
     * @return
     */
    @Transactional
    public MSResponse resend(Long apiLogId, String operator, Long operatorId) {
        // 获取ApiLog中信息
        ViomiApiInfo vioMiApiLog = viomiApiLogService.getById(apiLogId);
        Long b2bOrderId = vioMiApiLog.getB2bOrderId();
        Integer operatingStatus = vioMiApiLog.getOperatingStatus();
        String infoJson = vioMiApiLog.getInfoJson();

        // 获取订单信息
        VioMiOrderInfo orderInfo = orderInfoService.getOrderStatusByOrderId(b2bOrderId);
        String viomiStatus = Optional.ofNullable(orderInfo).map(r->r.getViomiStatus()).orElse("");
        String viomiSubStatus = Optional.ofNullable(orderInfo).map(r->r.getViomiSubStatus()).orElse("");
        String orderType =  Optional.ofNullable(orderInfo).map(r->r.getType()).orElse("");

        String  way = "";
        String  node = "";
        MSResponse msResponse = null;
        if (operatingStatus.equals(B2BOrderActionEnum.PLAN.getValue())) {
            // 派单
            boolean needPlaning = false;
            if(orderType.equals("退货")){
                // 退货
                needPlaning = true;
                way = VioMiWay.STATUS_SERVICE_POINT_ORDER;
                node = VioMiWay.STATUS_PLANING;
            } else if(orderType.equals("换货")) {
                //换货单
                needPlaning = true;
                way = VioMiWay.STATUS_SERVICE_POINT_SEND_PACK;
                node = VioMiWay.STATUS_PLANING;
            } else if (viomiSubStatus.equals(VioMiWay.STATUS_PLANING) || viomiSubStatus.equals(VioMiWay.STATUS_REPLANING)) {
                //上一次 node：派送师傅 || 转派师傅
                needPlaning = true;
                way = VioMiWay.STATUS_APPOINTMENT;
                node = VioMiWay.STATUS_REPLANING;
            } else if ((StringUtils.isBlank(viomiStatus) && StringUtils.isBlank(viomiSubStatus))) {
                //上一次 way为空，node为空:为新工单
                needPlaning = true;
                way = VioMiWay.STATUS_PLANING;
                node = VioMiWay.STATUS_PLANING;
            } else if (viomiSubStatus.equals(VioMiWay.STATUS_APPOINTMENT_SUCCESS) || viomiSubStatus.equals(VioMiWay.STATUS_REAPPOINTMENT)) {
                //上一次  node: 预约成功 || 重新预约
                needPlaning = true;
                way = VioMiWay.STATUS_CLOCK_IN_HOME;
                node = VioMiWay.STATUS_REPLANING;
            } else if (viomiStatus.equals(VioMiWay.STATUS_CLOCK_IN_HOME) && viomiSubStatus.equals(VioMiWay.STATUS_CLOCK_IN_SUCCESS)) {
                //上一次 way：上门打卡，node: 打卡成功
                needPlaning = true;
                way = VioMiWay.STATUS_PROCESS_COMPLETE;
                node = VioMiWay.STATUS_REPLANING;
            }

            if (needPlaning) {
                OrderHandleRequestParam<EngineerParam> orderHandleRequestParam = GsonUtils.getInstance().fromUnderscoresJson(infoJson, OrderHandleRequestParam.class);
                HandleParam handleParam = orderHandleRequestParam.getHandle();
                if (handleParam != null) {
                    handleParam.setWay(way);
                    handleParam.setNode(node);
                    handleParam.setOperator(operator);
                }
                // 发送
                msResponse = pushMessageToVioMi(orderHandleRequestParam, apiLogId, b2bOrderId, operatorId, System.currentTimeMillis(), operatingStatus);
            } else {
                return new MSResponse(1, "无法操作", null, null);
            }
        } else if (operatingStatus.equals(B2BOrderActionEnum.APPOINT.getValue()) && (orderType.equals(VioMiOrderType.TYPE_INSTALL) || orderType.equals(VioMiOrderType.TYPE_REPAIR))) {
            // 预约
            boolean needAppointment = false;
            if ( viomiSubStatus.equals(VioMiWay.STATUS_PLANING)                     //上一次 node：派送师傅
                    || viomiSubStatus.equals(VioMiWay.STATUS_REPLANING)             //上一次 node：转派师傅
                    || viomiSubStatus.equals(VioMiWay.STATUS_APPOINTMENT_FAILURE)   //上一次 node: 预约失败
                    || viomiSubStatus.equals(VioMiWay.STATUS_APPOINTMENT_SUCCESS)   //上一次  node：预约成功
                    || viomiSubStatus.equals(VioMiWay.STATUS_REAPPOINTMENT)         //上一次  node: 重新预约
                    || viomiSubStatus.equals(VioMiWay.STATUS_CLOCK_IN_SUCCESS)      //上一次  node: 打卡成功
                    || orderType.equals("退货")     // 退货单
                    || orderType.equals("换货")     // 换货单
            ) {
                needAppointment = true;
            }

            if (needAppointment) {
                if(viomiStatus.equals(VioMiWay.STATUS_SERVICE_POINT_SEND_PACK) ||
                        viomiStatus.equals(VioMiWay.STATUS_SERVICE_POINT_ORDER) ||
                        viomiStatus.equals(VioMiWay.STATUS_ENGINEER_APPOINTMENT)){
                    way = VioMiWay.STATUS_ENGINEER_APPOINTMENT;
                    node = VioMiWay.STATUS_APPOINTMENT_SUCCESS;
                } else if(viomiSubStatus.equals(VioMiWay.STATUS_PLANING) || viomiSubStatus.equals(VioMiWay.STATUS_REPLANING)){
                    way = VioMiWay.STATUS_APPOINTMENT;
                    node = VioMiWay.STATUS_APPOINTMENT_SUCCESS;
                } else if (viomiSubStatus.equals(VioMiWay.STATUS_REAPPOINTMENT) || viomiSubStatus.equals(VioMiWay.STATUS_APPOINTMENT_SUCCESS)){
                    way = VioMiWay.STATUS_CLOCK_IN_HOME;
                    node = VioMiWay.STATUS_REAPPOINTMENT;
                } else if(StringUtils.isBlank(viomiSubStatus)){
                    // 上次操作为空时，退货单、换货单才能预约
                    if(orderType.equals("退货")){
                        way = VioMiWay.STATUS_SERVICE_POINT_ORDER;
                        node = VioMiWay.STATUS_APPOINTMENT;
                    }else if(orderType.equals("换货")){
                        way = VioMiWay.STATUS_SERVICE_POINT_SEND_PACK;
                        node = VioMiWay.STATUS_APPOINTMENT;
                    }
                } else {
                    way = VioMiWay.STATUS_PROCESS_COMPLETE;
                    node = VioMiWay.STATUS_REAPPOINTMENT;
                }
                // 预约失败，暂停工单 这两项功能暂不考虑
                OrderHandleRequestParam<AppointmentParam> orderHandleRequestParam = GsonUtils.getInstance().fromUnderscoresJson(infoJson, OrderHandleRequestParam.class);
                HandleParam handleParam = orderHandleRequestParam.getHandle();
                if (handleParam != null) {
                    handleParam.setWay(way);
                    handleParam.setNode(node);
                    handleParam.setOperator(operator);
                }

                msResponse = pushMessageToVioMi(orderHandleRequestParam, apiLogId, b2bOrderId, operatorId, System.currentTimeMillis(), operatingStatus);
            } else {
                return new MSResponse(1, "无法操作", null, null);
            }
        } else if (operatingStatus.equals(B2BOrderActionEnum.CONVERTED_CANCEL.getValue()) ) {
            if (orderType.equals("换货")) {
                OrderHandleRequestParam<CompleteParam> orderHandleRequestParam = GsonUtils.getInstance().fromUnderscoresJson(infoJson, OrderHandleRequestParam.class);
                msResponse = pushMessageToVioMi(orderHandleRequestParam, apiLogId, b2bOrderId, operatorId, System.currentTimeMillis(), operatingStatus);
            } else {
                OrderCancelRequestParam orderCancelRequestParam = GsonUtils.getInstance().fromUnderscoresJson(infoJson, OrderCancelRequestParam.class);
                msResponse = pushMessageToVioMi(orderCancelRequestParam, apiLogId, b2bOrderId, operatorId, System.currentTimeMillis(), operatingStatus);
            }
        } else if (operatingStatus.equals(B2BOrderActionEnum.SERVICE.getValue())) {
            //  上门
            OrderHandleRequestParam<LocationParam> orderHandleRequestParam = GsonUtils.getInstance().fromUnderscoresJson(infoJson, OrderHandleRequestParam.class);
            msResponse = pushMessageToVioMi(orderHandleRequestParam, apiLogId, b2bOrderId, operatorId, System.currentTimeMillis(), operatingStatus);
        } else {
            //确认收货  //完工 //退换货拆装   //退换货寄回 //鉴定
            OrderHandleRequestParam<CompleteParam> orderHandleRequestParam = GsonUtils.getInstance().fromUnderscoresJson(infoJson, OrderHandleRequestParam.class);
            msResponse = pushMessageToVioMi(orderHandleRequestParam, apiLogId, b2bOrderId, operatorId, System.currentTimeMillis(), operatingStatus);
        }
        return msResponse;
    }


    /**
     * 推送工单消息给云米
     * @param requestParam
     * @return
     */
    private MSResponse pushMessageToVioMi(com.kkl.kklplus.b2b.viomi.http.request.RequestParam requestParam, Long apiLogId, Long b2bOrderId, Long createById, Long createDt, Integer operatingStatus) {
        // 获取工单状态
        String strDataType = "";
        String strOrderNumber = "";
        String requestParamType = "";
        String node = "";
        if (requestParam instanceof  OrderHandleRequestParam) {
            OrderHandleRequestParam  orderHandleRequestParam = (OrderHandleRequestParam)requestParam;
            strDataType = orderHandleRequestParam.getHandle().getWay();
            strOrderNumber = orderHandleRequestParam.getOrderNumber();
            node = orderHandleRequestParam.getHandle().getNode();
            requestParamType = "handleRequest";
        } else if (requestParam instanceof  OrderCancelRequestParam) {
            OrderCancelRequestParam orderCancelRequestParam = (OrderCancelRequestParam)requestParam;
            strDataType = orderCancelRequestParam.getHandle().getWay();
            node = orderCancelRequestParam.getHandle().getNode();
            strOrderNumber = orderCancelRequestParam.getOrderNumber();
            requestParamType = "cancelRequest";
        }

        MSResponse msResponse = new MSResponse();
        msResponse.setErrorCode(MSErrorCode.SUCCESS);

        // 生成发送Log
        B2BOrderProcesslog b2BProcesslog = new B2BOrderProcesslog();
        b2BProcesslog.setDataType(strDataType);
        if (requestParamType.equals("handleRequest")) {
            b2BProcesslog.setInterfaceName(OperationCommand.OperationCode.ORDER_HANDLE.apiUrl);
        } else if (requestParamType.equals("cancelRequest")) {
            b2BProcesslog.setInterfaceName(OperationCommand.OperationCode.OTHER_CANCEL_ORDER.apiUrl);
        }
        b2BProcesslog.setProcessFlag(B2BProcessFlag.PROCESS_FLAG_PROCESSING.value);
        b2BProcesslog.setProcessTime(0);
        b2BProcesslog.setCreateById(createById);
        b2BProcesslog.setUpdateById(createById);
        b2BProcesslog.preInsert();
        b2BProcesslog.setQuarter(QuarterUtils.getQuarter(b2BProcesslog.getCreateDt()));

        // 生成要发送的json对象
        String infoJson = GsonUtils.getInstance().toUnderscoresJson(requestParam);
        log.warn("发送给云米的数据:{}", infoJson);
        b2BProcesslog.setInfoJson(infoJson);
        try {
            b2BProcesslogService.insert(b2BProcesslog);
            OperationCommand command = null;
            if (requestParamType.equals("handleRequest")) {
                command = OperationCommand.newInstance(OperationCommand.OperationCode.ORDER_HANDLE, requestParam);
            } else if (requestParamType.equals("cancelRequest")) {
                command = OperationCommand.newInstance(OperationCommand.OperationCode.OTHER_CANCEL_ORDER, requestParam);
            }
            ResponseBody<ResponseBody> resBody = OkHttpUtils.postSyncGenericNew(command, ResponseBody.class);
            b2BProcesslog.setResultJson(resBody.getOriginalJson());
            ResponseBody data = resBody.getData();
            // for test
//            resBody.setErrorCode(0);
//            data = new ResponseBody();
//            data.setErrorCode(0);
//            data.setErrorMsg("调用成功");
            //
            if( resBody.getErrorCode() == ResponseBody.ErrorCode.SUCCESS.code && data != null){
                if (data.getErrorCode().equals(0)) { // 0 -- 成功
                    //
                    VioMiOrderInfo vioMiOrderInfo = new VioMiOrderInfo();
                    Long id = viomiApiLogService.getIdByOrderId(b2bOrderId, apiLogId);
                    vioMiOrderInfo.setUpdateById(createById);
                    vioMiOrderInfo.setUpdateDt(createDt);
                    vioMiOrderInfo.setId(b2bOrderId);
                    if (id != null) {
                        vioMiOrderInfo.setFirstExceptionId(id);
                    } else {
                        vioMiOrderInfo.setFirstExceptionId(0L);
                        vioMiOrderInfo.setApiExceptionStatus(0);
                    }
                    vioMiOrderInfo.setViomiStatus(strDataType);
                    vioMiOrderInfo.setViomiSubStatus(node);
                    vioMiOrderInfo.setOrderStatus(operatingStatus);
                    orderInfoService.updateNextStepException(vioMiOrderInfo);

                    // 更新viomiApiLog的processFlag
                    VioMiApiLog vioMiApiLog = new VioMiApiLog();
                    vioMiApiLog.setId(apiLogId);
                    vioMiApiLog.setResultJson(resBody.getOriginalJson());
                    vioMiApiLog.setProcessFlag(B2BProcessFlag.PROCESS_FLAG_SUCESS.value);
                    vioMiApiLog.setUpdateBy(createById);
                    vioMiApiLog.setUpdateDt(System.currentTimeMillis());
                    viomiApiLogService.updateProcessFlag(vioMiApiLog);

                    b2BProcesslog.setProcessFlag(B2BProcessFlag.PROCESS_FLAG_SUCESS.value);
                    b2BProcesslogService.updateProcessFlag(b2BProcesslog);

                    msResponse.setMsg(data.getErrorMsg());

                    return msResponse;
                }
            }
            // 1：签名无效 2：参数有误3：流程错误 4：办理方式错误 5：办理参数有误 6：验收码错误 7：SN码有误
            String errorStr = data != null ? data.getErrorMsg() : resBody.getErrorMsg();
            Integer errorCode = data != null ? data.getErrorCode() : resBody.getErrorCode();
            errorStr = StringUtils.left(errorStr, 255);
            b2BProcesslog.setProcessFlag(B2BProcessFlag.PROCESS_FLAG_FAILURE.value);
            b2BProcesslog.setProcessComment(errorStr);
            msResponse.setThirdPartyErrorCode(new MSErrorCode(errorCode,errorStr));
            //errorCode大于等于90000300，设置错误编码，便于调用方重试
            if(errorCode >= ResponseBody.ErrorCode.REQUEST_INVOCATION_FAILURE.code){
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
            sysLogService.insert(1L, infoJson, e.getMessage(), "调用云米重送接口失败", String.format("orderNumber:%s,handleWay:%s",strOrderNumber,strDataType), "POST");
            return msResponse;
        }
    }
}
