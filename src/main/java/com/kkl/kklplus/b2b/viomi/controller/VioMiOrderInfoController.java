package com.kkl.kklplus.b2b.viomi.controller;

import com.google.gson.FieldNamingPolicy;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.kkl.kklplus.b2b.viomi.entity.SysLog;
import com.kkl.kklplus.b2b.viomi.entity.VioMiOrderInfo;
import com.kkl.kklplus.b2b.viomi.http.command.OperationCommand;
import com.kkl.kklplus.b2b.viomi.http.config.B2BVioMiProperties;
import com.kkl.kklplus.b2b.viomi.http.request.OrderRemarkRequestParam;
import com.kkl.kklplus.b2b.viomi.http.request.ProductPartsRequestParam;
import com.kkl.kklplus.b2b.viomi.http.response.ResponseBody;
import com.kkl.kklplus.b2b.viomi.http.utils.OkHttpUtils;
import com.kkl.kklplus.b2b.viomi.service.B2BProcesslogService;
import com.kkl.kklplus.b2b.viomi.service.OrderInfoService;
import com.kkl.kklplus.b2b.viomi.service.OrderRemarkService;
import com.kkl.kklplus.b2b.viomi.service.SysLogService;
import com.kkl.kklplus.b2b.viomi.utils.GsonUtils;
import com.kkl.kklplus.common.exception.MSErrorCode;
import com.kkl.kklplus.common.response.MSResponse;
import com.kkl.kklplus.entity.b2b.common.B2BProcessFlag;
import com.kkl.kklplus.entity.b2b.rpt.B2BProcesslog;
import com.kkl.kklplus.entity.b2bcenter.sd.B2BOrder;
import com.kkl.kklplus.entity.b2bcenter.sd.B2BOrderSearchModel;
import com.kkl.kklplus.entity.b2bcenter.sd.B2BOrderTransferResult;
import com.kkl.kklplus.entity.common.MSPage;
import com.kkl.kklplus.entity.viomi.sd.VioMiOrderRemark;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * @author Administrator
 */
@Slf4j
@RestController
@RequestMapping("/viomiOrderInfo")
public class VioMiOrderInfoController {

    @Autowired
    private B2BProcesslogService b2BProcesslogService;

    @Autowired
    private SysLogService sysLogService;

    @Autowired
    private B2BVioMiProperties vioMiProperties;

    @Autowired
    private OrderInfoService orderInfoService;

    @Autowired
    private OrderRemarkService orderRemarkService;


    @PostMapping("/test")
    public MSResponse test() {
        OrderRemarkRequestParam requestParam = new OrderRemarkRequestParam();
        requestParam.setOrderNumber("");
        requestParam.setInputuser("测试");
        requestParam.setRemarks("测试");
        OperationCommand command = OperationCommand.newInstance
                (OperationCommand.OperationCode.OTHER_REMARK_ORDER, requestParam);
        ResponseBody<ResponseBody> resBody = OkHttpUtils.postSyncGenericNew(command, ResponseBody.class);
        return new MSResponse(new MSErrorCode(0,resBody.getOriginalJson()));
    }

    /**
     * 获取工单(分页)
     * @param orderSearchModel
     * @return
     */
    @ApiOperation("未转单列表")
    @PostMapping("/getList")
    public MSResponse<MSPage<B2BOrder>> getList(@RequestBody B2BOrderSearchModel orderSearchModel) {
        try {
            MSPage<B2BOrder> returnPage = orderInfoService.getList(orderSearchModel);
            return new MSResponse<>(MSErrorCode.SUCCESS, returnPage);
        } catch (Exception e) {
            log.error("查询工单异常:{}", e.getMessage());
            sysLogService.insert(1L, GsonUtils.getInstance().toJson(orderSearchModel), e.getMessage(),
                    "查询工单异常", "viomiOrderInfo/getList", "POST");
            return new MSResponse<>(new MSErrorCode(1000, StringUtils.left(e.getMessage(),200)));
        }
    }

    /**
     * 检查工单是否可以转换
     * @param resultList
     * @return
     */
    @ApiOperation("检查工单是否可以转换")
    @PostMapping("/checkWorkcardProcessFlag")
    public MSResponse checkWorkcardProcessFlag(@RequestBody List<B2BOrderTransferResult> resultList){
        try {
            if(resultList == null){
                return new MSResponse(new MSErrorCode(1000, "参数错误，工单编号不能为空"));
            }
            //查询出对应工单的状态
            List<VioMiOrderInfo> orderInfos = orderInfoService.findOrdersProcessFlag(resultList);
            if(orderInfos == null){
                return new MSResponse(MSErrorCode.FAILURE);
            }
            for (VioMiOrderInfo orderInfo : orderInfos) {
                if (orderInfo.getProcessFlag() != null && orderInfo.getProcessFlag() >= B2BProcessFlag.PROCESS_FLAG_SUCESS.value) {
                    return new MSResponse(new MSErrorCode(1000, orderInfo.getOrderNumber()+"工单已经转换成功或已取消,不能重复操作"));
                }
            }
            return new MSResponse(MSErrorCode.SUCCESS);
        }catch (Exception e){
            log.error("检查工单异常:{}", e.getMessage());
            sysLogService.insert(1L,GsonUtils.getInstance().toJson(resultList), e.getMessage(),
                    "检查工单异常","viomiOrderInfo/checkWorkcardProcessFlag", "POST");
            return new MSResponse(new MSErrorCode(1000, StringUtils.left(e.getMessage(),200)));
        }
    }
    @ApiOperation("转换工单")
    @PostMapping("/updateTransferResult")
    public MSResponse updateOrderTransferResult(@RequestBody List<B2BOrderTransferResult> results) {
        try {
            //根据订单ID转MAP
            Map<Long,B2BOrderTransferResult> maps = results.stream().collect(Collectors.toMap(B2BOrderTransferResult::getB2bOrderId, Function.identity(), (key1, key2) -> key2));
            //查询出需要转换的工单
            List<VioMiOrderInfo> orderInfos = orderInfoService.findOrdersProcessFlag(results);
            //存放需要转换的工单集合
            List<VioMiOrderInfo> orders = new ArrayList<>();
            for(VioMiOrderInfo orderInfo:orderInfos){
                //如果工单为转换成功的才存放进工单集合
                if(orderInfo.getProcessFlag() != B2BProcessFlag.PROCESS_FLAG_SUCESS.value){
                    B2BOrderTransferResult transferResult = maps.get(orderInfo.getId());

                    if(transferResult != null){
                        Long updateById = 1L;
                        String updater = transferResult.getUpdater();
                        if(StringUtils.isNotBlank(updater) && StringUtils.isNumeric(updater)){
                            updateById = Long.valueOf(updater);
                        }
                        orderInfo.setProcessFlag(transferResult.getProcessFlag());
                        orderInfo.setKklOrderId(transferResult.getOrderId());
                        orderInfo.setKklOrderNo(transferResult.getKklOrderNo());
                        orderInfo.setUpdateDt(transferResult.getUpdateDt());
                        orderInfo.setUpdateById(updateById);
                        orderInfo.setProcessComment(transferResult.getProcessComment());
                        orders.add(orderInfo);
                    }
                }
            }
            orderInfoService.updateTransferResult(orders);
            return new MSResponse(MSErrorCode.SUCCESS);
        } catch (Exception e) {
            log.error("工单转换异常:{}", e.getMessage());
            sysLogService.insert(1L, GsonUtils.getInstance().toJson(results),e.getMessage(),
                    "工单转换异常","viomiOrderInfo/updateTransferResult", "POST");
            return new MSResponse(new MSErrorCode(1000, StringUtils.left(e.getMessage(),200)));

        }
    }

    @ApiOperation("取消转换")
    @PostMapping("/cancelOrderTransition")
    public MSResponse cancelOrderTransition(@RequestBody B2BOrderTransferResult b2BOrderTransferResult){
        try {
            return orderInfoService.cancelOrderTransition(b2BOrderTransferResult);
        }catch (Exception e){
            log.error("取消工单失败", e.getMessage());
            sysLogService.insert(1L,GsonUtils.getInstance().toJson(b2BOrderTransferResult),"取消工单失败：" + e.getMessage(),
                    "取消工单失败","orderInfo/cancelOrderTransition", "POST");
            return new MSResponse(new MSErrorCode(1000, org.apache.commons.lang3.StringUtils.left(e.getMessage(),200)));
        }
    }


    @ApiOperation("日志")
    @PostMapping("/log")
    public MSResponse saveLog(@RequestBody VioMiOrderRemark remark) {
        return orderRemarkService.remarkApiRequest(remark);
    }


}
