package com.kkl.kklplus.b2b.viomi.service;

import com.kkl.kklplus.b2b.viomi.entity.VioMiMessageLog;
import com.kkl.kklplus.b2b.viomi.mapper.VioMiMessageLogMapper;
import com.kkl.kklplus.b2b.viomi.utils.QuarterUtils;
import com.kkl.kklplus.entity.b2bcenter.rpt.B2BOrderProcesslog;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.ibatis.annotations.Mapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;

/**
 * describe:
 *
 * @author chenxj
 * @date 2020/10/23
 */
@Service
@Slf4j
@Transactional(propagation = Propagation.NOT_SUPPORTED)
public class VioMiMessageLogService  {

    @Resource
    private VioMiMessageLogMapper vioMiMessageLogMapper;

    public VioMiMessageLog insert(Long createById,Long createDt,Long messageId){
        try {
            if(messageId == null || messageId == 0){
                return new VioMiMessageLog();
            }
            VioMiMessageLog messageLog = vioMiMessageLogMapper.getByMessageId(messageId);
            if(messageLog != null){
                messageLog.setDuplicateFlag(true);
                return messageLog;
            }
            messageLog = new VioMiMessageLog();
            messageLog.setMessageId(messageId);
            messageLog.setCreateById(createById);
            messageLog.setCreateDt(createDt);
            messageLog.setQuarter(QuarterUtils.getQuarter(createDt));
            vioMiMessageLogMapper.insert(messageLog);
            return messageLog;
        }catch (Exception e){
            log.error("消息处理新增失败:{}:{}",createById.toString(),messageId,e);
            return new VioMiMessageLog();
        }
    }

    public void updateProcessFlag(VioMiMessageLog messageLog){
        try {
            if(messageLog.getMessageId()== null || messageLog.getMessageId() == 0){
                return;
            }
            messageLog.setProcessComment(StringUtils.left(messageLog.getProcessComment(),255));
            messageLog.setUpdateDt(System.currentTimeMillis());
            messageLog.setProcessTime(messageLog.getProcessTime()+1);
            vioMiMessageLogMapper.updateProcessFlag(messageLog);
        }catch (Exception e){
            log.error("消息处理更新失败:{}",messageLog.toString(),e);
        }
    }
}
