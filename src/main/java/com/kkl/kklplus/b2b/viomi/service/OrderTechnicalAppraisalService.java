package com.kkl.kklplus.b2b.viomi.service;

import com.google.gson.reflect.TypeToken;
import com.kkl.kklplus.b2b.viomi.entity.VioMiOrderInfo;
import com.kkl.kklplus.b2b.viomi.entity.request.VioMiCommonRequest;
import com.kkl.kklplus.b2b.viomi.entity.request.VioMiHandleRequest;
import com.kkl.kklplus.b2b.viomi.entity.request.VioMiTechnicalAppraisalRequest;
import com.kkl.kklplus.b2b.viomi.entity.response.VioMiResponse;
import com.kkl.kklplus.b2b.viomi.http.config.B2BVioMiProperties;
import com.kkl.kklplus.b2b.viomi.mq.sender.B2BCenterOrderProcessMQSend;
import com.kkl.kklplus.b2b.viomi.utils.GsonUtils;
import com.kkl.kklplus.b2b.viomi.utils.VioMiUtils;
import com.kkl.kklplus.common.response.MSResponse;
import com.kkl.kklplus.entity.b2bcenter.md.B2BDataSourceEnum;
import com.kkl.kklplus.entity.b2bcenter.pb.MQB2BOrderProcessMessage;
import com.kkl.kklplus.entity.b2bcenter.sd.B2BOrderActionEnum;
import com.kkl.kklplus.entity.viomi.sd.VioMiOrderHandle;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * describe:
 *
 * @author chenxj
 * @date 2020/10/14
 */
@Slf4j
@Service
@Transactional(propagation = Propagation.NOT_SUPPORTED)
public class OrderTechnicalAppraisalService {

    @Autowired
    private B2BVioMiProperties vioMiProperties;

    @Autowired
    private OrderInfoService orderInfoService;

    @Autowired
    private B2BCenterOrderProcessMQSend orderProcessMQSend;

    @Autowired
    private ThreadPoolTaskExecutor threadPoolTaskExecutor;

    @Autowired
    private VioMiOrderHandleService orderHandleService;

    public VioMiResponse technicalAppraisal(String json) {
        VioMiCommonRequest<VioMiTechnicalAppraisalRequest> request = GsonUtils.getInstance().fromUnderscoresJson
                (json, new TypeToken<VioMiCommonRequest<VioMiTechnicalAppraisalRequest>>() {
                }.getType());
        if(!vioMiProperties.getDataSourceConfig().getKey().equals(request.getKey())){
            return new VioMiResponse(1,"签名异常");
        }
        VioMiTechnicalAppraisalRequest data = request.getData();
        VioMiResponse response = validate(data);
        if(response.getRes() == 0){
            VioMiOrderInfo orderInfo = orderInfoService.getOrderByOrderNumber(data.getOrderNumber());
            if(orderInfo == null){
                response.setRes(VioMiUtils.FAILURE_CODE);
                response.setMsg("没找到对应单据");
                return response;
            }
            Long kklOrderId = orderInfo.getKklOrderId();
            if(kklOrderId != null && kklOrderId > 0) {
                String remarks = data.getHandle().getRemarks();
                int status = 0;
                if(data.getHandle().getNode().equals("重新办理")){
                    status = 1;
                }
                MQB2BOrderProcessMessage.B2BOrderProcessMessage processMessage =
                        MQB2BOrderProcessMessage.B2BOrderProcessMessage.newBuilder()
                                .setMessageId(System.currentTimeMillis())
                                .setB2BOrderNo(data.getOrderNumber())
                                .setKklOrderId(kklOrderId)
                                .setB2BOrderId(orderInfo.getB2bOrderId())
                                .setActionType(B2BOrderActionEnum.VALIDATE.value)
                                .setStatus(status)
                                .setDataSource(B2BDataSourceEnum.VIOMI.id)
                                .setRemarks(remarks).build();
                orderProcessMQSend.send(processMessage);
                if(status == 1) {
                    threadPoolTaskExecutor.execute(() -> {
                                VioMiOrderHandle vioMiOrderHandle = orderHandleService.findLastPlan(orderInfo.getId());
                                for (int i = 0; i < 3; i++) {
                                    MSResponse handleResponse = orderHandleService.planing(vioMiOrderHandle,true);
                                    if (MSResponse.isSuccessCode(handleResponse)) {
                                        break;
                                    }
                                }
                            }
                    );
                }
            }
        }
        return response;
    }

    private VioMiResponse validate(VioMiTechnicalAppraisalRequest data) {
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
        String node = handle.getNode();
        if(StringUtils.isBlank(node)){
            response.setRes(VioMiUtils.FAILURE_CODE);
            response.setMsg("流程节点不能为空");
            return response;
        }
        String remarks = handle.getRemarks();
        if(StringUtils.isBlank(remarks)){
            response.setRes(VioMiUtils.FAILURE_CODE);
            response.setMsg("工单备注原因不能为空");
            return response;
        }
        return response;
    }
}
