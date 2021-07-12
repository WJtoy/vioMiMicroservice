package com.kkl.kklplus.b2b.viomi.service;

import com.google.gson.reflect.TypeToken;
import com.kkl.kklplus.b2b.viomi.entity.VioMiOrderInfo;
import com.kkl.kklplus.b2b.viomi.entity.VioMiOrderReminder;
import com.kkl.kklplus.b2b.viomi.entity.request.VioMiCommonRequest;
import com.kkl.kklplus.b2b.viomi.entity.request.VioMiHandleRequest;
import com.kkl.kklplus.b2b.viomi.entity.request.VioMiOrderCancelRequest;
import com.kkl.kklplus.b2b.viomi.entity.request.VioMiOrderReminderRequest;
import com.kkl.kklplus.b2b.viomi.entity.response.VioMiResponse;
import com.kkl.kklplus.b2b.viomi.http.config.B2BVioMiProperties;
import com.kkl.kklplus.b2b.viomi.mapper.OrderReminderMapper;
import com.kkl.kklplus.b2b.viomi.mq.sender.B2BCenterReminderMQSender;
import com.kkl.kklplus.b2b.viomi.utils.GsonUtils;
import com.kkl.kklplus.b2b.viomi.utils.QuarterUtils;
import com.kkl.kklplus.b2b.viomi.utils.VioMiUtils;
import com.kkl.kklplus.entity.b2b.common.B2BProcessFlag;
import com.kkl.kklplus.entity.b2bcenter.md.B2BDataSourceEnum;
import com.kkl.kklplus.entity.b2bcenter.pb.MQB2BOrderReminderMessage;
import com.kkl.kklplus.entity.b2bcenter.rpt.B2BOrderProcesslog;
import com.kkl.kklplus.entity.viomi.sd.VioMiOrderCancel;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;

/**
 * describe:
 *
 * @author chenxj
 * @date 2020/09/22
 */
@Slf4j
@Service
@Transactional(propagation = Propagation.NOT_SUPPORTED)
public class OrderReminderService {


    @Autowired
    private B2BProcesslogService b2BProcesslogService;

    @Autowired
    private OrderInfoService orderInfoService;

    @Autowired
    private B2BCenterReminderMQSender reminderMQSender;

    @Autowired
    private B2BVioMiProperties vioMiProperties;

    @Resource
    private OrderReminderMapper orderReminderMapper;


    public VioMiResponse reminderOrder(String json) {
        VioMiCommonRequest<VioMiOrderReminderRequest> request = GsonUtils.getInstance().fromUnderscoresJson
                (json, new TypeToken<VioMiCommonRequest<VioMiOrderReminderRequest>>() {
                }.getType());
        if(!vioMiProperties.getDataSourceConfig().getKey().equals(request.getKey())){
            return new VioMiResponse(1,"签名异常");
        }
        VioMiOrderReminderRequest data = request.getData();
        VioMiResponse response = validateOrderReminder(data);
        if(response.getRes() == 0){
            VioMiOrderInfo orderInfo = orderInfoService.getOrderByOrderNumber(data.getOrderNumber());
            if(orderInfo == null){
                response.setRes(VioMiUtils.FAILURE_CODE);
                response.setMsg("没找到对应单据");
                return response;
            }
            Long kklOrderId = orderInfo.getKklOrderId();
            VioMiOrderReminder reminder = parseEntity(data);
            reminder.setB2bOrderId(orderInfo.getId());
            reminder.setKklOrderId(kklOrderId);
            orderReminderMapper.insert(reminder);
            if(kklOrderId != null && kklOrderId > 0) {
                sendOrderReminderMQ(reminder);
            }
        }
        return response;
    }

    private void sendOrderReminderMQ(VioMiOrderReminder reminder) {
        MQB2BOrderReminderMessage.B2BOrderReminderMessage message = MQB2BOrderReminderMessage.B2BOrderReminderMessage.newBuilder()
                .setB2BReminderId(reminder.getId())
                .setDataSource(B2BDataSourceEnum.VIOMI.id)
                .setContent(reminder.getRemarks())
                .setCreateDate(reminder.getCreateDt())
                .setKklOrderId(reminder.getKklOrderId())
                .setB2BQuarter(reminder.getQuarter()).build();
        reminderMQSender.send(message);
    }

    private VioMiOrderReminder parseEntity(VioMiOrderReminderRequest data) {
        VioMiOrderReminder vioMiOrderReminder = new VioMiOrderReminder();
        vioMiOrderReminder.setOperator(data.getHandle().getOperator());
        vioMiOrderReminder.setRemarks(data.getHandle().getRemarks());
        vioMiOrderReminder.setOrderNumber(data.getOrderNumber());
        vioMiOrderReminder.setCreateById(1L);
        vioMiOrderReminder.preInsert();
        vioMiOrderReminder.setQuarter(QuarterUtils.getQuarter(vioMiOrderReminder.getCreateDt()));
        return vioMiOrderReminder;
    }

    private VioMiResponse validateOrderReminder(VioMiOrderReminderRequest data) {
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
        String remarks = handle.getRemarks();
        if(StringUtils.isBlank(remarks)){
            response.setRes(VioMiUtils.FAILURE_CODE);
            response.setMsg("工单催单备注不能为空");
            return response;
        }
        return response;
    }
}
