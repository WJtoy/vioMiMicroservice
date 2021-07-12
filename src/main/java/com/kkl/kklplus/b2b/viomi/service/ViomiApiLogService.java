package com.kkl.kklplus.b2b.viomi.service;

import com.google.gson.Gson;
import com.kkl.kklplus.b2b.viomi.entity.SysLog;
import com.kkl.kklplus.b2b.viomi.entity.ViomiApiInfo;
import com.kkl.kklplus.b2b.viomi.mapper.VioMiApiLogMapper;
import com.kkl.kklplus.b2b.viomi.utils.QuarterUtils;
import com.kkl.kklplus.entity.b2b.common.B2BProcessFlag;
import com.kkl.kklplus.entity.viomi.sd.VioMiApiLog;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * @Auther wj
 * @Date 2020/10/29 10:15
 */
@Service
@Slf4j
@Transactional(propagation = Propagation.NOT_SUPPORTED)
public class ViomiApiLogService {


    @Autowired
    private VioMiApiLogMapper vioMiApiLogMapper;

    @Autowired
    private SysLogService sysLogService;

    /**
     * 添加原始数据
     * @param
     */
    public Long insert(VioMiApiLog vioMiApiLog){
        return vioMiApiLogMapper.insert(vioMiApiLog);
    }

    public void updateProcessFlag(VioMiApiLog vioMiApiLog) {
        try{
            vioMiApiLog.setUpdateDt(System.currentTimeMillis());
            vioMiApiLog.setProcessComment(StringUtils.left(vioMiApiLog.getProcessComment(),200));
            vioMiApiLogMapper.updateViomiFlag(vioMiApiLog);
        }catch (Exception e) {
            log.error("原始数据结果修改错误:{}", vioMiApiLog.toString(),e);
            SysLog sysLog = new SysLog();
            sysLog.setCreateDt(System.currentTimeMillis());
            sysLog.setType(1);
            sysLog.setCreateById(1L);
            sysLog.setParams(new Gson().toJson(vioMiApiLog));
            sysLog.setException( e.getMessage());
            sysLog.setTitle("原始数据结果修改错误");
            sysLog.setQuarter(QuarterUtils.getQuarter(sysLog.getCreateDt()));
            sysLogService.insert(sysLog);
        }
    }



    public VioMiApiLog insertPushApiLog(String json, String interfaceName, Integer operatingStatus, Long B2bOrderId){
        VioMiApiLog vioMiApiLog = new VioMiApiLog();
        vioMiApiLog.setOperatingStatus(operatingStatus);
        vioMiApiLog.setB2bOrderId(B2bOrderId);
        vioMiApiLog.setInterfaceName(interfaceName);
        vioMiApiLog.setInfoJson(json);
        vioMiApiLog.setProcessFlag(B2BProcessFlag.PROCESS_FLAG_ACCEPT.value);
        vioMiApiLog.setProcessTime(0);
        vioMiApiLog.setCreateBy(1L);
        vioMiApiLog.setUpdateBy(1L);
        vioMiApiLog.setUpdateDt(System.currentTimeMillis());
        vioMiApiLog.setCreateDt(System.currentTimeMillis());
        vioMiApiLog.setQuarter(QuarterUtils.getQuarter(vioMiApiLog.getCreateDt()));
        vioMiApiLogMapper.insert(vioMiApiLog);
        return vioMiApiLog;
    }


    public List<VioMiApiLog> getVioMiApiLogList(Long b2bOrderId){
        return  vioMiApiLogMapper.getList(b2bOrderId);
    }



    public ViomiApiInfo getById(Long id) {
        return vioMiApiLogMapper.findApiById(id);
    }

    public Long getIdByOrderId(Long b2bOrderId, Long id) {
        return vioMiApiLogMapper.getIdByOrderId(b2bOrderId, id);
    }

}
