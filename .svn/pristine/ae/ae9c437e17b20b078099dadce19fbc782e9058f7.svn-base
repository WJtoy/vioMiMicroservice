package com.kkl.kklplus.b2b.viomi.controller;

import com.kkl.kklplus.b2b.viomi.entity.VioMiOrderRemark;
import com.kkl.kklplus.b2b.viomi.service.OrderCancelService;
import com.kkl.kklplus.b2b.viomi.service.OrderRemarkService;
import com.kkl.kklplus.b2b.viomi.service.VioMiOrderHandleService;
import com.kkl.kklplus.common.exception.MSErrorCode;
import com.kkl.kklplus.common.response.MSResponse;
import com.kkl.kklplus.entity.viomi.sd.VioMiOrderCancel;
import com.kkl.kklplus.entity.viomi.sd.VioMiOrderHandle;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Calendar;
import java.util.Date;

/**
 * describe:
 *
 * @author chenxj
 * @date 2020/09/21
 */
@Slf4j
@RestController
@RequestMapping("/order/cancel")
public class VioMiOrderCancelController {

    @Autowired
    private OrderCancelService orderCancelService;

    @Autowired
    private VioMiOrderHandleService vioMiOrderHandleService;

    @ApiOperation("取消工单")
    @PostMapping("")
    public MSResponse cancel(@RequestBody VioMiOrderCancel cancel) {
        VioMiOrderHandle vioMiOrderHandle = new VioMiOrderHandle();
        vioMiOrderHandle.setB2bOrderId(cancel.getB2bOrderId());
        vioMiOrderHandle.setOrderNumber(cancel.getOrderNumber());
        vioMiOrderHandle.setStatus(10);
        vioMiOrderHandle.setOperator(cancel.getOperator());
        vioMiOrderHandle.setTimeOfAppointment(autoBookDateNextHours());
        vioMiOrderHandle.setEngineerName("");
        vioMiOrderHandle.setEngineerPhone("");
        vioMiOrderHandle.setRemarks("");
        vioMiOrderHandle.setCreateById(cancel.getCreateById());
        vioMiOrderHandle.setUpdateById(cancel.getCreateById());
        vioMiOrderHandle.preInsert();
        vioMiOrderHandleService.appointment(vioMiOrderHandle);
        return orderCancelService.cancelApiRequest(cancel);
    }

    public Long autoBookDateNextHours() {
        Calendar createCalendar = Calendar.getInstance();
        createCalendar.setTime(new Date(System.currentTimeMillis()));
        createCalendar.add(Calendar.HOUR_OF_DAY, 1);
        createCalendar.set(Calendar.MINUTE, 0);
        createCalendar.set(Calendar.SECOND, 0);
        createCalendar.set(Calendar.MILLISECOND, 0);
        return createCalendar.getTimeInMillis();
    }

    @GetMapping("/updateProcessFlag/{id}")
    public MSResponse updateProcessFlag(@PathVariable("id") Long id){
        orderCancelService.updateProcessFlag(id);
        return new MSResponse(MSErrorCode.SUCCESS);
    }
}
