package com.kkl.kklplus.b2b.viomi.controller;

import com.kkl.kklplus.b2b.viomi.service.VioMiOrderHandleService;
import com.kkl.kklplus.common.response.MSResponse;
import com.kkl.kklplus.entity.b2bcenter.sd.B2BOrderActionEnum;
import com.kkl.kklplus.entity.b2bcenter.sd.B2BOrderComplainProcess;
import com.kkl.kklplus.entity.viomi.sd.VioMiOrderHandle;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/order/handle")
public class VioMiOrderHandleController {

    @Autowired
    private VioMiOrderHandleService vioMiOrderHandleService;

    /**
     * 派送师傅
     * @param vioMiOrderHandle
     * @return
     */
    @PostMapping("planing")
    public MSResponse<Integer> planing(@RequestBody VioMiOrderHandle vioMiOrderHandle) {
        return vioMiOrderHandleService.planing(vioMiOrderHandle,false);
    }

    /**
     * 预约上门
     * @param vioMiOrderHandle
     * @return
     */
    @PostMapping("appointment")
    public MSResponse<Integer> appointment(@RequestBody VioMiOrderHandle vioMiOrderHandle) {
        return vioMiOrderHandleService.appointment(vioMiOrderHandle);
    }

    /**
     * 上门打卡
     * @param vioMiOrderHandle
     * @return
     */
    @PostMapping("clockInHome")
    public MSResponse<Integer> clockInHome(@RequestBody VioMiOrderHandle vioMiOrderHandle) {
        return vioMiOrderHandleService.clockInHome(vioMiOrderHandle);
    }

    /**
     * 处理完成
     * @param vioMiOrderHandle
     * @return
     */
    @PostMapping("processComplete")
    public MSResponse<Integer> processComplete(@RequestBody VioMiOrderHandle vioMiOrderHandle) {
        return vioMiOrderHandleService.processComplete(vioMiOrderHandle);
    }

    /**
     * 申请完单
     * @param vioMiOrderHandle
     * @return
     */
    @PostMapping("applyFinished")
    public MSResponse<Integer> applyFinished(@RequestBody VioMiOrderHandle vioMiOrderHandle) {
        return vioMiOrderHandleService.applyFinished(vioMiOrderHandle);
    }

    /**
     * 申请完单
     * @param vioMiOrderHandle
     * @return
     */
    @PostMapping("orderReturnVisit")
    public MSResponse<Integer> orderReturnVisit(@RequestBody VioMiOrderHandle vioMiOrderHandle) {
        return vioMiOrderHandleService.orderReturnVisit(vioMiOrderHandle);
    }

    /**
     * 工单鉴定
     * @param vioMiOrderHandle
     * @return
     */
    @PostMapping("orderNeedValidate")
    public MSResponse<Integer> orderNeedValidate(@RequestBody VioMiOrderHandle vioMiOrderHandle) {
        return vioMiOrderHandleService.orderNeedValidate(vioMiOrderHandle);
    }

    /**
     * 投诉单完成
     * @param complainProcess
     * @return
     */
    @PostMapping("complainCompleted")
    public MSResponse<Integer> complainCompleted(@RequestBody B2BOrderComplainProcess complainProcess) {
        VioMiOrderHandle vioMiOrderHandle = new VioMiOrderHandle();
        vioMiOrderHandle.setOrderNumber(complainProcess.getB2bComplainNo());
        vioMiOrderHandle.setB2bOrderId(0L);
        vioMiOrderHandle.setAttachment(complainProcess.getAttachments());
        vioMiOrderHandle.setOperator(complainProcess.getCreateName());
        vioMiOrderHandle.setRemarks(complainProcess.getContent());
        vioMiOrderHandle.setCreateById(complainProcess.getCreateId());
        vioMiOrderHandle.setCreateDt(complainProcess.getCreateAt());
        vioMiOrderHandle.setStatus(B2BOrderActionEnum.COMPLETE.value);
        return vioMiOrderHandleService.complainCompleted(vioMiOrderHandle);
    }

    /**
     * 退换货回寄送
     * @param vioMiOrderHandle
     * @return
     */
    @PostMapping("orderServicePointSend")
    public MSResponse<Integer> orderServicePointSend(@RequestBody VioMiOrderHandle vioMiOrderHandle) {
        return vioMiOrderHandleService.orderServicePointSend(vioMiOrderHandle);
    }

    /**
     * 退换货拆装
     * @param vioMiOrderHandle
     * @return
     */
    @PostMapping("orderDismounting")
    public MSResponse<Integer> orderDismounting(@RequestBody VioMiOrderHandle vioMiOrderHandle) {
        return vioMiOrderHandleService.orderDismounting(vioMiOrderHandle);
    }

    /**
     * 工单确认
     * @param vioMiOrderHandle
     * @return
     */
    @PostMapping("orderConfirm")
    public MSResponse<Integer> orderConfirm(@RequestBody VioMiOrderHandle vioMiOrderHandle) {
        return vioMiOrderHandleService.orderConfirm(vioMiOrderHandle);
    }



}
