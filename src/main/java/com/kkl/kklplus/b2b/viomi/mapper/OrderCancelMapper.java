package com.kkl.kklplus.b2b.viomi.mapper;

import com.kkl.kklplus.entity.b2bcenter.rpt.B2BOrderProcesslog;
import com.kkl.kklplus.entity.viomi.sd.VioMiOrderCancel;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface OrderCancelMapper {

    Integer insert(VioMiOrderCancel remark);

    Integer updateProcessFlag(VioMiOrderCancel remark);
}
