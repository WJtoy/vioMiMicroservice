package com.kkl.kklplus.b2b.viomi.service;

import cn.hutool.core.util.StrUtil;
import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import com.google.gson.reflect.TypeToken;
import com.kkl.kklplus.b2b.viomi.entity.VioMiOrderInfo;

import com.kkl.kklplus.b2b.viomi.entity.request.*;
import com.kkl.kklplus.b2b.viomi.entity.response.VioMiResponse;
import com.kkl.kklplus.b2b.viomi.feign.WebFeign;
import com.kkl.kklplus.b2b.viomi.http.config.B2BVioMiProperties;
import com.kkl.kklplus.b2b.viomi.mapper.OrderInfoMapper;
import com.kkl.kklplus.b2b.viomi.mq.sender.B2BCenterOrderComplainSender;
import com.kkl.kklplus.b2b.viomi.mq.sender.B2BOrderMQSender;
import com.kkl.kklplus.b2b.viomi.utils.GsonUtils;
import com.kkl.kklplus.b2b.viomi.utils.QuarterUtils;
import com.kkl.kklplus.b2b.viomi.utils.VioMiUtils;
import com.kkl.kklplus.common.exception.MSErrorCode;
import com.kkl.kklplus.common.response.MSResponse;
import com.kkl.kklplus.entity.b2bcenter.md.B2BDataSourceEnum;
import com.kkl.kklplus.entity.b2bcenter.md.B2BShopEnum;
import com.kkl.kklplus.entity.b2bcenter.pb.MQB2BOrderComplainMessage;
import com.kkl.kklplus.entity.b2bcenter.pb.MQB2BOrderMessage;
import com.kkl.kklplus.entity.b2bcenter.sd.B2BOrder;
import com.kkl.kklplus.entity.b2bcenter.sd.B2BOrderActionEnum;
import com.kkl.kklplus.entity.b2bcenter.sd.B2BOrderSearchModel;
import com.kkl.kklplus.entity.b2bcenter.sd.B2BOrderTransferResult;
import com.kkl.kklplus.entity.common.MSPage;
import com.kkl.kklplus.entity.viomi.sd.VioMiExceptionOrder;
import com.kkl.kklplus.entity.viomi.sd.VioMiOrderCancel;
import com.kkl.kklplus.entity.viomi.sd.VioMiOrderExceptionSearchModel;
import com.kkl.kklplus.entity.viomi.sd.VioMiOrderHandle;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * describe:
 *
 * @author chenxj
 * @date 2020/09/18
 */
@Slf4j
@Service
@Transactional(propagation = Propagation.NOT_SUPPORTED)
public class OrderInfoService {

    @Resource
    private OrderInfoMapper orderInfoMapper;

    @Autowired
    private B2BOrderMQSender b2BOrderMQSender;

    @Autowired
    private B2BCenterOrderComplainSender b2BCenterOrderComplainSender;

    @Autowired
    private B2BProcesslogService b2BProcesslogService;

    @Autowired
    private WebFeign webFeign;

    @Autowired
    private B2BVioMiProperties vioMiProperties;

    /**
     * 处理数据
     * @param json
     * @return
     */
    public VioMiResponse createOrder(String json) {
        VioMiCommonRequest<VioMiOrderInfoRequest> request = GsonUtils.getInstance().fromUnderscoresJson(json, new TypeToken<VioMiCommonRequest<VioMiOrderInfoRequest>>() {
        }.getType());
        if(!vioMiProperties.getDataSourceConfig().getKey().equals(request.getKey())){
            return new VioMiResponse(1,"签名异常");
        }
        VioMiOrderInfoRequest data = request.getData();
        VioMiResponse response = valiOrderdata(data);
        if(response.getRes() == 0){
            VioMiOrderInfo orderInfo = parseOrderEntity(data);
//                Long id = orderInfoMapper.getIdByOrderNumber(data.getOrderNumber());
//                if (id != null && id > 0) {
//                    response.setRes(VioMiUtils.FAILURE_CODE);
//                    response.setMsg("该单据已存在");
//                    return response;
//                }
            if("投诉".equals(orderInfo.getType())) {
                //投诉时查询原始单据
                VioMiOrderInfo sourceOrder =
                        orderInfoMapper.getOrderByOrderNumber(orderInfo.getComplainOrderNumber());
                if (sourceOrder == null) {
                    response.setRes(VioMiUtils.FAILURE_CODE);
                    response.setMsg("没有找到关联单据");
                    return response;
                }
                MSResponse<String> rep = webFeign.generateComplainNo();
                if(MSResponse.isSuccess(rep)){
                    String orderNo = rep.getData();
                    orderInfo.setKklOrderNo(orderNo);
                    orderInfoMapper.insert(orderInfo);
                    processComplain(orderInfo,sourceOrder);
                    Map<String,String> repData = new HashMap<>();
                    repData.put("number",orderNo);
                    response.setData(repData);
                }else{
                    response.setRes(VioMiUtils.FAILURE_CODE);
                    response.setMsg("业务异常,单号获取失败");
                }
            }else{
                MSResponse<String> rep = webFeign.createOrderNo();
                if(MSResponse.isSuccess(rep)){
                    String orderNo = rep.getData();
                    orderInfo.setKklOrderNo(orderNo);
                    orderInfoMapper.insert(orderInfo);
                    sendOrderMQ(orderInfo);
                    Map<String,String> repData = new HashMap<>();
                    repData.put("number",orderNo);
                    response.setData(repData);
                }else{
                    response.setRes(VioMiUtils.FAILURE_CODE);
                    response.setMsg("业务异常,单号获取失败");
                }
            }
        }
        return response;
    }

    private void processComplain(VioMiOrderInfo orderInfo,VioMiOrderInfo sourceOrder) {
        Long kklOrderId = sourceOrder.getKklOrderId();
        if(kklOrderId != null &&kklOrderId > 0){
            MQB2BOrderComplainMessage.B2BOrderComplainMessage complainMessage = MQB2BOrderComplainMessage.B2BOrderComplainMessage.newBuilder()
                    .setB2BComplainNo(orderInfo.getOrderNumber())
                    .setOrderId(kklOrderId)
                    .setCreateAt(orderInfo.getCreateDt())
                    .setQuarter(sourceOrder.getQuarter())
                    .setContent(orderInfo.getRemarks())
                    .setComplainNo(orderInfo.getKklOrderNo())
                    .setB2BOrderNo(sourceOrder.getOrderNumber())
                    .setDataSource(B2BDataSourceEnum.VIOMI.id).build();
            b2BCenterOrderComplainSender.send(complainMessage);
        }
    }

    private void sendOrderMQ(VioMiOrderInfo orderInfo) {
        MQB2BOrderMessage.B2BOrderMessage.Builder builder = MQB2BOrderMessage.B2BOrderMessage.newBuilder()
                .setId(orderInfo.getId())
                .setKklOrderNo(orderInfo.getKklOrderNo())
                .setDataSource(B2BDataSourceEnum.VIOMI.id)
                .setOrderNo(orderInfo.getOrderNumber())
                .setShopId(StringUtils.trimToEmpty(orderInfo.getMiPurchaseChannel()))
                .setUserName(orderInfo.getContacts())
                .setUserMobile(orderInfo.getContactsPhone())
                .setUserAddress(orderInfo.getAddress()+orderInfo.getAddressDetail())
                .setStatus(0)
                .setParentBizOrderId(orderInfo.getOrderNumber())
                .setDescription(StringUtils.trimToEmpty(orderInfo.getRemarks()))
                .setCreateDt(orderInfo.getCreateDt())
                .setQuarter(orderInfo.getQuarter());
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        Long expectedServiceTime = orderInfo.getExpectedServiceTime();
        if(expectedServiceTime != null && expectedServiceTime > 0){
            try {
                builder.setExpectServiceTime(sdf.format(new Date(expectedServiceTime)));
            } catch (Exception e) {
                log.error("期望日期格式异常:{}:{}",orderInfo.getOrderNumber(),expectedServiceTime);
            }
        }
        Long miPurchaseTime = orderInfo.getMiPurchaseTime();
        if(miPurchaseTime != null){
            builder.setBuyDate(miPurchaseTime);
        }
        MQB2BOrderMessage.B2BOrderItem.Builder b2BOrderItem = MQB2BOrderMessage.B2BOrderItem.newBuilder()
                .setProductCode(orderInfo.getProduct69Code())
                .setProductName(orderInfo.getProductName())
                .setProductSpec(StringUtils.trimToEmpty(orderInfo.getProductModel()))
                .setServiceType(orderInfo.getType()+StringUtils.trimToEmpty(orderInfo.getSubType()))
                .setExpressNo(StringUtils.trimToEmpty(orderInfo.getSendCustomerLogisticsNumber()))
                .setExpressCompany(StringUtils.trimToEmpty(orderInfo.getSendCustomerCourierCompany()))
                .setQty(1);
        if(orderInfo.getPaymentObject().equals("合作方")){
            b2BOrderItem.setWarrantyType("保内");
        }else if(orderInfo.getPaymentObject().equals("客户")){
            b2BOrderItem.setWarrantyType("保外");
        }
        builder.addB2BOrderItem(b2BOrderItem.build());
        MQB2BOrderMessage.B2BOrderMessage b2BOrderMessage = builder.build();
        b2BOrderMQSender.send(b2BOrderMessage);
    }

    private VioMiOrderInfo parseOrderEntity(VioMiOrderInfoRequest data) {
        VioMiOrderInfo orderInfo = new VioMiOrderInfo();
        orderInfo.setCreateById(1L);
        orderInfo.preInsert();
        orderInfo.setQuarter(QuarterUtils.getQuarter(orderInfo.getCreateDt()));
        orderInfo.setOrderNumber(data.getOrderNumber());
        VioMiHandleRequest handle = data.getHandle();
        orderInfo.setOperator(StringUtils.left(handle.getOperator(),20));
        //orderInfo.setRemarks(StringUtils.left(handle.getRemarks(),255));
        VioMiOrderRequest order = data.getOrder();
        orderInfo.setContacts(StringUtils.left(order.getContacts(),60));
        orderInfo.setContactsPhone(StringUtils.left(order.getContactsPhone(),30));
        orderInfo.setAddress(StringUtils.left(order.getAddress(),100));
        orderInfo.setAddressDetail(StringUtils.left(order.getAddressDetail(),255));
        orderInfo.setType(StringUtils.left(order.getType(),30));
        orderInfo.setSubType(StringUtils.left(order.getSubType(),50));
        orderInfo.setSource(StringUtils.left(order.getSource(),50));
        orderInfo.setPaymentObject(StringUtils.left(order.getPaymentObject(),50));
        orderInfo.setComplainNetworkNumber(StringUtils.left(order.getComplainNetworkNumber(),50));
        orderInfo.setComplainOrderNumber(StringUtils.left(order.getComplainOrderNumber(),50));
        orderInfo.setUniqueid(order.getUniqueid());
        orderInfo.setSendCustomerCourierCompany(StringUtils.left(order.getSendCustomerCourierCompany(),50));
        orderInfo.setSendCustomerLogisticsNumber(StringUtils.left(order.getSendCustomerLogisticsNumber(),50));
        orderInfo.setRemarks(StringUtils.left(handle.getRemarks()+" "+
                StringUtils.trimToEmpty(order.getRemarks()),255));
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        String expectedServiceTime = order.getExpectedServiceTime();
        if(StringUtils.isNotBlank(expectedServiceTime)){
            try {
                orderInfo.setExpectedServiceTime(sdf.parse(expectedServiceTime).getTime());
            } catch (ParseException e) {
                log.error("期望日期格式异常:{}:{}",data.getOrderNumber(),expectedServiceTime);
            }
        }else{
            orderInfo.setExpectedServiceTime(0L);
        }
        VioMiProduct product = data.getProduct();
        orderInfo.setProductName(StringUtils.left(product.getProductName(),100));
        orderInfo.setProductModel(StringUtils.left(product.getProductModel(),50));
        orderInfo.setProductType(product.getProductType());
        orderInfo.setProductBigType(product.getProductBigType());
        orderInfo.setProduct69Code(StringUtils.left(product.getProduct_69Code(),50));
        orderInfo.setMiSn(StringUtils.left(product.getMiSn(),50));
        orderInfo.setMiPurchaseChannel(StringUtils.left(product.getMiPurchaseChannel(),50));
        orderInfo.setMiOrderNumber(StringUtils.left(product.getMiOrderNumber(),60));
        orderInfo.setInnerGuarantee(product.getInnerGuarantee());
//        String miPurchaseTime = product.getMiPurchaseTime();
//        if(StringUtils.isNotBlank(miPurchaseTime)){
//            try {
//                orderInfo.setMiPurchaseTime(sdf.parse(miPurchaseTime).getTime());
//            } catch (ParseException e) {
//                log.error("购买日期格式异常:{}:{}",data.getOrderNumber(),miPurchaseTime);
//            }
//        }else{
//            orderInfo.setMiPurchaseTime(0L);
//        }
        if (StrUtil.isNotEmpty(product.getMiPurchaseTime())) {
            Long miPurchaseTime = VioMiUtils.parseTimestamp(product.getMiPurchaseTime(), "yyyy-MM-dd HH:mm:ss");
            if (miPurchaseTime == 0) {
                miPurchaseTime = VioMiUtils.parseTimestamp(product.getMiPurchaseTime(), "yyyy-MM-dd");
            }
            orderInfo.setMiPurchaseTime(miPurchaseTime);
        } else {
            orderInfo.setMiPurchaseTime(0L);
        }

        List<VioMiKtProduct> ktProduct = data.getKtProduct();
        if(ktProduct != null){
            orderInfo.setKtProductJson(GsonUtils.getInstance().toJson(ktProduct));
        }
        return orderInfo;
    }

    private VioMiResponse valiOrderdata(VioMiOrderInfoRequest data) {
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
        VioMiOrderRequest order = data.getOrder();
        if(order == null){
            response.setRes(VioMiUtils.FAILURE_CODE);
            response.setMsg("工单信息不能为空");
            return response;
        }
        String contacts = order.getContacts();
        if(StringUtils.isBlank(contacts)){
            response.setRes(VioMiUtils.FAILURE_CODE);
            response.setMsg("用户姓名不能为空");
            return response;
        }
        String contactsPhone = order.getContactsPhone();
        if(StringUtils.isBlank(contactsPhone)){
            response.setRes(VioMiUtils.FAILURE_CODE);
            response.setMsg("用户电话不能为空");
            return response;
        }
        String address = order.getAddress();
        if(StringUtils.isBlank(address)){
            response.setRes(VioMiUtils.FAILURE_CODE);
            response.setMsg("用户地址不能为空");
            return response;
        }
        String addressDetail = order.getAddressDetail();
        if(StringUtils.isBlank(addressDetail)){
            response.setRes(VioMiUtils.FAILURE_CODE);
            response.setMsg("用户详细地址不能为空");
            return response;
        }
        String type = order.getType();
        if(StringUtils.isBlank(type)){
            response.setRes(VioMiUtils.FAILURE_CODE);
            response.setMsg("工单类型不能为空");
            return response;
        }
        String subType = order.getSubType();
        if(StringUtils.isBlank(subType)){
            response.setRes(VioMiUtils.FAILURE_CODE);
            response.setMsg("工单子类型不能为空");
            return response;
        }
        VioMiProduct product = data.getProduct();
        if(product == null){
            response.setRes(VioMiUtils.FAILURE_CODE);
            response.setMsg("服务产品不能为空");
            return response;
        }
        String productName = product.getProductName();
        if(StringUtils.isBlank(productName)){
            response.setRes(VioMiUtils.FAILURE_CODE);
            response.setMsg("产品名称不能为空");
            return response;
        }
        String product69Code = product.getProduct_69Code();
        if(StringUtils.isBlank(product69Code)){
            response.setRes(VioMiUtils.FAILURE_CODE);
            response.setMsg("产品69码不能为空");
            return response;
        }
        String innerGuarantee = product.getInnerGuarantee();
        if(StringUtils.isBlank(innerGuarantee)){
            //默认保内
            product.setInnerGuarantee("");
        }
//        /*String miPurchaseTime = product.getMiPurchaseTime();
//        if(StringUtils.isBlank(miPurchaseTime)){
//            response.setRes(VioMiUtils.FAILURE_CODE);
//            response.setMsg("购买时间不能为空");
//            return response;
//        }*/
        return response;
    }

    public MSPage<B2BOrder> getList(B2BOrderSearchModel orderSearchModel) {
        if (orderSearchModel.getPage() != null) {
            PageHelper.startPage(orderSearchModel.getPage().getPageNo(), orderSearchModel.getPage().getPageSize());
            Page<VioMiOrderInfo> orderPage = orderInfoMapper.getList(orderSearchModel);
            MSPage<B2BOrder> returnPage = new MSPage<>();
            Page<B2BOrder> b2bOrderPage = new Page<>();
            for(VioMiOrderInfo order : orderPage){
                B2BOrder b2bOrder = new B2BOrder();
                b2bOrder.setKklOrderNo(order.getKklOrderNo());
                b2bOrder.setId(order.getId());
                b2bOrder.setB2bOrderId(order.getId());
                b2bOrder.setDataSource(B2BDataSourceEnum.VIOMI.id);
                b2bOrder.setOrderNo(order.getOrderNumber());
                b2bOrder.setParentBizOrderId(order.getOrderNumber());
                b2bOrder.setShopId(order.getMiPurchaseChannel());
                b2bOrder.setUserName(order.getContacts());
                b2bOrder.setUserMobile(order.getContactsPhone());
                b2bOrder.setUserAddress(order.getAddress()+order.getAddressDetail());
                b2bOrder.setDescription(order.getRemarks());

                b2bOrder.setStatus(0);
                b2bOrder.setProcessFlag(order.getProcessFlag());
                b2bOrder.setProcessTime(order.getProcessTime());
                b2bOrder.setProcessComment(order.getProcessComment());
                b2bOrder.setCreateDt(order.getCreateDt());
                b2bOrder.setQuarter(order.getQuarter());
                SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                Long expectedServiceTime = order.getExpectedServiceTime();
                if(expectedServiceTime != null && expectedServiceTime > 0){
                    try {
                        b2bOrder.setExpectServiceTime(sdf.format(new Date(expectedServiceTime)));
                    } catch (Exception e) {
                        log.error("期望日期格式异常:{}:{}",order.getOrderNumber(),expectedServiceTime);
                    }
                }
                //产品
                B2BOrder.B2BOrderItem product = new B2BOrder.B2BOrderItem();
                product.setProductCode(order.getProduct69Code());
                product.setProductSpec(order.getProductModel().toString());
                product.setQty(1);
                product.setClassName(order.getProductType());
                product.setProductName(order.getProductName());
                product.setServiceType(order.getType()+StringUtils.trimToEmpty(order.getSubType()));
                if(order.getPaymentObject().equals("合作方")){
                    product.setWarrantyType("保内");
                }else if(order.getPaymentObject().equals("客户")){
                    product.setWarrantyType("保外");
                }
                product.setWarrantyType(order.getInnerGuarantee());
                product.setExpressNo(StringUtils.trimToEmpty(order.getSendCustomerLogisticsNumber()));
                product.setExpressCompany(StringUtils.trimToEmpty(order.getSendCustomerCourierCompany()));
                b2bOrder.getItems().add(product);
                b2bOrderPage.add(b2bOrder);
            }
            returnPage.setPageNo(orderPage.getPageNum());
            returnPage.setPageSize(orderPage.getPageSize());
            returnPage.setPageCount(orderPage.getPages());
            returnPage.setRowCount((int) orderPage.getTotal());
            returnPage.setList(b2bOrderPage.getResult());
            return returnPage;
        } else {
            return null;
        }
    }

    public List<VioMiOrderInfo> findOrdersProcessFlag(List<B2BOrderTransferResult> results) {
        return orderInfoMapper.findOrdersProcessFlag(results);
    }

    @Transactional(rollbackFor = Exception.class)
    public void updateTransferResult(List<VioMiOrderInfo> orders) {
        for(VioMiOrderInfo orderInfo:orders){
            orderInfoMapper.updateTransferResult(orderInfo);
        }
    }

    /**
     * 更新订单状态
     * @param vioMiOrderInfo
     * @return
     */
    public Integer updateOrderStatus(VioMiOrderInfo vioMiOrderInfo) {
        return orderInfoMapper.updateOrderStatus(vioMiOrderInfo);
    }

    /**
     * 更新工单异常信息
     * @param vioMiOrderInfo
     * @return
     */
    public Integer updateException(VioMiOrderInfo vioMiOrderInfo) {
        return orderInfoMapper.updateException(vioMiOrderInfo);
    }

    public Integer updateNextStepException(VioMiOrderInfo vioMiOrderInfo) {
        return orderInfoMapper.updateNextStepException(vioMiOrderInfo);
    }


    public VioMiOrderInfo getOrderStatusByOrderId(Long orderId) {
        return orderInfoMapper.getOrderStatusByOrderId(orderId);
    }

    /**
     * 根据云米工单编码查询工单信息
     * @param orderNumber
     * @return
     */
    public VioMiOrderInfo getOrderByOrderNumber(String orderNumber) {
        return orderInfoMapper.getOrderByOrderNumber(orderNumber);
    }

    public VioMiOrderInfo getOrderNumberByKklOrderId(Long kklOrderId) {
        return orderInfoMapper.getOrderNumberByKklOrderId(kklOrderId);
    }

    @Autowired
    private OrderCancelService orderCancelService;

    @Autowired
    private VioMiOrderHandleService orderHandleService;

    public MSResponse cancelOrderTransition(B2BOrderTransferResult result) {
        String updateBy = result.getUpdater();
        Long id = result.getId();
        VioMiOrderInfo order = orderInfoMapper.getOrderStatusByOrderId(id);
        MSResponse response = null;
        if(order.getType().equals("退货") || order.getType().equals("维修")){
            return new MSResponse<>(new MSErrorCode(1,"退货/维修不能主动退单！"));
        }
        if(order.getType().equals("换货")){
            VioMiOrderHandle vioMiOrderHandle = new VioMiOrderHandle();
            vioMiOrderHandle.setOrderNumber(result.getB2bOrderNo());
            vioMiOrderHandle.setB2bOrderId(result.getId());
            if(StringUtils.isNumeric(updateBy)){
                vioMiOrderHandle.setCreateById(Long.valueOf(updateBy));
            }else{
                vioMiOrderHandle.setCreateById(1L);
            }
            vioMiOrderHandle.setCreateDt(System.currentTimeMillis());
            vioMiOrderHandle.setStatus(B2BOrderActionEnum.CONVERTED_CANCEL.value);
            vioMiOrderHandle.setOperator(result.getUpdaterName());
            vioMiOrderHandle.setRemarks(result.getProcessComment());
            response = orderHandleService.orderConfirm(vioMiOrderHandle);
        }else{
            VioMiOrderCancel cancel = new VioMiOrderCancel();
            if(StringUtils.isNumeric(updateBy)){
                cancel.setCreateById(Long.valueOf(updateBy));
            }else{
                cancel.setCreateById(1L);
            }
            cancel.setOperator(result.getUpdaterName());
            cancel.setB2bOrderId(result.getId());
            cancel.setOrderNumber(result.getB2bOrderNo());
            cancel.setReason("其它原因");
            cancel.setRemarks(result.getProcessComment());
            cancel.preInsert();
            response = orderCancelService.cancelApiRequest(cancel);
        }
        MSErrorCode thirdPartyErrorCode = response.getThirdPartyErrorCode();
        if(!MSResponse.isSuccessCode(response) || thirdPartyErrorCode != null){
            result.setProcessFlag(3);
            result.setProcessComment(thirdPartyErrorCode != null ? thirdPartyErrorCode.getMsg():response.getMsg());
        }
        orderInfoMapper.cancelledOrder(result);
        return new MSResponse(MSErrorCode.SUCCESS);
    }

    public void cancalOrder(B2BOrderTransferResult result) {
        orderInfoMapper.cancelledOrder(result);
    }



    public MSPage<VioMiExceptionOrder> getOrderInfoList(VioMiOrderExceptionSearchModel vioMiOrderExceptionSearchModel){
        MSPage<VioMiExceptionOrder> returnPage = new MSPage<>();
        try {
                PageHelper.startPage(vioMiOrderExceptionSearchModel.getPage().getPageNo(), vioMiOrderExceptionSearchModel.getPage().getPageSize());
                Page<VioMiExceptionOrder> orderPage = orderInfoMapper.getOrderInfoList(vioMiOrderExceptionSearchModel);
                Page<VioMiExceptionOrder> vioMiExceptionOrders = new Page<>();
                for (VioMiExceptionOrder vioMiExceptionOrder : orderPage) {
                    vioMiExceptionOrder.setViomiStatus(B2BOrderActionEnum.valueOf(vioMiExceptionOrder.getOrderStatus()).label);
                    vioMiExceptionOrder.setAddress(vioMiExceptionOrder.getAddress()+vioMiExceptionOrder.getAddressDetail());
                    vioMiExceptionOrders.add(vioMiExceptionOrder);
                }
                returnPage.setPageNo(orderPage.getPageNum());
                returnPage.setPageSize(orderPage.getPageSize());
                returnPage.setPageCount(orderPage.getPages());
                returnPage.setRowCount((int) orderPage.getTotal());
                returnPage.setList(vioMiExceptionOrders.getResult());
            }catch (Exception e){
                log.warn(e.getMessage());
            }
        return returnPage;
    }


    public VioMiExceptionOrder getOrderById(Long id){
       return orderInfoMapper.getOrderById(id);
    }

}
