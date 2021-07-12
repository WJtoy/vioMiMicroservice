package com.kkl.kklplus.b2b.viomi.mapper;

import com.kkl.kklplus.b2b.viomi.entity.VioMiMessageLog;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface VioMiMessageLogMapper {

    Integer insert(VioMiMessageLog messageLog);

    Integer updateProcessFlag(VioMiMessageLog messageLog);

    VioMiMessageLog getByMessageId(@Param("messageId") Long messageId);
}
