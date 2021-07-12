package com.kkl.kklplus.b2b.viomi.mapper;

import com.kkl.kklplus.entity.viomi.sd.VioMiOrderHandle;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface VioMiOrderHandleMapper {
    /**
     * 添加工单办理
     * @param vioMiOrderHandle
     * @return
     */
    Integer insert(VioMiOrderHandle vioMiOrderHandle);

    /**
     * 更新处理结果
     * @param vioMiOrderHandle
     * @return
     */
    Integer updateProcessFlag(VioMiOrderHandle vioMiOrderHandle);

    VioMiOrderHandle findLastPlan(@Param("b2bOrderId") Long b2bOrderId);
}
