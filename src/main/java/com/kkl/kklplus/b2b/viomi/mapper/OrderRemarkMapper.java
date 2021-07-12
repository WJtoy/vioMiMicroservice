package com.kkl.kklplus.b2b.viomi.mapper;

import com.kkl.kklplus.entity.viomi.sd.VioMiOrderRemark;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface OrderRemarkMapper {

    Integer insert(VioMiOrderRemark remark);

    Integer updateProcessFlag(VioMiOrderRemark remark);

}
