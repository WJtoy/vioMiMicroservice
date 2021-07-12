package com.kkl.kklplus.b2b.viomi.mapper;

import com.github.pagehelper.Page;
import com.kkl.kklplus.b2b.viomi.entity.VioMiOrderInfo;

import com.kkl.kklplus.entity.b2bcenter.sd.B2BOrderSearchModel;
import com.kkl.kklplus.entity.b2bcenter.sd.B2BOrderTransferResult;
import com.kkl.kklplus.entity.viomi.sd.VioMiExceptionOrder;
import com.kkl.kklplus.entity.viomi.sd.VioMiOrderExceptionSearchModel;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface OrderInfoMapper {

    Integer insert(VioMiOrderInfo orderInfo);

    Long getIdByOrderNumber(@Param("orderNumber")String orderNumber);

    /**
     * 更新订单状态
     * @param orderInfo
     * @return
     */
    Integer updateOrderStatus(VioMiOrderInfo orderInfo);

    Integer updateException(VioMiOrderInfo orderInfo);

    Integer updateNextStepException(VioMiOrderInfo orderInfo);

    Page<VioMiOrderInfo> getList(B2BOrderSearchModel orderSearchModel);

    List<VioMiOrderInfo> findOrdersProcessFlag(@Param("results") List<B2BOrderTransferResult> results);

    void updateTransferResult(VioMiOrderInfo orderInfo);

    VioMiOrderInfo getOrderByOrderNumber(@Param("orderNumber")String orderNumber);

    VioMiOrderInfo getOrderNumberByKklOrderId(@Param("kklOrderId") Long kklOrderId);

    VioMiOrderInfo getOrderStatusByOrderId(@Param("orderId") Long orderId);

    Integer cancelledOrder(B2BOrderTransferResult b2BOrderTransferResult);

    Page<VioMiExceptionOrder> getOrderInfoList(VioMiOrderExceptionSearchModel b2BViomiApiLogRequest);

    VioMiExceptionOrder getOrderById(Long id);

}
