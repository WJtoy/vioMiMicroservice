package com.kkl.kklplus.b2b.viomi.controller;

import com.kkl.kklplus.b2b.viomi.entity.response.VioMiResponse;
import com.kkl.kklplus.b2b.viomi.service.*;
import com.kkl.kklplus.b2b.viomi.utils.GsonUtils;
import com.kkl.kklplus.b2b.viomi.utils.QuarterUtils;
import com.kkl.kklplus.b2b.viomi.utils.VioMiUtils;
import com.kkl.kklplus.entity.b2b.common.B2BProcessFlag;
import com.kkl.kklplus.entity.b2bcenter.rpt.B2BOrderProcesslog;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

/**
 *第三方调用控制器
 * @author chenxj
 * @date 2020/09/18
 */
@Slf4j
@RestController
@RequestMapping("/b2b")
public class B2BPushController {

    @Autowired
    private OrderInfoService orderInfoService;

    @Autowired
    private OrderRemarkService orderRemarkService;

    @Autowired
    private OrderTechnicalAppraisalService orderTechnicalAppraisalService;

    @Autowired
    private OrderCancelService orderCancelService;

    @Autowired
    private OrderReminderService orderReminderService;

    @Autowired
    private B2BProcesslogService b2BProcesslogService;

    @ApiOperation("第三方派发工单")
    @PostMapping("createOrder")
    public VioMiResponse createOrder(HttpServletRequest req) throws IOException {
        String json = getRequestJson(req);
        try {
            B2BOrderProcesslog processlog = b2BProcesslogService.insertPushApiLog(json, "createOrder");
            VioMiResponse response = orderInfoService.createOrder(json);
            this.updateLog(response,processlog);
            return response;
        }catch (Exception e){
            log.error("第三方派发工单:{}",json,e);
            return new VioMiResponse(1,"处理异常");
        }
    }



    @ApiOperation("第三方工单备注")
    @PostMapping("remarkOrder")
    public VioMiResponse remarkOrder(HttpServletRequest req) throws IOException {
        String json = getRequestJson(req);
        try {
            B2BOrderProcesslog processlog = b2BProcesslogService.insertPushApiLog(json, "remarkOrder");
            VioMiResponse response = orderRemarkService.remarkOrder(json);
            this.updateLog(response,processlog);
            return response;
        }catch (Exception e){
            log.error("第三方工单备注:{}",json,e);
            return new VioMiResponse(1,"处理异常");
        }
    }




    @ApiOperation("第三方工单撤销")
    @PostMapping("cancelOrder")
    public VioMiResponse cancelOrder(HttpServletRequest req) throws IOException {
        String json = getRequestJson(req);
        try {
            B2BOrderProcesslog processlog = b2BProcesslogService.insertPushApiLog(json, "cancelOrder");
            VioMiResponse response = orderCancelService.cancelOrder(json);
            this.updateLog(response,processlog);
            return response;
        }catch (Exception e){
            log.error("第三方工单撤销:{}",json,e);
            return new VioMiResponse(1,"处理异常");
        }
    }

    @ApiOperation("第三方工单催单")
    @PostMapping("reminderOrder")
    public VioMiResponse reminderOrder(HttpServletRequest req) throws IOException {
        String json = getRequestJson(req);
        try {
            B2BOrderProcesslog processlog = b2BProcesslogService.insertPushApiLog(json, "reminderOrder");
            VioMiResponse response = orderReminderService.reminderOrder(json);
            this.updateLog(response,processlog);
            return response;
        }catch (Exception e){
            log.error("第三方工单催单:{}",json,e);
            return new VioMiResponse(1,"处理异常");
        }
    }

    @ApiOperation("第三方技术鉴定")
    @PostMapping("handleOrder")
    public VioMiResponse handleOrder(HttpServletRequest req) throws IOException {
        String json = getRequestJson(req);
        try {
            B2BOrderProcesslog processlog = b2BProcesslogService.insertPushApiLog(json, "handleOrder");
            VioMiResponse response = orderTechnicalAppraisalService.technicalAppraisal(json);
            this.updateLog(response,processlog);
            return response;
        }catch (Exception e){
            log.error("第三方技术鉴定:{}",json,e);
            return new VioMiResponse(1,"处理异常");
        }
    }

    private void updateLog(VioMiResponse response, B2BOrderProcesslog processlog) {
        int res = response.getRes();
        if(res == VioMiUtils.SUCCESS_CODE){
            processlog.setProcessFlag(B2BProcessFlag.PROCESS_FLAG_SUCESS.value);
        }else{
            processlog.setProcessFlag(B2BProcessFlag.PROCESS_FLAG_FAILURE.value);
            processlog.setProcessComment(response.getMsg());
        }
        processlog.setResultJson(GsonUtils.getInstance().toJson(response));
        b2BProcesslogService.updateProcessFlag(processlog);
    }
    private String getRequestJson(HttpServletRequest req) throws IOException {
        // 读取参数
        InputStream inputStream;
        StringBuffer sb = new StringBuffer();
        inputStream = req.getInputStream();
        String s;
        BufferedReader in = new BufferedReader(new InputStreamReader(inputStream, "UTF-8"));
        while ((s = in.readLine()) != null) {
            sb.append(s);
        }
        in.close();
        inputStream.close();
        String json = sb.toString();
        return json;
    }
}
