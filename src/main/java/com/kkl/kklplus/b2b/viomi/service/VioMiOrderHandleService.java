package com.kkl.kklplus.b2b.viomi.service;

import com.google.common.collect.Lists;
import com.kkl.kklplus.b2b.viomi.entity.*;
import com.kkl.kklplus.b2b.viomi.http.request.*;
import com.kkl.kklplus.b2b.viomi.http.command.OperationCommand;
import com.kkl.kklplus.b2b.viomi.http.response.ResponseBody;
import com.kkl.kklplus.b2b.viomi.http.utils.OkHttpUtils;
import com.kkl.kklplus.b2b.viomi.mapper.VioMiOrderHandleMapper;
import com.kkl.kklplus.b2b.viomi.utils.GsonUtils;
import com.kkl.kklplus.b2b.viomi.utils.QuarterUtils;
import com.kkl.kklplus.b2b.viomi.utils.VioMiUtils;
import com.kkl.kklplus.common.exception.MSErrorCode;
import com.kkl.kklplus.common.response.MSResponse;
import com.kkl.kklplus.entity.b2b.common.B2BProcessFlag;
import com.kkl.kklplus.entity.b2bcenter.rpt.B2BOrderProcesslog;
import com.kkl.kklplus.entity.b2bcenter.sd.B2BOrderActionEnum;
import com.kkl.kklplus.entity.viomi.sd.VioMiApiLog;
import com.kkl.kklplus.entity.viomi.sd.VioMiKtServiceMessage;
import com.kkl.kklplus.entity.viomi.sd.VioMiOrderHandle;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.ObjectUtils;

import javax.annotation.Resource;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Service
@Slf4j
@Transactional(propagation = Propagation.NOT_SUPPORTED)
public class VioMiOrderHandleService {
    @Autowired
    private B2BProcesslogService b2BProcesslogService;

    @Autowired
    private SysLogService sysLogService;

    @Resource
    private VioMiOrderHandleMapper vioMiOrderHandleMapper;

    @Autowired
    private OrderInfoService orderInfoService;

    @Autowired
    private VioMiMessageLogService vioMiMessageLogService;

    @Autowired
    private ViomiApiLogService viomiApiLogService;

    /**
     * 验证工单办理的基本资料
     * @param vioMiOrderHandle
     * @return
     */
    private String validateOrderHandle(VioMiOrderHandle vioMiOrderHandle) {
        StringBuilder stringBuilder = new StringBuilder();
        if (vioMiOrderHandle.getB2bOrderId() == null) {
            stringBuilder.append("B2BOrderId不能为空.");
        }
        if (StringUtils.isBlank(vioMiOrderHandle.getOperator())){
            stringBuilder.append("操作人不能为空.");
        }
        if (vioMiOrderHandle.getCreateById() == null) {
            stringBuilder.append("createById不能为空.");
        }
        if (vioMiOrderHandle.getCreateDt() == null) {
            stringBuilder.append("createDt不能为空.");
        }
        if (vioMiOrderHandle.getStatus() == null) {
            stringBuilder.append("操作状态status不能为空.");
        }

        return stringBuilder.toString();
    }

    /**
     * 派单/转派
     * @param vioMiOrderHandle
     * @param flag 是否验证状态标志
     */
    @Transactional
    public MSResponse planing(VioMiOrderHandle vioMiOrderHandle,boolean flag) {
        String strMsg  = validateOrderHandle(vioMiOrderHandle);
        StringBuilder stringBuilder = new StringBuilder();
        if (StringUtils.isNotBlank(strMsg)) {
            stringBuilder.append(strMsg);
        }
        if (StringUtils.isBlank(vioMiOrderHandle.getEngineerName())) {
            stringBuilder.append("师傅姓名不能为空.");
        }
        if (StringUtils.isBlank(vioMiOrderHandle.getEngineerPhone())) {
            stringBuilder.append("师傅电话不能为空.");
        }
        if (stringBuilder.length() >0) {
            return new MSResponse(VioMiUtils.FAILURE_CODE, stringBuilder.toString(), null, null);
        }

        VioMiMessageLog vioMiMessageLog = vioMiMessageLogService.insert(vioMiOrderHandle.getCreateById(), vioMiOrderHandle.getCreateDt(), vioMiOrderHandle.getUniqueId());
        if (vioMiMessageLog.getProcessFlag().equals(VioMiMessageProcessFlagEnum.PROCESS_FLAG_FAILURE.value) ||
                vioMiMessageLog.getProcessFlag().equals(VioMiMessageProcessFlagEnum.PROCESS_FLAG_SUCCESS.value) ) {
            return new MSResponse(MSErrorCode.SUCCESS);
        }

        MSResponse msResponse;
        VioMiOrderInfo vioMiOrderInfo = orderInfoService.getOrderStatusByOrderId(vioMiOrderHandle.getB2bOrderId());
        boolean needPlaning = false;
        String  way = "";
        String  node = "";
        if(vioMiOrderInfo.getType().equals("退货")){
            // 退货
            needPlaning = true;
            way = VioMiWay.STATUS_SERVICE_POINT_ORDER;
            node = VioMiWay.STATUS_PLANING;
        } else if(vioMiOrderInfo.getType().equals("换货")){
            //换货单
            needPlaning = true;
            way = VioMiWay.STATUS_SERVICE_POINT_SEND_PACK;
            node = VioMiWay.STATUS_PLANING;
        } else if ((StringUtils.isBlank(vioMiOrderInfo.getViomiStatus())
                && StringUtils.isBlank(vioMiOrderInfo.getViomiSubStatus())) || flag ) {
            //上一次 way为空，node为空:为新工单
            needPlaning = true;
            way = VioMiWay.STATUS_PLANING;
            node = VioMiWay.STATUS_PLANING;
        } else if (vioMiOrderInfo.getViomiSubStatus().equals(VioMiWay.STATUS_PLANING) || vioMiOrderInfo.getViomiSubStatus().equals(VioMiWay.STATUS_REPLANING)) {
            //上一次 node：派送师傅 || 转派师傅
            needPlaning = true;
            way = VioMiWay.STATUS_APPOINTMENT;
            node = VioMiWay.STATUS_REPLANING;
        } else if (vioMiOrderInfo.getViomiSubStatus().equals(VioMiWay.STATUS_APPOINTMENT_SUCCESS) || vioMiOrderInfo.getViomiSubStatus().equals(VioMiWay.STATUS_REAPPOINTMENT)) {
            //上一次  node: 预约成功 || 重新预约
            needPlaning = true;
            way = VioMiWay.STATUS_CLOCK_IN_HOME;
            node = VioMiWay.STATUS_REPLANING;
        } else if (vioMiOrderInfo.getViomiStatus().equals(VioMiWay.STATUS_CLOCK_IN_HOME) && vioMiOrderInfo.getViomiSubStatus().equals(VioMiWay.STATUS_CLOCK_IN_SUCCESS)) {
            //上一次 way：上门打卡，node: 打卡成功
            needPlaning = true;
            way = VioMiWay.STATUS_PROCESS_COMPLETE;
            node = VioMiWay.STATUS_REPLANING;
        } else if(vioMiOrderInfo.getType().equals("退货")){
            // 退货
            needPlaning = true;
            way = VioMiWay.STATUS_SERVICE_POINT_ORDER;
            node = VioMiWay.STATUS_PLANING;
        } else if(vioMiOrderInfo.getType().equals("换货")){
            //换货单
            needPlaning = true;
            way = VioMiWay.STATUS_SERVICE_POINT_SEND_PACK;
            node = VioMiWay.STATUS_PLANING;
        }
        vioMiOrderHandle.setWay(way);
        vioMiOrderHandle.setNode(node);

        // 添加一个工单处理数据
        vioMiOrderHandle.setUpdateById(vioMiOrderHandle.getCreateById());
        vioMiOrderHandle.setUpdateDt(vioMiOrderHandle.getCreateDt());
        vioMiOrderHandle.setQuarter(QuarterUtils.getQuarter(vioMiOrderHandle.getCreateDt()));
        vioMiOrderHandle.setProcessFlag(0);
        vioMiOrderHandle.setProcessTime(0);
        vioMiOrderHandle.setOrderNumber(vioMiOrderInfo.getOrderNumber());
        int effectRowCount = vioMiOrderHandleMapper.insert(vioMiOrderHandle);
        if (effectRowCount >= 1) {
            /*
            if (!needPlaning) {
                String msg = String.format("不能符合派单条件id:%s,当前云米的状态way:%s,node:%s", vioMiOrderHandle.getB2bOrderId(), vioMiOrderInfo.getViomiStatus(), vioMiOrderInfo.getViomiSubStatus());
                return new MSResponse(VioMiUtils.FAILURE_CODE, msg, null, null);
            }
            */
            log.warn("{派单的云米状态:way:{},node:{},engineer_name:{},engineer_phone:{}", way, node, vioMiOrderHandle.getEngineerName(), vioMiOrderHandle.getEngineerPhone());

            OrderHandleRequestParam<EngineerParam> orderHandleRequestParam = new OrderHandleRequestParam<>();
            orderHandleRequestParam.setOrderNumber(vioMiOrderInfo.getOrderNumber());

            // handle
            HandleParam handleParam = new HandleParam();
            handleParam.setWay(vioMiOrderHandle.getWay());
            handleParam.setNode(vioMiOrderHandle.getNode());
            handleParam.setOperator(vioMiOrderHandle.getOperator());
            handleParam.setRemarks(vioMiOrderHandle.getRemarks());
            orderHandleRequestParam.setHandle(handleParam);

            // 师傅数据
            EngineerParam engineerParam = new EngineerParam();
            engineerParam.setEngineerName(vioMiOrderHandle.getEngineerName());
            engineerParam.setEngineerPhone(vioMiOrderHandle.getEngineerPhone());
            orderHandleRequestParam.setData(engineerParam);

            //*
            String infoJson = GsonUtils.getInstance().toUnderscoresJson(orderHandleRequestParam);
            if (!needPlaning) {
                if(!vioMiMessageLog.getDuplicateFlag()){
                    saveApiLog(vioMiOrderHandle, infoJson, 1);
                }

                String msg = String.format("不能符合派单条件id:%s,当前云米的状态way:%s,node:%s", vioMiOrderHandle.getB2bOrderId(), vioMiOrderInfo.getViomiStatus(), vioMiOrderInfo.getViomiSubStatus());
                return new MSResponse(VioMiUtils.FAILURE_CODE, msg, null, null);
            }
            //**

            msResponse = pushMessageToVioMi(orderHandleRequestParam, vioMiOrderHandle, vioMiMessageLog);
            if (msResponse.getCode() == 0 && msResponse.getThirdPartyErrorCode() == null) {
                // 更新订单状态
                VioMiOrderInfo updateVioMiOrderInfo = new VioMiOrderInfo();
                updateVioMiOrderInfo.setId(vioMiOrderInfo.getId());
                updateVioMiOrderInfo.setOrderStatus(vioMiOrderHandle.getStatus());
                updateVioMiOrderInfo.setViomiStatus(way);
                updateVioMiOrderInfo.setViomiSubStatus(node);
                //*
                updateVioMiOrderInfo.setApiExceptionStatus(0);
                updateVioMiOrderInfo.setFirstExceptionId(0L);
                //**
                updateVioMiOrderInfo.setUpdateById(vioMiOrderHandle.getUpdateById());
                updateVioMiOrderInfo.setUpdateDt(vioMiOrderHandle.getUpdateDt());
                orderInfoService.updateOrderStatus(updateVioMiOrderInfo);
            }
            return msResponse;
        } else {
            msResponse = new MSResponse(MSErrorCode.FAILURE);
            return msResponse;
        }
    }

    /**
     * 预约上门
     * @param vioMiOrderHandle
     * @return
     */
    @Transactional
    public MSResponse appointment(VioMiOrderHandle vioMiOrderHandle) {
        // 判断当前状态是否应当为预约上门
        String strMsg  = validateOrderHandle(vioMiOrderHandle);
        StringBuilder stringBuilder = new StringBuilder();
        if (StringUtils.isNotBlank(strMsg)) {
            stringBuilder.append(strMsg);
        }
        if (vioMiOrderHandle.getTimeOfAppointment() == null &&
           StringUtils.isBlank(vioMiOrderHandle.getEngineerName()) && StringUtils.isBlank(vioMiOrderHandle.getEngineerPhone())) {
            stringBuilder.append("预约时间，师傅姓名和师傅电话都为空.");
        }
        if (stringBuilder.length() >0) {
            return new MSResponse(VioMiUtils.FAILURE_CODE, stringBuilder.toString(), null, null);
        }

        VioMiMessageLog vioMiMessageLog = vioMiMessageLogService.insert(vioMiOrderHandle.getCreateById(), vioMiOrderHandle.getCreateDt(), vioMiOrderHandle.getUniqueId());
        if (vioMiMessageLog.getProcessFlag().equals(VioMiMessageProcessFlagEnum.PROCESS_FLAG_FAILURE.value) ||
                vioMiMessageLog.getProcessFlag().equals(VioMiMessageProcessFlagEnum.PROCESS_FLAG_SUCCESS.value) ) {
            return new MSResponse(MSErrorCode.SUCCESS);
        }

        MSResponse msResponse;
        VioMiOrderInfo vioMiOrderInfo = orderInfoService.getOrderStatusByOrderId(vioMiOrderHandle.getB2bOrderId());
        boolean needAppointment = false;
        String  way = "";
        String  node = "";
        /*
        if ((vioMiOrderInfo.getViomiStatus().equals(VioMiWay.STATUS_PLANING) && vioMiOrderInfo.getViomiSubStatus().equals(VioMiWay.STATUS_PLANING))   //上一次 way：派送师傅，node：派送师傅
               || (vioMiOrderInfo.getViomiStatus().equals(VioMiWay.STATUS_APPOINTMENT) && vioMiOrderInfo.getViomiSubStatus().equals(VioMiWay.STATUS_APPOINTMENT_FAILURE)) // //上一次 way：预约上门，node: 预约失败
               || (vioMiOrderInfo.getViomiStatus().equals(VioMiWay.STATUS_APPOINTMENT) && vioMiOrderInfo.getViomiSubStatus().equals(VioMiWay.STATUS_REPLANING)) //上一次 way：预约上门，node: 转派师傅
               || (vioMiOrderInfo.getViomiStatus().equals(VioMiWay.STATUS_CLOCK_IN_HOME) && vioMiOrderInfo.getViomiSubStatus().equals(VioMiWay.STATUS_REPLANING)) //上一次 way：上门打卡，node: 转派师傅
               || (vioMiOrderInfo.getViomiStatus().equals(VioMiWay.STATUS_PROCESS_COMPLETE) && vioMiOrderInfo.getViomiSubStatus().equals(VioMiWay.STATUS_REPLANING)) //上一次 way：处理完成，node: 转派师傅
               || (vioMiOrderInfo.getViomiStatus().equals(VioMiWay.STATUS_APPOINTMENT) && vioMiOrderInfo.getViomiSubStatus().equals(VioMiWay.STATUS_APPOINTMENT_SUCCESS))   //上一次 way：预约上门，node：预约成功
               || (vioMiOrderInfo.getViomiStatus().equals(VioMiWay.STATUS_CLOCK_IN_HOME) && vioMiOrderInfo.getViomiSubStatus().equals(VioMiWay.STATUS_REAPPOINTMENT))    //上一次 way：上门打卡，node: 重新预约
               || (vioMiOrderInfo.getViomiStatus().equals(VioMiWay.STATUS_PROCESS_COMPLETE) && vioMiOrderInfo.getViomiSubStatus().equals(VioMiWay.STATUS_REAPPOINTMENT))  //上一次 way：处理完成，node: 重新预约
            ) {
            needAppointment = true;
        }
        */

        if ( vioMiOrderInfo.getViomiSubStatus().equals(VioMiWay.STATUS_PLANING)    //上一次 node：派送师傅
                || vioMiOrderInfo.getViomiSubStatus().equals(VioMiWay.STATUS_REPLANING)  //上一次 node：转派师傅
                || vioMiOrderInfo.getViomiSubStatus().equals(VioMiWay.STATUS_APPOINTMENT_FAILURE)   //上一次 node: 预约失败
                || vioMiOrderInfo.getViomiSubStatus().equals(VioMiWay.STATUS_APPOINTMENT_SUCCESS)   //上一次  node：预约成功
                || vioMiOrderInfo.getViomiSubStatus().equals(VioMiWay.STATUS_REAPPOINTMENT)         //上一次  node: 重新预约
                || vioMiOrderInfo.getViomiSubStatus().equals(VioMiWay.STATUS_CLOCK_IN_SUCCESS)         //上一次  node: 打卡成功
                || vioMiOrderInfo.getType().equals("退货") //退货单
                || vioMiOrderInfo.getType().equals("换货") //换货单
        ) {
            needAppointment = true;
        }

        if(vioMiOrderInfo.getViomiStatus().equals(VioMiWay.STATUS_SERVICE_POINT_SEND_PACK) ||
                vioMiOrderInfo.getViomiStatus().equals(VioMiWay.STATUS_SERVICE_POINT_ORDER) ||
                vioMiOrderInfo.getViomiStatus().equals(VioMiWay.STATUS_ENGINEER_APPOINTMENT)){
            way = VioMiWay.STATUS_ENGINEER_APPOINTMENT;
            node = VioMiWay.STATUS_APPOINTMENT_SUCCESS;
        } else if(vioMiOrderInfo.getViomiSubStatus().equals(VioMiWay.STATUS_PLANING) || vioMiOrderInfo.getViomiSubStatus().equals(VioMiWay.STATUS_REPLANING)){
            way = VioMiWay.STATUS_APPOINTMENT;
            node = VioMiWay.STATUS_APPOINTMENT_SUCCESS;
        } else if (vioMiOrderInfo.getViomiSubStatus().equals(VioMiWay.STATUS_REAPPOINTMENT) || vioMiOrderInfo.getViomiSubStatus().equals(VioMiWay.STATUS_APPOINTMENT_SUCCESS)){
            way = VioMiWay.STATUS_CLOCK_IN_HOME;
            node = VioMiWay.STATUS_REAPPOINTMENT;
        } else if(StringUtils.isBlank(vioMiOrderInfo.getViomiSubStatus())){
            // 上次操作为空时，退货单、换货单才能预约
            if(vioMiOrderInfo.getType().equals("退货")){
                way = VioMiWay.STATUS_SERVICE_POINT_ORDER;
                node = VioMiWay.STATUS_APPOINTMENT;
            }else if(vioMiOrderInfo.getType().equals("换货")){
                way = VioMiWay.STATUS_SERVICE_POINT_SEND_PACK;
                node = VioMiWay.STATUS_APPOINTMENT;
            }
        }else {
            way = VioMiWay.STATUS_PROCESS_COMPLETE;
            node = VioMiWay.STATUS_REAPPOINTMENT;
        }
        // 预约失败，暂停工单 这两项功能暂不考虑

        vioMiOrderHandle.setWay(way);
        vioMiOrderHandle.setNode(node);

        // 添加一个工单处理数据
        vioMiOrderHandle.setUpdateById(vioMiOrderHandle.getCreateById());
        vioMiOrderHandle.setUpdateDt(vioMiOrderHandle.getCreateDt());
        vioMiOrderHandle.setQuarter(QuarterUtils.getQuarter(vioMiOrderHandle.getCreateDt()));
        vioMiOrderHandle.setProcessFlag(0);
        vioMiOrderHandle.setProcessTime(0);
        vioMiOrderHandle.setOrderNumber(vioMiOrderInfo.getOrderNumber());
        if (StringUtils.isBlank(vioMiOrderHandle.getEngineerName()) || StringUtils.isBlank(vioMiOrderHandle.getEngineerPhone())) {
            vioMiOrderHandle.setEngineerPhone("");
            vioMiOrderHandle.setEngineerName("");
        }
        int effectRowCount = vioMiOrderHandleMapper.insert(vioMiOrderHandle);
        if (effectRowCount >= 1) {
            /*
            if (!needAppointment) {
                String msg = String.format("不符合预约上门条件id:%s,当前云米的状态way:%s,node:%s", vioMiOrderHandle.getB2bOrderId(), vioMiOrderInfo.getViomiStatus(), vioMiOrderInfo.getViomiSubStatus());
                return new MSResponse(VioMiUtils.FAILURE_CODE, msg, null, null);
            }
            */

            OrderHandleRequestParam orderHandleRequestParam = null;

            if (vioMiOrderHandle.getTimeOfAppointment() != null) {
                //  预约成功
                orderHandleRequestParam = new OrderHandleRequestParam<AppointmentParam>();
                AppointmentParam appointmentParam = new AppointmentParam();
                String strDateFormat = "yyyy-MM-dd HH:mm:ss";
                SimpleDateFormat simpleDateFormat = new SimpleDateFormat(strDateFormat);
                String strTimeOfAppointment = simpleDateFormat.format(new Date(vioMiOrderHandle.getTimeOfAppointment()));
                log.warn("预约时间:{}", strTimeOfAppointment);
                appointmentParam.setTimeOfAppointment(strTimeOfAppointment);

                orderHandleRequestParam.setData(appointmentParam);
            } else if (StringUtils.isNotBlank(vioMiOrderHandle.getEngineerName()) && StringUtils.isNotBlank(vioMiOrderHandle.getEngineerPhone())) {
                // 转派师傅
                orderHandleRequestParam = new OrderHandleRequestParam<EngineerParam>();
                EngineerParam engineerParam = new EngineerParam();
                engineerParam.setEngineerName(vioMiOrderHandle.getEngineerName());
                engineerParam.setEngineerPhone(vioMiOrderHandle.getEngineerPhone());
                orderHandleRequestParam.setData(engineerParam);
            }
            orderHandleRequestParam.setOrderNumber(vioMiOrderInfo.getOrderNumber());

            // handle
            HandleParam handleParam = new HandleParam();
            handleParam.setWay(vioMiOrderHandle.getWay());
            handleParam.setNode(vioMiOrderHandle.getNode());
            handleParam.setOperator(vioMiOrderHandle.getOperator());
            handleParam.setRemarks(vioMiOrderHandle.getRemarks());
            orderHandleRequestParam.setHandle(handleParam);

            //*1323204463720136704
            String infoJson = GsonUtils.getInstance().toUnderscoresJson(orderHandleRequestParam);
            if (!needAppointment) {
                if (!vioMiMessageLog.getDuplicateFlag()) {
                    saveApiLog(vioMiOrderHandle, infoJson, 1);
                }
                String msg = String.format("不符合预约上门条件id:%s,当前云米的状态way:%s,node:%s", vioMiOrderHandle.getB2bOrderId(), vioMiOrderInfo.getViomiStatus(), vioMiOrderInfo.getViomiSubStatus());
                return new MSResponse(VioMiUtils.FAILURE_CODE, msg, null, null);
            }
            //**

            msResponse = pushMessageToVioMi(orderHandleRequestParam, vioMiOrderHandle, vioMiMessageLog);
            if (msResponse.getCode() == 0 && msResponse.getThirdPartyErrorCode() == null) {
                // 更新订单状态
                VioMiOrderInfo updateVioMiOrderInfo = new VioMiOrderInfo();
                updateVioMiOrderInfo.setId(vioMiOrderInfo.getId());
                updateVioMiOrderInfo.setOrderStatus(vioMiOrderHandle.getStatus());
                updateVioMiOrderInfo.setViomiStatus(way);
                updateVioMiOrderInfo.setViomiSubStatus(node);
                //*
                updateVioMiOrderInfo.setApiExceptionStatus(0);
                updateVioMiOrderInfo.setFirstExceptionId(0L);
                //**
                updateVioMiOrderInfo.setUpdateById(vioMiOrderHandle.getUpdateById());
                updateVioMiOrderInfo.setUpdateDt(vioMiOrderHandle.getUpdateDt());
                orderInfoService.updateOrderStatus(updateVioMiOrderInfo);
            }
            return msResponse;
        } else {
            msResponse = new MSResponse(MSErrorCode.FAILURE);
            return msResponse;
        }
    }

    /**
     * 上门打卡
     * @param vioMiOrderHandle
     * @return
     */
    public MSResponse clockInHome(VioMiOrderHandle vioMiOrderHandle) {
        String strMsg  = validateOrderHandle(vioMiOrderHandle);
        StringBuilder stringBuilder = new StringBuilder();
        if (StringUtils.isNotBlank(strMsg)) {
            stringBuilder.append(strMsg);
        }
        if (vioMiOrderHandle.getTimeOfAppointment() == null &&
                StringUtils.isBlank(vioMiOrderHandle.getEngineerName()) && StringUtils.isBlank(vioMiOrderHandle.getEngineerPhone()) &&
                StringUtils.isBlank(vioMiOrderHandle.getLocation()) ) {
            stringBuilder.append("预约时间，师傅姓名和师傅电话, 打卡地址经纬度都为空.");
        }
        if (stringBuilder.length() >0) {
            return new MSResponse(VioMiUtils.FAILURE_CODE, stringBuilder.toString(), null, null);
        }

        VioMiMessageLog vioMiMessageLog = vioMiMessageLogService.insert(vioMiOrderHandle.getCreateById(), vioMiOrderHandle.getCreateDt(), vioMiOrderHandle.getUniqueId());
        if (vioMiMessageLog.getProcessFlag().equals(VioMiMessageProcessFlagEnum.PROCESS_FLAG_FAILURE.value) ||
                vioMiMessageLog.getProcessFlag().equals(VioMiMessageProcessFlagEnum.PROCESS_FLAG_SUCCESS.value) ) {
            return new MSResponse(MSErrorCode.SUCCESS);
        }

        // 判断当前状态是否应当为上门打卡
        MSResponse msResponse;
        VioMiOrderInfo vioMiOrderInfo = orderInfoService.getOrderStatusByOrderId(vioMiOrderHandle.getB2bOrderId());
        boolean needClockInHome = false;
        String  way = "";
        String  node = "";
        if ((vioMiOrderInfo.getViomiStatus().equals(VioMiWay.STATUS_APPOINTMENT) && vioMiOrderInfo.getViomiSubStatus().equals(VioMiWay.STATUS_APPOINTMENT_SUCCESS))   //上一次 way：预约上门，node：预约成功
                || (vioMiOrderInfo.getViomiStatus().equals(VioMiWay.STATUS_CLOCK_IN_HOME) && vioMiOrderInfo.getViomiSubStatus().equals(VioMiWay.STATUS_REAPPOINTMENT)) // //上一次 way：上门打卡，node: 重新预约
                || (vioMiOrderInfo.getViomiStatus().equals(VioMiWay.STATUS_PROCESS_COMPLETE) && vioMiOrderInfo.getViomiSubStatus().equals(VioMiWay.STATUS_REAPPOINTMENT)) //上一次 way：处理完成，node: 重新预约
        ) {
            needClockInHome = true;
        }

        //if (StringUtils.isNotBlank(vioMiOrderHandle.getLocation())) {
            // 上门打卡,打卡成功
            way = VioMiWay.STATUS_CLOCK_IN_HOME;
            node = VioMiWay.STATUS_CLOCK_IN_SUCCESS;
        //}

        vioMiOrderHandle.setWay(way);
        vioMiOrderHandle.setNode(node);

        // 添加一个工单处理数据
        vioMiOrderHandle.setUpdateById(vioMiOrderHandle.getCreateById());
        vioMiOrderHandle.setUpdateDt(vioMiOrderHandle.getCreateDt());
        vioMiOrderHandle.setQuarter(QuarterUtils.getQuarter(vioMiOrderHandle.getCreateDt()));
        vioMiOrderHandle.setProcessFlag(0);
        vioMiOrderHandle.setProcessTime(0);
        vioMiOrderHandle.setOrderNumber(vioMiOrderInfo.getOrderNumber());
        if (StringUtils.isBlank(vioMiOrderHandle.getEngineerName()) || StringUtils.isBlank(vioMiOrderHandle.getEngineerPhone())) {
            vioMiOrderHandle.setEngineerPhone("");
            vioMiOrderHandle.setEngineerName("");
        }
        int effectRowCount = vioMiOrderHandleMapper.insert(vioMiOrderHandle);
        if (effectRowCount >= 1) {
            /*
            if (!needClockInHome) {
                String msg = String.format("不符合上门打卡条件id:%s,当前云米的状态way:%s,node:%s", vioMiOrderHandle.getB2bOrderId(), vioMiOrderInfo.getViomiStatus(), vioMiOrderInfo.getViomiSubStatus());
                return new MSResponse(VioMiUtils.FAILURE_CODE, msg, null, null);
            }
            */

            OrderHandleRequestParam orderHandleRequestParam = null;

            if (vioMiOrderHandle.getTimeOfAppointment() != null) {
                //  重新预约
                orderHandleRequestParam = new OrderHandleRequestParam<AppointmentParam>();
                AppointmentParam appointmentParam = new AppointmentParam();
                String strDateFormat = "yyyy-MM-dd HH:mm:ss";
                SimpleDateFormat simpleDateFormat = new SimpleDateFormat(strDateFormat);
                String strTimeOfAppointment = simpleDateFormat.format(new Date(vioMiOrderHandle.getTimeOfAppointment()));
                log.warn("预约时间:{}", strTimeOfAppointment);
                appointmentParam.setTimeOfAppointment(strTimeOfAppointment);

                orderHandleRequestParam.setData(appointmentParam);
            } else if (StringUtils.isNotBlank(vioMiOrderHandle.getEngineerName()) && StringUtils.isNotBlank(vioMiOrderHandle.getEngineerPhone())) {
                // 转派师傅
                orderHandleRequestParam = new OrderHandleRequestParam<EngineerParam>();
                EngineerParam engineerParam = new EngineerParam();
                engineerParam.setEngineerName(vioMiOrderHandle.getEngineerName());
                engineerParam.setEngineerPhone(vioMiOrderHandle.getEngineerPhone());
                orderHandleRequestParam.setData(engineerParam);
            } else if (StringUtils.isNotBlank(vioMiOrderHandle.getLocation())) {
                // 打卡成功
                orderHandleRequestParam = new OrderHandleRequestParam<LocationParam>();
                LocationParam locationParam = new LocationParam();
                locationParam.setLocation(vioMiOrderHandle.getLocation());
                orderHandleRequestParam.setData(locationParam);
            }
            orderHandleRequestParam.setOrderNumber(vioMiOrderInfo.getOrderNumber());

            // handle
            HandleParam handleParam = new HandleParam();
            handleParam.setWay(vioMiOrderHandle.getWay());
            handleParam.setNode(vioMiOrderHandle.getNode());
            handleParam.setOperator(vioMiOrderHandle.getOperator());
            handleParam.setRemarks(vioMiOrderHandle.getRemarks());
            orderHandleRequestParam.setHandle(handleParam);

            //*
            String infoJson = GsonUtils.getInstance().toUnderscoresJson(orderHandleRequestParam);
            if (!needClockInHome) {
                if (!vioMiMessageLog.getDuplicateFlag()) {
                    saveApiLog(vioMiOrderHandle, infoJson, 1);
                }

                String msg = String.format("不符合上门打卡条件id:%s,当前云米的状态way:%s,node:%s", vioMiOrderHandle.getB2bOrderId(), vioMiOrderInfo.getViomiStatus(), vioMiOrderInfo.getViomiSubStatus());
                return new MSResponse(VioMiUtils.FAILURE_CODE, msg, null, null);
            }
            //**

            msResponse = pushMessageToVioMi(orderHandleRequestParam, vioMiOrderHandle, vioMiMessageLog);
            if (msResponse.getCode() == 0 && msResponse.getThirdPartyErrorCode() == null) {
                // 更新订单状态
                VioMiOrderInfo updateVioMiOrderInfo = new VioMiOrderInfo();
                updateVioMiOrderInfo.setId(vioMiOrderInfo.getId());
                updateVioMiOrderInfo.setOrderStatus(vioMiOrderHandle.getStatus());
                updateVioMiOrderInfo.setViomiStatus(way);
                updateVioMiOrderInfo.setViomiSubStatus(node);
                //*
                updateVioMiOrderInfo.setApiExceptionStatus(0);
                updateVioMiOrderInfo.setFirstExceptionId(0L);
                //**
                updateVioMiOrderInfo.setUpdateById(vioMiOrderHandle.getUpdateById());
                updateVioMiOrderInfo.setUpdateDt(vioMiOrderHandle.getUpdateDt());
                orderInfoService.updateOrderStatus(updateVioMiOrderInfo);
            }
            return msResponse;
        } else {
            msResponse = new MSResponse(MSErrorCode.FAILURE);
            return msResponse;
        }
    }

    /**
     * 处理完成
     * @param vioMiOrderHandle
     * @return
     */
    @Transactional
    public MSResponse processComplete(VioMiOrderHandle vioMiOrderHandle) {
        String strMsg  = validateOrderHandle(vioMiOrderHandle);
        StringBuilder stringBuilder = new StringBuilder();
        if (StringUtils.isNotBlank(strMsg)) {
            stringBuilder.append(strMsg);
        }

        MSResponse msResponse;
        VioMiOrderInfo vioMiOrderInfo = orderInfoService.getOrderStatusByOrderId(vioMiOrderHandle.getB2bOrderId());
        String type = vioMiOrderInfo.getType();
        String subType = vioMiOrderInfo.getSubType();

        // 验证数据 begin
        if (vioMiOrderHandle.getIsFault() != null && vioMiOrderHandle.getIsFault().equals("是")) { // 需转鉴定
            if (vioMiOrderHandle.getBuyDate() == null) {
                stringBuilder.append("购买日期不能为空.");
            }
            if (StringUtils.isBlank(vioMiOrderHandle.getMiSn())) {
                stringBuilder.append("产品SN码不能为空.");
            }
            if (StringUtils.isBlank(vioMiOrderHandle.getWorkerErrorDesc())) {
                stringBuilder.append("故障记录为空.");
            }
            if (StringUtils.isBlank(vioMiOrderHandle.getCheckValidateResult())) {
                stringBuilder.append("检验鉴定结果为空.");
            }
            if (StringUtils.isBlank(vioMiOrderHandle.getCheckValidateDetail())) {
                stringBuilder.append("检验鉴定结果详情为空.");
            }
            if (StringUtils.isBlank(vioMiOrderHandle.getPackValidate())) {
                stringBuilder.append("包装鉴定为空.");
            }
            if (StringUtils.isBlank(vioMiOrderHandle.getPackValidateDetail())) {
                stringBuilder.append("包装鉴定详情为空.");
            }
            if (StringUtils.isBlank(vioMiOrderHandle.getNetworkInfo())) {
                stringBuilder.append("网点信息为空.");
            }
            if (type.equals(VioMiOrderType.TYPE_REPAIR)) {
                if (StringUtils.isBlank(vioMiOrderHandle.getFaultType())) {  // 仅维修
                    stringBuilder.append("故障类型不能为空.");
                }
                if (ObjectUtils.isEmpty(vioMiOrderHandle.getAttachment())) {  //仅维修
                    stringBuilder.append(subType);
                    stringBuilder.append("照片不能为空.");
                }
            }
        } else if (StringUtils.isNotBlank(vioMiOrderHandle.getEngineerName()) && StringUtils.isNotBlank(vioMiOrderHandle.getEngineerPhone())) {
            // 转派师傅
        } else if (vioMiOrderHandle.getTimeOfAppointment() != null && vioMiOrderHandle.getTimeOfAppointment() >0) {
            // 重新预约
        } else {
            // 安装完成或维修完成， 上门检测
            if (vioMiOrderHandle.getBuyDate() == null) {
                stringBuilder.append("购买日期不能为空.");
            }
            if (StringUtils.isBlank(vioMiOrderHandle.getMiSn())) {
                stringBuilder.append("产品SN码不能为空.");
            }
            if (ObjectUtils.isEmpty(vioMiOrderHandle.getAttachment())) {
                stringBuilder.append(subType);
                stringBuilder.append("照片不能为空.");
            }
            if (type.equals(VioMiOrderType.TYPE_REPAIR)) {  // 订单类型为维修
                if (StringUtils.isBlank(vioMiOrderHandle.getFaultType())) {  // 仅维修
                    stringBuilder.append("故障类型不能为空.");
                }
                if (StringUtils.isBlank(vioMiOrderHandle.getServiceMeasures())) {
                    stringBuilder.append("服务措施不能为空.");
                }
            }
        }
        if (stringBuilder.length() >0) {
            return new MSResponse(VioMiUtils.FAILURE_CODE, stringBuilder.toString(), null, null);
        }
        // 验证数据 end

        VioMiMessageLog vioMiMessageLog = vioMiMessageLogService.insert(vioMiOrderHandle.getCreateById(), vioMiOrderHandle.getCreateDt(), vioMiOrderHandle.getUniqueId());
        if (vioMiMessageLog.getProcessFlag().equals(VioMiMessageProcessFlagEnum.PROCESS_FLAG_FAILURE.value) ||
                vioMiMessageLog.getProcessFlag().equals(VioMiMessageProcessFlagEnum.PROCESS_FLAG_SUCCESS.value) ) {
            return new MSResponse(MSErrorCode.SUCCESS);
        }

        boolean needProcessComplete = false;
        String  way = "";
        String  node = "";

        if ((vioMiOrderInfo.getViomiStatus().equals(VioMiWay.STATUS_CLOCK_IN_HOME) && vioMiOrderInfo.getViomiSubStatus().equals(VioMiWay.STATUS_CLOCK_IN_SUCCESS))) {
            needProcessComplete = true;
        }

        way = VioMiWay.STATUS_PROCESS_COMPLETE;      // 处理完成
        CompleteParam completeParam = new CompleteParam();
        if (vioMiOrderHandle.getIsFault() != null && vioMiOrderHandle.getIsFault().equals("是")) {
            node = VioMiWay.STATUS_NEED_VALIDATE;    // 需转鉴定

            completeParam.setWorkerErrorDesc(vioMiOrderHandle.getWorkerErrorDesc());
            completeParam.setCheckValidateResult(vioMiOrderHandle.getCheckValidateResult());
            completeParam.setCheckValidateDetail(vioMiOrderHandle.getCheckValidateDetail());
            completeParam.setPackValidate(vioMiOrderHandle.getPackValidate());
            completeParam.setPackValidateDetail(vioMiOrderHandle.getPackValidateDetail());
            completeParam.setNetworkInfo(vioMiOrderHandle.getNetworkInfo());

            fillDataIfCompleteOrDetectOrValidate(completeParam, vioMiOrderHandle, type );
        } else if (StringUtils.isNotBlank(vioMiOrderHandle.getEngineerName()) && StringUtils.isNotBlank(vioMiOrderHandle.getEngineerPhone())) {
            // 转派师傅
            node = VioMiWay.STATUS_REPLANING;
            completeParam.setEngineerName(vioMiOrderHandle.getEngineerName());
            completeParam.setEngineerPhone(vioMiOrderHandle.getEngineerPhone());
        } else if (vioMiOrderHandle.getTimeOfAppointment() != null && vioMiOrderHandle.getTimeOfAppointment() >0) {
            // 重新预约
            node = VioMiWay.STATUS_REAPPOINTMENT;
            String strDateFormat = "yyyy-MM-dd HH:mm:ss";
            SimpleDateFormat simpleDateFormat = new SimpleDateFormat(strDateFormat);
            String strTimeOfAppointment = simpleDateFormat.format(new Date(vioMiOrderHandle.getTimeOfAppointment()));
            log.warn("和客户预约上门的时间:{}", strTimeOfAppointment);
            completeParam.setTimeOfAppointment(strTimeOfAppointment);
        } else {
            if (type.equals(VioMiOrderType.TYPE_INSTALL)) {
                if (subType.equals(VioMiOrderSubType.SUB_TYPE_INSTALL_BY_ORDER)) {// 报单安装
                    node = VioMiWay.STATUS_INSTALL_COMPLETE; //安装完成
                } else if (subType.equals(VioMiOrderSubType.SUB_TYPE_DETECT_IN_HOME) || subType.equals(VioMiOrderSubType.SUB_TYPE_MEASURING_IN_HOME)) {// 上门检测 或者上门测量
                    node = VioMiWay.STATUS_DETECT_IN_HOME;  //上门检测
                }
            } else if (type.equals(VioMiOrderType.TYPE_REPAIR)) {
                 if (subType.equals(VioMiOrderSubType.SUB_TYPE_REPAIR_BY_ORDER)) {// 报单维修
                     node = VioMiWay.STATUS_REPAIR_COMPLETE; // 维修完成
                 } else if (subType.equals(VioMiOrderSubType.SUB_TYPE_CHECK_IN_HOME)) {// 上门检查
                     node = VioMiWay.STATUS_DETECT_IN_HOME;  // 上门检测
                 }
            }

            fillDataIfCompleteOrDetectOrValidate(completeParam, vioMiOrderHandle, type );
        }

        if (vioMiOrderHandle.getPraiseSuccess() != null && vioMiOrderHandle.getPraiseSuccess().equals("是")) {
            List<String> praiseScreenShots = vioMiOrderHandle.getPraiseScreenshot();
            if (ObjectUtils.isEmpty(praiseScreenShots)) {
                stringBuilder.append("选择了好评成功，但好评照片数量为0.");
            }
            completeParam.setPraiseScreenshot(praiseScreenShots);

            vioMiOrderHandle.setPraiseScreenshotStr(GsonUtils.getInstance().toJson(praiseScreenShots));
        }
        completeParam.setPraiseSuccess(vioMiOrderHandle.getPraiseSuccess());

        vioMiOrderHandle.setWay(way);
        vioMiOrderHandle.setNode(node);

        // 添加一个工单处理数据
        vioMiOrderHandle.setUpdateById(vioMiOrderHandle.getCreateById());
        vioMiOrderHandle.setUpdateDt(vioMiOrderHandle.getCreateDt());
        vioMiOrderHandle.setQuarter(QuarterUtils.getQuarter(vioMiOrderHandle.getCreateDt()));
        vioMiOrderHandle.setProcessFlag(0);
        vioMiOrderHandle.setProcessTime(0);
        vioMiOrderHandle.setOrderNumber(vioMiOrderInfo.getOrderNumber());
        if (StringUtils.isBlank(vioMiOrderHandle.getEngineerName()) || StringUtils.isBlank(vioMiOrderHandle.getEngineerPhone())) {
            vioMiOrderHandle.setEngineerPhone("");
            vioMiOrderHandle.setEngineerName("");
        }
        if (!ObjectUtils.isEmpty(vioMiOrderHandle.getParts())) {
            vioMiOrderHandle.setPartsStr(GsonUtils.getInstance().toJson(vioMiOrderHandle.getParts()));
        }
        int effectRowCount = vioMiOrderHandleMapper.insert(vioMiOrderHandle);
        if (effectRowCount >= 1) {
            /*
            if (!needProcessComplete) {
                String msg = String.format("不符合处理完成条件id:%s,当前云米的状态way:%s,node:%s", vioMiOrderHandle.getB2bOrderId(), vioMiOrderInfo.getViomiStatus(), vioMiOrderInfo.getViomiSubStatus());
                return new MSResponse(VioMiUtils.FAILURE_CODE, msg, null, null);
            }
            */

            OrderHandleRequestParam<CompleteParam> orderHandleRequestParam = new OrderHandleRequestParam<>();
            orderHandleRequestParam.setData(completeParam);
            orderHandleRequestParam.setOrderNumber(vioMiOrderInfo.getOrderNumber());

            // handle
            HandleParam handleParam = new HandleParam();
            handleParam.setWay(vioMiOrderHandle.getWay());
            handleParam.setNode(vioMiOrderHandle.getNode());
            handleParam.setOperator(vioMiOrderHandle.getOperator());
            handleParam.setRemarks(vioMiOrderHandle.getRemarks());
            if (type.equals(VioMiOrderType.TYPE_REPAIR)) {  // 维修，附上更换的料号
                handleParam.setParts(vioMiOrderHandle.getParts());
            }
            orderHandleRequestParam.setHandle(handleParam);

            //*
            String infoJson = GsonUtils.getInstance().toUnderscoresJson(orderHandleRequestParam);
            if (!needProcessComplete) {
                if (!vioMiMessageLog.getDuplicateFlag()) {
                    saveApiLog(vioMiOrderHandle, infoJson, 1);
                }

                String msg = String.format("不符合处理完成条件id:%s,当前云米的状态way:%s,node:%s", vioMiOrderHandle.getB2bOrderId(), vioMiOrderInfo.getViomiStatus(), vioMiOrderInfo.getViomiSubStatus());
                return new MSResponse(VioMiUtils.FAILURE_CODE, msg, null, null);
            }
            //**

            msResponse = pushMessageToVioMi(orderHandleRequestParam, vioMiOrderHandle, vioMiMessageLog);
            if (msResponse.getCode() == 0 && msResponse.getThirdPartyErrorCode() == null) {
                // 更新订单状态
                VioMiOrderInfo updateVioMiOrderInfo = new VioMiOrderInfo();
                updateVioMiOrderInfo.setId(vioMiOrderInfo.getId());
                updateVioMiOrderInfo.setOrderStatus(vioMiOrderHandle.getStatus());
                updateVioMiOrderInfo.setViomiStatus(way);
                updateVioMiOrderInfo.setViomiSubStatus(node);
                //*
                updateVioMiOrderInfo.setApiExceptionStatus(0);
                updateVioMiOrderInfo.setFirstExceptionId(0L);
                //**
                updateVioMiOrderInfo.setUpdateById(vioMiOrderHandle.getUpdateById());
                updateVioMiOrderInfo.setUpdateDt(vioMiOrderHandle.getUpdateDt());
                orderInfoService.updateOrderStatus(updateVioMiOrderInfo);
            }
            return msResponse;
        } else {
            msResponse = new MSResponse(MSErrorCode.FAILURE);
            return msResponse;
        }
    }

    /**
     * 当为(安装/维修)完成,上门检测,重新鉴定填充数据
     * @param completeParam
     * @param orderHandle
     * @param orderType
     */
    private void fillDataIfCompleteOrDetectOrValidate(CompleteParam completeParam, VioMiOrderHandle orderHandle, String orderType) {
        // 购买时间
        if (orderHandle.getBuyDate() != null && orderHandle.getBuyDate() > 0) {
            String strDateFormat = "yyyy-MM-dd HH:mm:ss";
            SimpleDateFormat simpleDateFormat = new SimpleDateFormat(strDateFormat);
            String strTimeOfPurchase = simpleDateFormat.format(new Date(orderHandle.getBuyDate()));
            log.warn("购买时间:{}", strTimeOfPurchase);
            completeParam.setMiPurchaseTime(strTimeOfPurchase);
        }

        completeParam.setMiSn(orderHandle.getMiSn());
        if (orderType.equals(VioMiOrderType.TYPE_REPAIR)) {  //仅维修
            completeParam.setServiceMeasures(orderHandle.getServiceMeasures());
            completeParam.setFaultType(orderHandle.getFaultType());
        }
        // 维修完成/上门检测/需转鉴定并且产品为空调类时必填
        if (orderHandle.getKtSn() != null) {
            List<MiSnParam> miSnParams = Lists.newArrayList();
            List<VioMiKtServiceMessage> vioMiKtServiceMessages = Lists.newArrayList(orderHandle.getKtSn());
            for (VioMiKtServiceMessage ktsn : vioMiKtServiceMessages) {
                MiSnParam miSnParam = new MiSnParam();
                miSnParam.setMiSn(ktsn.getMiSn());
                if (orderType.equals(VioMiOrderType.TYPE_REPAIR)) { //仅维修
                    miSnParam.setServiceMeasures(ktsn.getServiceMeasures());
                    miSnParam.setIsFault(ktsn.getIsFault());
                    miSnParam.setFaultType(ktsn.getFaultType());
                }
                miSnParams.add(miSnParam);
            }
            completeParam.setKtSn(miSnParams);
            orderHandle.setKtSnStr(GsonUtils.getInstance().toJson(miSnParams));
        }

        List<String> attachmentList = orderHandle.getAttachment();
        List<String> aL = new ArrayList<>();
        for(String url : attachmentList){
            if(StringUtils.isNotBlank(url)){
                aL.add(url);
            }
        }
//        int size = aL.size();
//        // 补全为4个一组
//        if(size < 4){
//            int j = 4 - size;
//            for(int i = 0;i < j;i++){
//                aL.add("");
//            }
//        }
        if (!ObjectUtils.isEmpty(aL)) {
            completeParam.setAttachment(aL);
            orderHandle.setAttachmentStr(GsonUtils.getInstance().toJson(aL));
        }
    }

    /**
     * 申请完单
     * @param vioMiOrderHandle
     * @return
     */
    @Transactional
    public MSResponse applyFinished(VioMiOrderHandle vioMiOrderHandle) {
        String strMsg  = validateOrderHandle(vioMiOrderHandle);
        StringBuilder stringBuilder = new StringBuilder();
        if (StringUtils.isNotBlank(strMsg)) {
            stringBuilder.append(strMsg);
        }
        if (StringUtils.isBlank(vioMiOrderHandle.getVerifyCode())) {
            stringBuilder.append("验收码不能为空.");
        }
        if (stringBuilder.length() >0) {
            return new MSResponse(VioMiUtils.FAILURE_CODE, stringBuilder.toString(), null, null);
        }

        VioMiMessageLog vioMiMessageLog = vioMiMessageLogService.insert(vioMiOrderHandle.getCreateById(), vioMiOrderHandle.getCreateDt(), vioMiOrderHandle.getUniqueId());
        if (vioMiMessageLog.getProcessFlag().equals(VioMiMessageProcessFlagEnum.PROCESS_FLAG_FAILURE.value) ||
                vioMiMessageLog.getProcessFlag().equals(VioMiMessageProcessFlagEnum.PROCESS_FLAG_SUCCESS.value) ) {
            return new MSResponse(MSErrorCode.SUCCESS);
        }

        MSResponse msResponse;
        VioMiOrderInfo vioMiOrderInfo = orderInfoService.getOrderStatusByOrderId(vioMiOrderHandle.getB2bOrderId());
        boolean needApply = false;
        String  way = "";
        String  node = "";
//        if ((vioMiOrderInfo.getViomiStatus().equals(VioMiWay.STATUS_PROCESS_COMPLETE) && vioMiOrderInfo.getViomiSubStatus().equals(VioMiWay.STATUS_INSTALL_COMPLETE))   //上一次 way：处理完成，node：安装完成
//                || (vioMiOrderInfo.getViomiStatus().equals(VioMiWay.STATUS_PROCESS_COMPLETE) && vioMiOrderInfo.getViomiSubStatus().equals(VioMiWay.STATUS_REPAIR_COMPLETE)) //上一次 way：处理完成，node: 维修完成
//        ) {
//            needApply = true;
//        }
        needApply = true;
        if (needApply) {
            way = VioMiWay.STATUS_REPLY_FINISHED;  // 申请完单
            node = VioMiWay.STATUS_REPLY_FINISHED;
        }
        vioMiOrderHandle.setWay(way);
        vioMiOrderHandle.setNode(node);

        // 添加一个工单处理数据
        vioMiOrderHandle.setUpdateById(vioMiOrderHandle.getCreateById());
        vioMiOrderHandle.setUpdateDt(vioMiOrderHandle.getCreateDt());
        vioMiOrderHandle.setQuarter(QuarterUtils.getQuarter(vioMiOrderHandle.getCreateDt()));
        vioMiOrderHandle.setProcessFlag(0);
        vioMiOrderHandle.setProcessTime(0);
        vioMiOrderHandle.setOrderNumber(vioMiOrderInfo.getOrderNumber());
        if (StringUtils.isBlank(vioMiOrderHandle.getEngineerName()) || StringUtils.isBlank(vioMiOrderHandle.getEngineerPhone())) {
            vioMiOrderHandle.setEngineerPhone("");
            vioMiOrderHandle.setEngineerName("");
        }
        int effectRowCount = vioMiOrderHandleMapper.insert(vioMiOrderHandle);
        if (effectRowCount >= 1) {
            /*
            if (!needApply) {
                String msg = String.format("不符合申请完单条件id:%s,当前云米的状态way:%s,node:%s", vioMiOrderHandle.getB2bOrderId(), vioMiOrderInfo.getViomiStatus(), vioMiOrderInfo.getViomiSubStatus());
                return new MSResponse(VioMiUtils.FAILURE_CODE, msg, null, null);
            }
            */

            OrderHandleRequestParam<ApplyFinishedParam> orderHandleRequestParam = new OrderHandleRequestParam<>();
            ApplyFinishedParam applyFinishedParam = new ApplyFinishedParam();
            applyFinishedParam.setVerifyCode(vioMiOrderHandle.getVerifyCode());
            orderHandleRequestParam.setData(applyFinishedParam);
            orderHandleRequestParam.setOrderNumber(vioMiOrderInfo.getOrderNumber());

            // handle
            HandleParam handleParam = new HandleParam();
            handleParam.setWay(vioMiOrderHandle.getWay());
            handleParam.setNode(vioMiOrderHandle.getNode());
            handleParam.setOperator(vioMiOrderHandle.getOperator());
            handleParam.setRemarks(vioMiOrderHandle.getRemarks());
            orderHandleRequestParam.setHandle(handleParam);

            //*
            String infoJson = GsonUtils.getInstance().toUnderscoresJson(orderHandleRequestParam);
            if (!needApply) {
                saveApiLog(vioMiOrderHandle, infoJson, 1);

                String msg = String.format("不符合申请完单条件id:%s,当前云米的状态way:%s,node:%s", vioMiOrderHandle.getB2bOrderId(), vioMiOrderInfo.getViomiStatus(), vioMiOrderInfo.getViomiSubStatus());
                return new MSResponse(VioMiUtils.FAILURE_CODE, msg, null, null);
            }
            //**

            msResponse = pushMessageToVioMi(orderHandleRequestParam, vioMiOrderHandle, vioMiMessageLog);
            if (msResponse.getCode() == 0 && msResponse.getThirdPartyErrorCode() == null) {
                // 更新订单状态
                VioMiOrderInfo updateVioMiOrderInfo = new VioMiOrderInfo();
                updateVioMiOrderInfo.setId(vioMiOrderInfo.getId());
                updateVioMiOrderInfo.setOrderStatus(vioMiOrderHandle.getStatus());
                updateVioMiOrderInfo.setViomiStatus(way);
                updateVioMiOrderInfo.setViomiSubStatus(node);
                //*
                updateVioMiOrderInfo.setApiExceptionStatus(0);
                updateVioMiOrderInfo.setFirstExceptionId(0L);
                //**
                updateVioMiOrderInfo.setUpdateById(vioMiOrderHandle.getUpdateById());
                updateVioMiOrderInfo.setUpdateDt(vioMiOrderHandle.getUpdateDt());
                orderInfoService.updateOrderStatus(updateVioMiOrderInfo);
            }
            return msResponse;
        } else {
            msResponse = new MSResponse(MSErrorCode.FAILURE);
            return msResponse;
        }
    }

    /**
     * 工单回访
     * @param vioMiOrderHandle
     * @return
     */
    @Transactional
    public MSResponse orderReturnVisit(VioMiOrderHandle vioMiOrderHandle) {
        VioMiMessageLog vioMiMessageLog = vioMiMessageLogService.insert(vioMiOrderHandle.getCreateById(), vioMiOrderHandle.getCreateDt(), vioMiOrderHandle.getUniqueId());
        if (vioMiMessageLog.getProcessFlag().equals(VioMiMessageProcessFlagEnum.PROCESS_FLAG_FAILURE.value) ||
                vioMiMessageLog.getProcessFlag().equals(VioMiMessageProcessFlagEnum.PROCESS_FLAG_SUCCESS.value) ) {
            return new MSResponse(MSErrorCode.SUCCESS);
        }

        MSResponse msResponse;
        VioMiOrderInfo vioMiOrderInfo = orderInfoService.getOrderStatusByOrderId(vioMiOrderHandle.getB2bOrderId());
        String type = vioMiOrderInfo.getType();
        boolean needVisit = false;
        String  way = "";
        String  node = "";
//        if ((vioMiOrderInfo.getViomiStatus().equals(VioMiWay.STATUS_PROCESS_COMPLETE) && vioMiOrderInfo.getViomiSubStatus().equals(VioMiWay.STATUS_DETECT_IN_HOME))   //上一次 way：处理完成，node：上门检测
//                || (vioMiOrderInfo.getViomiStatus().equals(VioMiWay.STATUS_REPLY_FINISHED) && vioMiOrderInfo.getViomiSubStatus().equals(VioMiWay.STATUS_REPLY_FINISHED)) //上一次 way：申请完单，node: 申请完单
//        ) {
//            needVisit = true;
//        }
        needVisit = true;
        way = VioMiWay.STATUS_ORDER_RETURN_VISIT;  // 工单回访
        node = VioMiWay.STATUS_PLEASED_FINISHED;   //  满意完结
        vioMiOrderHandle.setWay(way);
        vioMiOrderHandle.setNode(node);

        // 添加一个工单处理数据
        vioMiOrderHandle.setUpdateById(vioMiOrderHandle.getCreateById());
        vioMiOrderHandle.setUpdateDt(vioMiOrderHandle.getCreateDt());
        vioMiOrderHandle.setQuarter(QuarterUtils.getQuarter(vioMiOrderHandle.getCreateDt()));
        vioMiOrderHandle.setProcessFlag(0);
        vioMiOrderHandle.setProcessTime(0);
        vioMiOrderHandle.setOrderNumber(vioMiOrderInfo.getOrderNumber());
        if (StringUtils.isBlank(vioMiOrderHandle.getEngineerName()) || StringUtils.isBlank(vioMiOrderHandle.getEngineerPhone())) {
            vioMiOrderHandle.setEngineerPhone("");
            vioMiOrderHandle.setEngineerName("");
        }
        int effectRowCount = vioMiOrderHandleMapper.insert(vioMiOrderHandle);
        if (effectRowCount >= 1) {
            /*
            if (!needVisit) {
                String msg = String.format("不符合工单回访条件id:%s,当前云米的状态way:%s,node:%s", vioMiOrderHandle.getB2bOrderId(), vioMiOrderInfo.getViomiStatus(), vioMiOrderInfo.getViomiSubStatus());
                return new MSResponse(VioMiUtils.FAILURE_CODE, msg, null, null);
            }
            */

            OrderHandleRequestParam<ReturnVisitParam> orderHandleRequestParam = new OrderHandleRequestParam<>();

            DecimalFormat df = new DecimalFormat("0.00");
            ReturnVisitParam returnVisitParam = new ReturnVisitParam();
            if (type.equals(VioMiOrderType.TYPE_INSTALL)) {  // 安装
                if (vioMiOrderHandle.getInstallationFee() != null && Math.abs(vioMiOrderHandle.getInstallationFee()) >0) {
                    returnVisitParam.setInstallationFee(df.format(vioMiOrderHandle.getInstallationFee()));
                }
            } else if (type.equals(VioMiOrderType.TYPE_REPAIR)) {
                if (vioMiOrderHandle.getMaintenanceCost() != null && Math.abs(vioMiOrderHandle.getMaintenanceCost()) >0) {
                    returnVisitParam.setMaintenanceCost(df.format(vioMiOrderHandle.getMaintenanceCost()));
                }
            }
            if (vioMiOrderHandle.getRemoteFee() != null && Math.abs(vioMiOrderHandle.getRemoteFee()) >0) {
                returnVisitParam.setRemoteFee(df.format(vioMiOrderHandle.getRemoteFee()));
            }
            if (vioMiOrderHandle.getDetectFee() != null && Math.abs(vioMiOrderHandle.getDetectFee()) >0) {
                returnVisitParam.setDetectFee(df.format(vioMiOrderHandle.getDetectFee()));
            }
            if (vioMiOrderHandle.getFineFee() != null && Math.abs(vioMiOrderHandle.getFineFee()) >0) {
                returnVisitParam.setFineFee(df.format(vioMiOrderHandle.getFineFee()));
            }
            if (vioMiOrderHandle.getMileage()!= null && Math.abs(vioMiOrderHandle.getMileage()) >0) {
                returnVisitParam.setMileage(df.format(vioMiOrderHandle.getMileage()));
            }

            orderHandleRequestParam.setData(returnVisitParam);
            orderHandleRequestParam.setOrderNumber(vioMiOrderInfo.getOrderNumber());

            // handle
            HandleParam handleParam = new HandleParam();
            handleParam.setWay(vioMiOrderHandle.getWay());
            handleParam.setNode(vioMiOrderHandle.getNode());
            handleParam.setOperator(vioMiOrderHandle.getOperator());
            handleParam.setRemarks(vioMiOrderHandle.getRemarks());
            orderHandleRequestParam.setHandle(handleParam);

            //*
            String infoJson = GsonUtils.getInstance().toUnderscoresJson(orderHandleRequestParam);
            if (!needVisit) {
                saveApiLog(vioMiOrderHandle, infoJson, 1);

                String msg = String.format("不符合工单回访条件id:%s,当前云米的状态way:%s,node:%s", vioMiOrderHandle.getB2bOrderId(), vioMiOrderInfo.getViomiStatus(), vioMiOrderInfo.getViomiSubStatus());
                return new MSResponse(VioMiUtils.FAILURE_CODE, msg, null, null);
            }
            //**

            msResponse = pushMessageToVioMi(orderHandleRequestParam, vioMiOrderHandle, vioMiMessageLog);
            if (msResponse.getCode() == 0 && msResponse.getThirdPartyErrorCode() == null) {
                // 更新订单状态
                VioMiOrderInfo updateVioMiOrderInfo = new VioMiOrderInfo();
                updateVioMiOrderInfo.setId(vioMiOrderInfo.getId());
                updateVioMiOrderInfo.setOrderStatus(vioMiOrderHandle.getStatus());
                updateVioMiOrderInfo.setViomiStatus(way);
                updateVioMiOrderInfo.setViomiSubStatus(node);
                //*
                updateVioMiOrderInfo.setApiExceptionStatus(0);
                updateVioMiOrderInfo.setFirstExceptionId(0L);
                //**
                updateVioMiOrderInfo.setUpdateById(vioMiOrderHandle.getUpdateById());
                updateVioMiOrderInfo.setUpdateDt(vioMiOrderHandle.getUpdateDt());
                orderInfoService.updateOrderStatus(updateVioMiOrderInfo);
            }
            return msResponse;
        } else {
            msResponse = new MSResponse(MSErrorCode.FAILURE);
            return msResponse;
        }
    }

    /**
     * 推送工单办理消息给云米
     * @param orderHandleRequestParam
     * @param createById
     * @param createDt
     * @param orderHandleId
     * @return
     */
    @Deprecated   // 此方法要删除
    public MSResponse pushMessageToVioMi(OrderHandleRequestParam orderHandleRequestParam, Long createById, Long createDt, Long orderHandleId, VioMiMessageLog vioMiMessageLog) {
        // 获取工单状态
        String strDataType = orderHandleRequestParam.getHandle().getWay();
        String strOrderNumber = orderHandleRequestParam.getOrderNumber();

        MSResponse msResponse = new MSResponse();
        msResponse.setErrorCode(MSErrorCode.SUCCESS);

        // 生成发送Log
        B2BOrderProcesslog b2BProcesslog = new B2BOrderProcesslog();
        b2BProcesslog.setDataType(strDataType);
        b2BProcesslog.setInterfaceName(OperationCommand.OperationCode.ORDER_HANDLE.apiUrl);
        b2BProcesslog.setProcessFlag(B2BProcessFlag.PROCESS_FLAG_PROCESSING.value);
        b2BProcesslog.setProcessTime(0);
        b2BProcesslog.setCreateById(createById);
        b2BProcesslog.setUpdateById(createById);
        b2BProcesslog.preInsert();
        b2BProcesslog.setQuarter(QuarterUtils.getQuarter(b2BProcesslog.getCreateDt()));

        // 更新OrderHandle
        VioMiOrderHandle vioMiOrderHandle = new VioMiOrderHandle();
        vioMiOrderHandle.setId(orderHandleId);
        vioMiOrderHandle.setUpdateById(createById);
        vioMiOrderHandle.setUpdateDt(createDt);
        vioMiOrderHandle.setQuarter(QuarterUtils.getQuarter(vioMiOrderHandle.getUpdateDate()));
        vioMiOrderHandle.setProcessTime(0);

        // 生成要发送的json对象
        String infoJson = GsonUtils.getInstance().toUnderscoresJson(orderHandleRequestParam);
        log.warn("发送给云米的数据:{}", infoJson);
        b2BProcesslog.setInfoJson(infoJson);
        try {
            b2BProcesslogService.insert(b2BProcesslog);
            OperationCommand command = OperationCommand.newInstance(OperationCommand.OperationCode.ORDER_HANDLE, orderHandleRequestParam);
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
                    vioMiMessageLog.setProcessFlag(VioMiMessageProcessFlagEnum.PROCESS_FLAG_SUCCESS.value);
                    vioMiMessageLogService.updateProcessFlag(vioMiMessageLog);

                    b2BProcesslog.setProcessFlag(B2BProcessFlag.PROCESS_FLAG_SUCESS.value);
                    b2BProcesslogService.updateProcessFlag(b2BProcesslog);

                    vioMiOrderHandle.setProcessFlag(B2BProcessFlag.PROCESS_FLAG_SUCESS.value);
                    vioMiOrderHandle.setProcessComment(data.getErrorMsg());
                    vioMiOrderHandleMapper.updateProcessFlag(vioMiOrderHandle);
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

            vioMiOrderHandle.setProcessFlag(B2BProcessFlag.PROCESS_FLAG_FAILURE.value);
            vioMiOrderHandle.setProcessComment(errorStr);
            vioMiOrderHandleMapper.updateProcessFlag(vioMiOrderHandle);

            return msResponse;
        } catch (Exception e) {
            String errorMsg = StringUtils.left(e.getMessage(), 255);
            msResponse.setErrorCode(MSErrorCode.FAILURE);
            msResponse.setMsg(errorMsg);
            b2BProcesslog.setProcessFlag(B2BProcessFlag.PROCESS_FLAG_FAILURE.value);
            b2BProcesslog.setProcessComment(errorMsg);
            b2BProcesslogService.updateProcessFlag(b2BProcesslog);
            log.error("errorStr:{}", e.getMessage());
            sysLogService.insert(1L, infoJson, e.getMessage(), "调用云米工单处理接口失败", String.format("orderNumber:%s,handleWay:%s",strOrderNumber,strDataType), "POST");
            return msResponse;
        }
    }

    /**
     * 推送工单办理消息给云米
     * @param orderHandleRequestParam
     * @param pastVioMiOrderHandle
     * @param vioMiMessageLog
     * @return
     */
    public MSResponse pushMessageToVioMi(OrderHandleRequestParam orderHandleRequestParam, VioMiOrderHandle pastVioMiOrderHandle, VioMiMessageLog vioMiMessageLog) {
        // 获取工单状态
        Long createById = pastVioMiOrderHandle.getCreateById();
        Long createDt = pastVioMiOrderHandle.getCreateDt();
        Long orderHandleId = pastVioMiOrderHandle.getId();

        String strDataType = orderHandleRequestParam.getHandle().getWay();
        String strOrderNumber = orderHandleRequestParam.getOrderNumber();

        MSResponse msResponse = new MSResponse();
        msResponse.setErrorCode(MSErrorCode.SUCCESS);

        // 生成发送Log
        B2BOrderProcesslog b2BProcesslog = new B2BOrderProcesslog();
        b2BProcesslog.setDataType(strDataType);
        b2BProcesslog.setInterfaceName(OperationCommand.OperationCode.ORDER_HANDLE.apiUrl);
        b2BProcesslog.setProcessFlag(B2BProcessFlag.PROCESS_FLAG_PROCESSING.value);
        b2BProcesslog.setProcessTime(0);
        b2BProcesslog.setCreateById(createById);
        b2BProcesslog.setUpdateById(createById);
        b2BProcesslog.preInsert();
        b2BProcesslog.setQuarter(QuarterUtils.getQuarter(b2BProcesslog.getCreateDt()));

        // 更新OrderHandle
        VioMiOrderHandle vioMiOrderHandle = new VioMiOrderHandle();
        vioMiOrderHandle.setId(orderHandleId);
        vioMiOrderHandle.setUpdateById(createById);
        vioMiOrderHandle.setUpdateDt(createDt);
        vioMiOrderHandle.setQuarter(QuarterUtils.getQuarter(vioMiOrderHandle.getUpdateDate()));
        vioMiOrderHandle.setProcessTime(0);

        // 生成要发送的json对象
        String infoJson = GsonUtils.getInstance().toUnderscoresJson(orderHandleRequestParam);
        log.warn("发送给云米的数据:{}", infoJson);
        b2BProcesslog.setInfoJson(infoJson);
        try {
            b2BProcesslogService.insert(b2BProcesslog);
            OperationCommand command = OperationCommand.newInstance(OperationCommand.OperationCode.ORDER_HANDLE, orderHandleRequestParam);
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
                    vioMiMessageLog.setProcessFlag(VioMiMessageProcessFlagEnum.PROCESS_FLAG_SUCCESS.value);
                    vioMiMessageLogService.updateProcessFlag(vioMiMessageLog);

                    saveApiLog(pastVioMiOrderHandle, infoJson, 0, resBody.getOriginalJson(), data.getErrorMsg(), B2BProcessFlag.PROCESS_FLAG_SUCESS.value);

                    b2BProcesslog.setProcessFlag(B2BProcessFlag.PROCESS_FLAG_SUCESS.value);
                    b2BProcesslogService.updateProcessFlag(b2BProcesslog);

                    vioMiOrderHandle.setProcessFlag(B2BProcessFlag.PROCESS_FLAG_SUCESS.value);
                    vioMiOrderHandle.setProcessComment(data.getErrorMsg());
                    vioMiOrderHandleMapper.updateProcessFlag(vioMiOrderHandle);
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

            if(!vioMiMessageLog.getDuplicateFlag() || errorCode < ResponseBody.ErrorCode.REQUEST_INVOCATION_FAILURE.code) {
                saveApiLog(pastVioMiOrderHandle, infoJson, 1, resBody.getOriginalJson(), errorStr, B2BProcessFlag.PROCESS_FLAG_FAILURE.value);
            }
            b2BProcesslogService.updateProcessFlag(b2BProcesslog);

            vioMiOrderHandle.setProcessFlag(B2BProcessFlag.PROCESS_FLAG_FAILURE.value);
            vioMiOrderHandle.setProcessComment(errorStr);
            vioMiOrderHandleMapper.updateProcessFlag(vioMiOrderHandle);

            return msResponse;
        } catch (Exception e) {
            String errorMsg = StringUtils.left(e.getMessage(), 255);
            msResponse.setErrorCode(MSErrorCode.FAILURE);
            msResponse.setMsg(errorMsg);
            b2BProcesslog.setProcessFlag(B2BProcessFlag.PROCESS_FLAG_FAILURE.value);
            b2BProcesslog.setProcessComment(errorMsg);
            b2BProcesslogService.updateProcessFlag(b2BProcesslog);
            log.error("errorStr:{}", e.getMessage());
            sysLogService.insert(1L, infoJson, e.getMessage(), "调用云米工单处理接口失败", String.format("orderNumber:%s,handleWay:%s",strOrderNumber,strDataType), "POST");
            return msResponse;
        }
    }

    public MSResponse<Integer> orderNeedValidate(VioMiOrderHandle vioMiOrderHandle) {
        String strMsg  = validateOrderHandle(vioMiOrderHandle);
        StringBuilder stringBuilder = new StringBuilder();
        if (StringUtils.isNotBlank(strMsg)) {
            stringBuilder.append(strMsg);
        }

        MSResponse msResponse;
        VioMiOrderInfo vioMiOrderInfo = orderInfoService.getOrderStatusByOrderId(vioMiOrderHandle.getB2bOrderId());
        String type = vioMiOrderInfo.getType();
        String subType = vioMiOrderInfo.getSubType();

        // 验证数据 begin
        if (vioMiOrderHandle.getBuyDate() == null) {
            stringBuilder.append("购买日期不能为空.");
        }
        if (StringUtils.isBlank(vioMiOrderHandle.getMiSn())) {
            stringBuilder.append("产品SN码不能为空.");
        }
        if (StringUtils.isBlank(vioMiOrderHandle.getCheckValidateResult())) {
            stringBuilder.append("检验鉴定结果为空.");
        }
        if (StringUtils.isBlank(vioMiOrderHandle.getCheckValidateDetail())) {
            stringBuilder.append("检验鉴定结果详情为空.");
        }
        if (StringUtils.isBlank(vioMiOrderHandle.getPackValidate())) {
            stringBuilder.append("包装鉴定为空.");
        }
        if (StringUtils.isBlank(vioMiOrderHandle.getPackValidateDetail())) {
            stringBuilder.append("包装鉴定详情为空.");
        }
        if (StringUtils.isBlank(vioMiOrderHandle.getNetworkInfo())) {
            stringBuilder.append("网点信息为空.");
        }
        if (ObjectUtils.isEmpty(vioMiOrderHandle.getAttachment())) {
            stringBuilder.append(subType);
            stringBuilder.append("照片不能为空.");
        }
        if (StringUtils.isBlank(vioMiOrderHandle.getIsFault())){
            stringBuilder.append("需转鉴定是否故障不能为空.");
        }else{
            if(vioMiOrderHandle.getIsFault().equals("是")){
                if(StringUtils.isBlank(vioMiOrderHandle.getWorkerErrorDesc())){
                    stringBuilder.append("是故障时，描述不能为空.");
                }
            }
        }
        if (StringUtils.isBlank(vioMiOrderHandle.getFaultType())) {
            stringBuilder.append("故障类型不能为空.");
        }
        if (stringBuilder.length() >0) {
            return new MSResponse(VioMiUtils.FAILURE_CODE, stringBuilder.toString(), null, null);
        }
        // 验证数据 end

        VioMiMessageLog vioMiMessageLog = vioMiMessageLogService.insert(vioMiOrderHandle.getCreateById(), vioMiOrderHandle.getCreateDt(), vioMiOrderHandle.getUniqueId());
        if (vioMiMessageLog.getProcessFlag().equals(VioMiMessageProcessFlagEnum.PROCESS_FLAG_FAILURE.value) ||
                vioMiMessageLog.getProcessFlag().equals(VioMiMessageProcessFlagEnum.PROCESS_FLAG_SUCCESS.value) ) {
            return new MSResponse(MSErrorCode.SUCCESS);
        }

        boolean needProcessComplete = false;
        String  way = VioMiWay.STATUS_PROCESS_COMPLETE;    // 需转鉴定
        String  node = VioMiWay.STATUS_NEED_VALIDATE;    // 需转鉴定

        if ((vioMiOrderInfo.getViomiStatus().equals(VioMiWay.STATUS_CLOCK_IN_HOME) && vioMiOrderInfo.getViomiSubStatus().equals(VioMiWay.STATUS_CLOCK_IN_SUCCESS))) {
            needProcessComplete = true;
        }
        CompleteParam completeParam = new CompleteParam();

        completeParam.setIsFault(vioMiOrderHandle.getIsFault());
        completeParam.setCheckValidateResult(vioMiOrderHandle.getCheckValidateResult());
        completeParam.setCheckValidateDetail(vioMiOrderHandle.getCheckValidateDetail());
        completeParam.setPackValidate(vioMiOrderHandle.getPackValidate());
        completeParam.setPackValidateDetail(vioMiOrderHandle.getPackValidateDetail());
        completeParam.setNetworkInfo(vioMiOrderHandle.getNetworkInfo());
        if (vioMiOrderHandle.getIsFault() != null && vioMiOrderHandle.getIsFault().equals("是")) {
            completeParam.setWorkerErrorDesc(vioMiOrderHandle.getWorkerErrorDesc());
        }
        fillDataIfCompleteOrDetectOrValidate(completeParam, vioMiOrderHandle, type);
        completeParam.setServiceMeasures(vioMiOrderHandle.getServiceMeasures());
        completeParam.setFaultType(vioMiOrderHandle.getFaultType());

        vioMiOrderHandle.setWay(way);
        vioMiOrderHandle.setNode(node);
        // 添加一个工单处理数据
        vioMiOrderHandle.setUpdateById(vioMiOrderHandle.getCreateById());
        vioMiOrderHandle.setUpdateDt(vioMiOrderHandle.getCreateDt());
        vioMiOrderHandle.setQuarter(QuarterUtils.getQuarter(vioMiOrderHandle.getCreateDt()));
        vioMiOrderHandle.setProcessFlag(0);
        vioMiOrderHandle.setProcessTime(0);
        vioMiOrderHandle.setOrderNumber(vioMiOrderInfo.getOrderNumber());
        if (StringUtils.isBlank(vioMiOrderHandle.getEngineerName()) || StringUtils.isBlank(vioMiOrderHandle.getEngineerPhone())) {
            vioMiOrderHandle.setEngineerPhone("");
            vioMiOrderHandle.setEngineerName("");
        }
        int effectRowCount = vioMiOrderHandleMapper.insert(vioMiOrderHandle);
        if (effectRowCount >= 1) {
            /*
            if (!needProcessComplete) {
                String msg = String.format("不符合需转鉴定条件id:%s,当前云米的状态way:%s,node:%s", vioMiOrderHandle.getB2bOrderId(), vioMiOrderInfo.getViomiStatus(), vioMiOrderInfo.getViomiSubStatus());
                return new MSResponse(VioMiUtils.FAILURE_CODE, msg, null, null);
            }
            */
            OrderHandleRequestParam<CompleteParam> orderHandleRequestParam = new OrderHandleRequestParam<>();
            orderHandleRequestParam.setData(completeParam);
            orderHandleRequestParam.setOrderNumber(vioMiOrderInfo.getOrderNumber());
            // handle
            HandleParam handleParam = new HandleParam();
            handleParam.setWay(vioMiOrderHandle.getWay());
            handleParam.setNode(vioMiOrderHandle.getNode());
            handleParam.setOperator(vioMiOrderHandle.getOperator());
            handleParam.setRemarks(vioMiOrderHandle.getRemarks());
            orderHandleRequestParam.setHandle(handleParam);

            //*
            String infoJson = GsonUtils.getInstance().toUnderscoresJson(orderHandleRequestParam);
            if (!needProcessComplete) {
                if(!vioMiMessageLog.getDuplicateFlag()){
                    saveApiLog(vioMiOrderHandle, infoJson, 1);
                }
                String msg = String.format("不符合需转鉴定条件id:%s,当前云米的状态way:%s,node:%s", vioMiOrderHandle.getB2bOrderId(), vioMiOrderInfo.getViomiStatus(), vioMiOrderInfo.getViomiSubStatus());
                return new MSResponse(VioMiUtils.FAILURE_CODE, msg, null, null);
            }
            //**

            msResponse = pushMessageToVioMi(orderHandleRequestParam, vioMiOrderHandle, vioMiMessageLog);
            if (msResponse.getCode() == 0 && msResponse.getThirdPartyErrorCode() == null) {
                // 更新订单状态
                VioMiOrderInfo updateVioMiOrderInfo = new VioMiOrderInfo();
                updateVioMiOrderInfo.setId(vioMiOrderInfo.getId());
                updateVioMiOrderInfo.setOrderStatus(vioMiOrderHandle.getStatus());
                updateVioMiOrderInfo.setViomiStatus(way);
                updateVioMiOrderInfo.setViomiSubStatus(node);
                //*
                updateVioMiOrderInfo.setApiExceptionStatus(0);
                updateVioMiOrderInfo.setFirstExceptionId(0L);
                //**
                updateVioMiOrderInfo.setUpdateById(vioMiOrderHandle.getUpdateById());
                updateVioMiOrderInfo.setUpdateDt(vioMiOrderHandle.getUpdateDt());
                orderInfoService.updateOrderStatus(updateVioMiOrderInfo);
            }
            return msResponse;
        } else {
            msResponse = new MSResponse(MSErrorCode.FAILURE);
            return msResponse;
        }
    }

    public MSResponse<Integer> complainCompleted(VioMiOrderHandle vioMiOrderHandle) {
        String strMsg  = validateOrderHandle(vioMiOrderHandle);
        StringBuilder stringBuilder = new StringBuilder();
        if (StringUtils.isNotBlank(strMsg)) {
            stringBuilder.append(strMsg);
        }

        MSResponse msResponse;
        VioMiOrderInfo vioMiOrderInfo = orderInfoService.getOrderByOrderNumber(vioMiOrderHandle.getOrderNumber());
        if (stringBuilder.length() >0) {
            return new MSResponse(VioMiUtils.FAILURE_CODE, stringBuilder.toString(), null, null);
        }
        // 验证数据 end

        VioMiMessageLog vioMiMessageLog = vioMiMessageLogService.insert(vioMiOrderHandle.getCreateById(), vioMiOrderHandle.getCreateDt(), vioMiOrderHandle.getUniqueId());
        if (vioMiMessageLog.getProcessFlag().equals(VioMiMessageProcessFlagEnum.PROCESS_FLAG_FAILURE.value) ||
                vioMiMessageLog.getProcessFlag().equals(VioMiMessageProcessFlagEnum.PROCESS_FLAG_SUCCESS.value) ) {
            return new MSResponse(MSErrorCode.SUCCESS);
        }

        String way = VioMiWay.STATUS_PROCESS_COMPLETE;
        String node = VioMiWay.STATUS_PROCESS_COMPLETE;
        //投诉只需要图片
        CompleteParam completeParam = new CompleteParam();
        completeParam.setAttachment(vioMiOrderHandle.getAttachment());
        vioMiOrderHandle.setB2bOrderId(vioMiOrderInfo.getId());
        vioMiOrderHandle.setWay(way);
        vioMiOrderHandle.setNode(node);
        // 添加一个工单处理数据
        vioMiOrderHandle.setUpdateById(vioMiOrderHandle.getCreateById());
        vioMiOrderHandle.setUpdateDt(vioMiOrderHandle.getCreateDt());
        vioMiOrderHandle.setQuarter(QuarterUtils.getQuarter(vioMiOrderHandle.getCreateDt()));
        vioMiOrderHandle.setProcessFlag(0);
        vioMiOrderHandle.setProcessTime(0);
        vioMiOrderHandle.setOrderNumber(vioMiOrderInfo.getOrderNumber());
        if (StringUtils.isBlank(vioMiOrderHandle.getEngineerName()) || StringUtils.isBlank(vioMiOrderHandle.getEngineerPhone())) {
            vioMiOrderHandle.setEngineerPhone("");
            vioMiOrderHandle.setEngineerName("");
        }
        int effectRowCount = vioMiOrderHandleMapper.insert(vioMiOrderHandle);
        if (effectRowCount >= 1) {
            OrderHandleRequestParam<CompleteParam> orderHandleRequestParam = new OrderHandleRequestParam<>();
            orderHandleRequestParam.setData(completeParam);
            orderHandleRequestParam.setOrderNumber(vioMiOrderInfo.getOrderNumber());
            // handle
            HandleParam handleParam = new HandleParam();
            handleParam.setWay(vioMiOrderHandle.getWay());
            handleParam.setNode(vioMiOrderHandle.getNode());
            handleParam.setOperator(vioMiOrderHandle.getOperator());
            handleParam.setRemarks(vioMiOrderHandle.getRemarks());
            orderHandleRequestParam.setHandle(handleParam);
            msResponse = pushMessageToVioMi(orderHandleRequestParam, vioMiOrderHandle, vioMiMessageLog);
            MSErrorCode errorCode = msResponse.getThirdPartyErrorCode();
            if(errorCode != null){
                msResponse.setErrorCode(errorCode);
            }
            if (msResponse.getCode() == 0) {
                // 更新订单状态
                VioMiOrderInfo updateVioMiOrderInfo = new VioMiOrderInfo();
                updateVioMiOrderInfo.setId(vioMiOrderInfo.getId());
                updateVioMiOrderInfo.setOrderStatus(vioMiOrderHandle.getStatus());
                updateVioMiOrderInfo.setViomiStatus(way);
                updateVioMiOrderInfo.setViomiSubStatus(node);
                //*
                updateVioMiOrderInfo.setApiExceptionStatus(0);
                updateVioMiOrderInfo.setFirstExceptionId(0L);
                //**
                updateVioMiOrderInfo.setUpdateById(vioMiOrderHandle.getUpdateById());
                updateVioMiOrderInfo.setUpdateDt(vioMiOrderHandle.getUpdateDt());
                orderInfoService.updateOrderStatus(updateVioMiOrderInfo);
            }
            return msResponse;
        } else {
            msResponse = new MSResponse(MSErrorCode.FAILURE);
            return msResponse;
        }
    }

    public VioMiOrderHandle findLastPlan(Long b2bOrderId) {
        return vioMiOrderHandleMapper.findLastPlan(b2bOrderId);
    }

    public MSResponse<Integer> orderServicePointSend(VioMiOrderHandle vioMiOrderHandle) {
        String strMsg  = validateOrderHandle(vioMiOrderHandle);
        StringBuilder stringBuilder = new StringBuilder();
        if (StringUtils.isNotBlank(strMsg)) {
            stringBuilder.append(strMsg);
        }
        List<String> productPhone = vioMiOrderHandle.getBackProductPhone();
        if (productPhone == null || productPhone.size() <= 0) {
            stringBuilder.append("寄回产品照片不能为空.");
        }
        if (StringUtils.isBlank(vioMiOrderHandle.getBackProductLogisticsCompany())) {
            stringBuilder.append("寄回产品物流公司不能为空.");
        }
        if (StringUtils.isBlank(vioMiOrderHandle.getBackProductLogisticsNumber())) {
            stringBuilder.append("寄回产品物流单号为空.");
        }
        MSResponse msResponse;
        VioMiOrderInfo vioMiOrderInfo = orderInfoService.getOrderStatusByOrderId(vioMiOrderHandle.getB2bOrderId());
        if (stringBuilder.length() >0) {
            return new MSResponse(VioMiUtils.FAILURE_CODE, stringBuilder.toString(), null, null);
        }
        // 验证数据 end

        VioMiMessageLog vioMiMessageLog = vioMiMessageLogService.insert(vioMiOrderHandle.getCreateById(), vioMiOrderHandle.getCreateDt(), vioMiOrderHandle.getUniqueId());
        if (vioMiMessageLog.getProcessFlag().equals(VioMiMessageProcessFlagEnum.PROCESS_FLAG_FAILURE.value) ||
                vioMiMessageLog.getProcessFlag().equals(VioMiMessageProcessFlagEnum.PROCESS_FLAG_SUCCESS.value) ) {
            return new MSResponse(MSErrorCode.SUCCESS);
        }

        String way = VioMiWay.STATUS_SERVICE_POINT_SEND;
        String node = VioMiWay.STATUS_PROCESS_COMPLETE;
        //寄送产品参数
        CompleteParam completeParam = new CompleteParam();
        completeParam.setBackProductPhone(productPhone);
        completeParam.setBackProductLogisticsCompany(vioMiOrderHandle.getBackProductLogisticsCompany());
        completeParam.setBackProductLogisticsNumber(vioMiOrderHandle.getBackProductLogisticsNumber());

        vioMiOrderHandle.setB2bOrderId(vioMiOrderInfo.getId());
        vioMiOrderHandle.setWay(way);
        vioMiOrderHandle.setNode(node);
        // 添加一个工单处理数据
        vioMiOrderHandle.setUpdateById(vioMiOrderHandle.getCreateById());
        vioMiOrderHandle.setUpdateDt(vioMiOrderHandle.getCreateDt());
        vioMiOrderHandle.setQuarter(QuarterUtils.getQuarter(vioMiOrderHandle.getCreateDt()));
        vioMiOrderHandle.setProcessFlag(0);
        vioMiOrderHandle.setProcessTime(0);
        vioMiOrderHandle.setOrderNumber(vioMiOrderInfo.getOrderNumber());
        if (StringUtils.isBlank(vioMiOrderHandle.getEngineerName()) || StringUtils.isBlank(vioMiOrderHandle.getEngineerPhone())) {
            vioMiOrderHandle.setEngineerPhone("");
            vioMiOrderHandle.setEngineerName("");
        }
        vioMiOrderHandle.setBackProductPhoneStr(GsonUtils.getInstance().toJson(productPhone));
        int effectRowCount = vioMiOrderHandleMapper.insert(vioMiOrderHandle);
        if (effectRowCount >= 1) {
            OrderHandleRequestParam<CompleteParam> orderHandleRequestParam = new OrderHandleRequestParam<>();
            orderHandleRequestParam.setData(completeParam);
            orderHandleRequestParam.setOrderNumber(vioMiOrderInfo.getOrderNumber());
            // handle
            HandleParam handleParam = new HandleParam();
            handleParam.setWay(vioMiOrderHandle.getWay());
            handleParam.setNode(vioMiOrderHandle.getNode());
            handleParam.setOperator(vioMiOrderHandle.getOperator());
            handleParam.setRemarks(vioMiOrderHandle.getRemarks());
            orderHandleRequestParam.setHandle(handleParam);
            msResponse = pushMessageToVioMi(orderHandleRequestParam, vioMiOrderHandle, vioMiMessageLog);
            MSErrorCode errorCode = msResponse.getThirdPartyErrorCode();
            if(errorCode != null){
                msResponse.setErrorCode(errorCode);
            }
            if (msResponse.getCode() == 0) {
                // 更新订单状态
                VioMiOrderInfo updateVioMiOrderInfo = new VioMiOrderInfo();
                updateVioMiOrderInfo.setId(vioMiOrderInfo.getId());
                updateVioMiOrderInfo.setOrderStatus(vioMiOrderHandle.getStatus());
                updateVioMiOrderInfo.setViomiStatus(way);
                updateVioMiOrderInfo.setViomiSubStatus(node);
                //*
                updateVioMiOrderInfo.setApiExceptionStatus(0);
                updateVioMiOrderInfo.setFirstExceptionId(0L);
                //**
                updateVioMiOrderInfo.setUpdateById(vioMiOrderHandle.getUpdateById());
                updateVioMiOrderInfo.setUpdateDt(vioMiOrderHandle.getUpdateDt());
                orderInfoService.updateOrderStatus(updateVioMiOrderInfo);
            }
            return msResponse;
        } else {
            msResponse = new MSResponse(MSErrorCode.FAILURE);
            return msResponse;
        }
    }

    public MSResponse<Integer> orderDismounting(VioMiOrderHandle vioMiOrderHandle) {
        String strMsg  = validateOrderHandle(vioMiOrderHandle);
        StringBuilder stringBuilder = new StringBuilder();
        if (StringUtils.isNotBlank(strMsg)) {
            stringBuilder.append(strMsg);
        }
        if (vioMiOrderHandle.getAttachment() == null) {
            stringBuilder.append("拆机安装图片不能为空.");
        }
        if (StringUtils.isBlank(vioMiOrderHandle.getMiSn())) {
            stringBuilder.append("产品条码不能为空.");
        }
        MSResponse msResponse;
        VioMiOrderInfo vioMiOrderInfo = orderInfoService.getOrderStatusByOrderId(vioMiOrderHandle.getB2bOrderId());
        String type = vioMiOrderInfo.getType();
        if (type.equals("换货") && StringUtils.isBlank(vioMiOrderHandle.getOldSn())) {
            stringBuilder.append(type);
            stringBuilder.append("换货单旧条码不能为空.");
        }
        if (stringBuilder.length() >0) {
            return new MSResponse(VioMiUtils.FAILURE_CODE, stringBuilder.toString(), null, null);
        }
        // 验证数据 end

        VioMiMessageLog vioMiMessageLog = vioMiMessageLogService.insert(vioMiOrderHandle.getCreateById(), vioMiOrderHandle.getCreateDt(), vioMiOrderHandle.getUniqueId());
        if (vioMiMessageLog.getProcessFlag().equals(VioMiMessageProcessFlagEnum.PROCESS_FLAG_FAILURE.value) ||
                vioMiMessageLog.getProcessFlag().equals(VioMiMessageProcessFlagEnum.PROCESS_FLAG_SUCCESS.value) ) {
            return new MSResponse(MSErrorCode.SUCCESS);
        }

        String way = VioMiWay.STATUS_DISMOUNTING;
        String node = VioMiWay.STATUS_SEND_PACK_COMPLETE;
        if(type.equals("退货")){
            way = VioMiWay.STATUS_DISASSEMBLE;
            node = VioMiWay.STATUS_PICKUP_COMPLETE;
        }
        //退换货拆装参数
        CompleteParam completeParam = new CompleteParam();
        completeParam.setMiSn(vioMiOrderHandle.getMiSn());
        completeParam.setAttachment(vioMiOrderHandle.getAttachment());
        if(type.equals("换货")){
            completeParam.setOldSn(vioMiOrderHandle.getOldSn());
        }

        vioMiOrderHandle.setB2bOrderId(vioMiOrderInfo.getId());
        vioMiOrderHandle.setWay(way);
        vioMiOrderHandle.setNode(node);
        // 添加一个工单处理数据
        vioMiOrderHandle.setUpdateById(vioMiOrderHandle.getCreateById());
        vioMiOrderHandle.setUpdateDt(vioMiOrderHandle.getCreateDt());
        vioMiOrderHandle.setQuarter(QuarterUtils.getQuarter(vioMiOrderHandle.getCreateDt()));
        vioMiOrderHandle.setProcessFlag(0);
        vioMiOrderHandle.setProcessTime(0);
        vioMiOrderHandle.setOrderNumber(vioMiOrderInfo.getOrderNumber());
        vioMiOrderHandle.setEngineerPhone("");
        vioMiOrderHandle.setEngineerName("");
        int effectRowCount = vioMiOrderHandleMapper.insert(vioMiOrderHandle);
        if (effectRowCount >= 1) {
            OrderHandleRequestParam<CompleteParam> orderHandleRequestParam = new OrderHandleRequestParam<>();
            orderHandleRequestParam.setData(completeParam);
            orderHandleRequestParam.setOrderNumber(vioMiOrderInfo.getOrderNumber());
            // handle
            HandleParam handleParam = new HandleParam();
            handleParam.setWay(vioMiOrderHandle.getWay());
            handleParam.setNode(vioMiOrderHandle.getNode());
            handleParam.setOperator(vioMiOrderHandle.getOperator());
            handleParam.setRemarks(vioMiOrderHandle.getRemarks());
            orderHandleRequestParam.setHandle(handleParam);
            msResponse = pushMessageToVioMi(orderHandleRequestParam, vioMiOrderHandle, vioMiMessageLog);
            MSErrorCode errorCode = msResponse.getThirdPartyErrorCode();
            if(errorCode != null){
                msResponse.setErrorCode(errorCode);
            }
            if (msResponse.getCode() == 0) {
                // 更新订单状态
                VioMiOrderInfo updateVioMiOrderInfo = new VioMiOrderInfo();
                updateVioMiOrderInfo.setId(vioMiOrderInfo.getId());
                updateVioMiOrderInfo.setOrderStatus(vioMiOrderHandle.getStatus());
                updateVioMiOrderInfo.setViomiStatus(way);
                updateVioMiOrderInfo.setViomiSubStatus(node);
                //*
                updateVioMiOrderInfo.setApiExceptionStatus(0);
                updateVioMiOrderInfo.setFirstExceptionId(0L);
                //**
                updateVioMiOrderInfo.setUpdateById(vioMiOrderHandle.getUpdateById());
                updateVioMiOrderInfo.setUpdateDt(vioMiOrderHandle.getUpdateDt());
                orderInfoService.updateOrderStatus(updateVioMiOrderInfo);
            }
            return msResponse;
        } else {
            msResponse = new MSResponse(MSErrorCode.FAILURE);
            return msResponse;
        }
    }

    public MSResponse<Integer> orderConfirm(VioMiOrderHandle vioMiOrderHandle) {
        String strMsg  = validateOrderHandle(vioMiOrderHandle);
        StringBuilder stringBuilder = new StringBuilder();
        if (StringUtils.isNotBlank(strMsg)) {
            stringBuilder.append(strMsg);
        }
        MSResponse msResponse;
        VioMiOrderInfo vioMiOrderInfo = orderInfoService.getOrderStatusByOrderId(vioMiOrderHandle.getB2bOrderId());
        String type = vioMiOrderInfo.getType();
        if (stringBuilder.length() >0) {
            return new MSResponse(VioMiUtils.FAILURE_CODE, stringBuilder.toString(), null, null);
        }
        // 验证数据 end

        VioMiMessageLog vioMiMessageLog = vioMiMessageLogService.insert(vioMiOrderHandle.getCreateById(), vioMiOrderHandle.getCreateDt(), vioMiOrderHandle.getUniqueId());
        if (vioMiMessageLog.getProcessFlag().equals(VioMiMessageProcessFlagEnum.PROCESS_FLAG_FAILURE.value) ||
                vioMiMessageLog.getProcessFlag().equals(VioMiMessageProcessFlagEnum.PROCESS_FLAG_SUCCESS.value) ) {
            return new MSResponse(MSErrorCode.SUCCESS);
        }

        String way = VioMiWay.STATUS_CONFIRM_NEW_PRODUCT;
        String node = VioMiWay.STATUS_CONFIRM_ARRIVED;
        if(vioMiOrderHandle.getStatus()== B2BOrderActionEnum.CONVERTED_CANCEL.value){
            node = VioMiWay.STATUS_BACK_ORDER;
        }
        vioMiOrderHandle.setWay(way);
        vioMiOrderHandle.setNode(node);
        // 添加一个工单处理数据
        vioMiOrderHandle.setUpdateById(vioMiOrderHandle.getCreateById());
        vioMiOrderHandle.setUpdateDt(vioMiOrderHandle.getCreateDt());
        vioMiOrderHandle.setQuarter(QuarterUtils.getQuarter(vioMiOrderHandle.getCreateDt()));
        vioMiOrderHandle.setProcessFlag(0);
        vioMiOrderHandle.setProcessTime(0);
        vioMiOrderHandle.setOrderNumber(vioMiOrderInfo.getOrderNumber());
        vioMiOrderHandle.setEngineerPhone("");
        vioMiOrderHandle.setEngineerName("");
        int effectRowCount = vioMiOrderHandleMapper.insert(vioMiOrderHandle);
        if (effectRowCount >= 1) {
            OrderHandleRequestParam<CompleteParam> orderHandleRequestParam = new OrderHandleRequestParam<>();
            orderHandleRequestParam.setOrderNumber(vioMiOrderInfo.getOrderNumber());
            // handle
            HandleParam handleParam = new HandleParam();
            handleParam.setWay(vioMiOrderHandle.getWay());
            handleParam.setNode(vioMiOrderHandle.getNode());
            handleParam.setOperator(vioMiOrderHandle.getOperator());
            handleParam.setRemarks(vioMiOrderHandle.getRemarks());
            orderHandleRequestParam.setHandle(handleParam);
            msResponse = pushMessageToVioMi(orderHandleRequestParam, vioMiOrderHandle, vioMiMessageLog);
            MSErrorCode errorCode = msResponse.getThirdPartyErrorCode();
            if (msResponse.getCode() == 0 && errorCode == null) {
                // 更新订单状态
                VioMiOrderInfo updateVioMiOrderInfo = new VioMiOrderInfo();
                updateVioMiOrderInfo.setId(vioMiOrderInfo.getId());
                updateVioMiOrderInfo.setOrderStatus(vioMiOrderHandle.getStatus());
                updateVioMiOrderInfo.setViomiStatus(way);
                updateVioMiOrderInfo.setViomiSubStatus(node);
                //*
                updateVioMiOrderInfo.setApiExceptionStatus(0);
                updateVioMiOrderInfo.setFirstExceptionId(0L);
                //**
                updateVioMiOrderInfo.setUpdateById(vioMiOrderHandle.getUpdateById());
                updateVioMiOrderInfo.setUpdateDt(vioMiOrderHandle.getUpdateDt());
                orderInfoService.updateOrderStatus(updateVioMiOrderInfo);
            }
            return msResponse;
        } else {
            msResponse = new MSResponse(MSErrorCode.FAILURE);
            return msResponse;
        }
    }

    private void saveApiLog(VioMiOrderHandle vioMiOrderHandle, String infoJson, Integer bFail){
        saveApiLog(vioMiOrderHandle, infoJson, bFail, null, "不能符合请求条件", 0);
    }

    private void saveApiLog(VioMiOrderHandle vioMiOrderHandle, String infoJson, Integer bFail, String resultJson, String processComment, Integer processFlag) {
        final Integer API_EXCEPTION_STATUS_NORMAL = 0;   // 正常状态
        final Integer API_EXCEPTION_STATUS_ABNORMAL = 1;  // 异常状态

        VioMiApiLog vioMiApiLog = new VioMiApiLog();
        vioMiApiLog.setB2bOrderId(vioMiOrderHandle.getB2bOrderId());
        vioMiApiLog.setOperatingStatus(vioMiOrderHandle.getStatus());
        vioMiApiLog.setInterfaceName(OperationCommand.OperationCode.ORDER_HANDLE.apiUrl);
        vioMiApiLog.setInfoJson(infoJson);
        vioMiApiLog.setProcessFlag(processFlag);
        vioMiApiLog.setProcessTime(0);
        vioMiApiLog.setResultJson(resultJson);
        vioMiApiLog.setProcessComment(processComment);
        vioMiApiLog.setCreateBy(vioMiOrderHandle.getCreateById());
        vioMiApiLog.setUpdateBy(vioMiOrderHandle.getCreateById());
        long currentDate = System.currentTimeMillis();
        vioMiApiLog.setCreateDt(currentDate);
        vioMiApiLog.setUpdateDt(currentDate);
        vioMiApiLog.setQuarter(QuarterUtils.getQuarter(currentDate));

        viomiApiLogService.insert(vioMiApiLog);
        Long vioMiApiLogId = vioMiApiLog.getId();

        // 更新异常状态及首次异常日志中id
        if (bFail.equals(1)) {  //1-失败，0- 成功
            VioMiOrderInfo updateVioMiOrderInfo = new VioMiOrderInfo();
            updateVioMiOrderInfo.setId(vioMiOrderHandle.getB2bOrderId());
            updateVioMiOrderInfo.setApiExceptionStatus(API_EXCEPTION_STATUS_ABNORMAL);
            updateVioMiOrderInfo.setFirstExceptionId(vioMiApiLogId);
            updateVioMiOrderInfo.setUpdateById(vioMiOrderHandle.getUpdateById());
            updateVioMiOrderInfo.setUpdateDt(vioMiOrderHandle.getUpdateDt());

            orderInfoService.updateException(updateVioMiOrderInfo);
        }
    }
}
