package com.kkl.kklplus.b2b.viomi.mapper;

import com.kkl.kklplus.b2b.viomi.entity.ViomiApiInfo;
import com.kkl.kklplus.entity.viomi.sd.VioMiApiLog;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * @Auther wj
 * @Date 2020/10/29 10:17
 */
@Mapper
public interface VioMiApiLogMapper {

    Long insert(VioMiApiLog vioMiApiLog);

    void updateViomiFlag(VioMiApiLog vioMiApiLog);

    List<VioMiApiLog> getList(Long b2bOrderId);

    ViomiApiInfo findApiById(Long id);

    Long getIdByOrderId(@Param("b2bOrderId") Long b2bOrderId, @Param("id") Long id);
}
