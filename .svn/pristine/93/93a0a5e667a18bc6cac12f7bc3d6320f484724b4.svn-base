package com.kkl.kklplus.b2b.viomi.controller;


import com.kkl.kklplus.b2b.viomi.service.OrderInfoService;
import com.kkl.kklplus.b2b.viomi.service.SysLogService;
import com.kkl.kklplus.b2b.viomi.service.ViomiApiLogService;
import com.kkl.kklplus.b2b.viomi.utils.GsonUtils;
import com.kkl.kklplus.common.exception.MSErrorCode;
import com.kkl.kklplus.common.response.MSResponse;
import com.kkl.kklplus.entity.common.MSPage;
import com.kkl.kklplus.entity.viomi.sd.VioMiApiLog;
import com.kkl.kklplus.entity.viomi.sd.VioMiExceptionOrder;
import com.kkl.kklplus.entity.viomi.sd.VioMiOrderExceptionSearchModel;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * @Auther wj
 * @Date 2020/10/29 11:27
 */

@Slf4j
@RestController
@RequestMapping("/apiLog")
public class B2BViomiApiLogController {

    @Autowired
    private OrderInfoService orderInfoService;

    @Autowired
    private SysLogService sysLogService;

    @Autowired
    private ViomiApiLogService viomiApiLogService;


    @ApiOperation("工单异常列表")
    @PostMapping("/getExceptionOrderList")
    public MSResponse<MSPage<VioMiExceptionOrder>> getExceptionOrderList(@RequestBody VioMiOrderExceptionSearchModel vioMiOrderExceptionSearchModel) {
        try {
            MSPage<VioMiExceptionOrder> returnPage = orderInfoService.getOrderInfoList(vioMiOrderExceptionSearchModel);
            return new MSResponse<>(MSErrorCode.SUCCESS, returnPage);
        } catch (Exception e) {
            log.error("查询工单异常:{}", e.getMessage());
            sysLogService.insert(1L, GsonUtils.getInstance().toJson(vioMiOrderExceptionSearchModel), e.getMessage(),
                    "查询工单异常", "apiLog/getOrderInfoList", "POST");
            return new MSResponse<>(new MSErrorCode(1000, StringUtils.left(e.getMessage(), 200)));
        }
    }

    @ApiOperation("查询api异常数据")
    @GetMapping("/getExceptionApiList/{b2bOrderId}")
    public MSResponse<List<VioMiApiLog>> getExceptionApiList(@PathVariable Long b2bOrderId) {
        try {
            List<VioMiApiLog> result = viomiApiLogService.getVioMiApiLogList(b2bOrderId);
            return new MSResponse<>(MSErrorCode.SUCCESS, result);
        } catch (Exception e) {
            log.error("查询工单异常:{}", e.getMessage());
            sysLogService.insert(1L, GsonUtils.getInstance().toJson(b2bOrderId), e.getMessage(),
                    "查询工单异常", "apiLog/getOrderInfoList", "get");
            return new MSResponse<>(new MSErrorCode(1000, StringUtils.left(e.getMessage(), 200)));
        }
    }


    @ApiOperation("查询工单数据")
    @GetMapping("/getOrderById/{id}")
    public MSResponse<VioMiExceptionOrder> getOrderById(@PathVariable Long id) {
        try {
            VioMiExceptionOrder result = orderInfoService.getOrderById(id);
            return new MSResponse<>(MSErrorCode.SUCCESS, result);
        } catch (Exception e) {
            log.error("查询工单异常:{}", e.getMessage());
            sysLogService.insert(1L, GsonUtils.getInstance().toJson(id), e.getMessage(),
                    "查询工单异常", "apiLog/getOrderById", "get");
            return new MSResponse<>(new MSErrorCode(1000, StringUtils.left(e.getMessage(), 200)));
        }

    }

}
