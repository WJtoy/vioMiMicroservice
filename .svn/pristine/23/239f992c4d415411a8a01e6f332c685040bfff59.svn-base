package com.kkl.kklplus.b2b.viomi.service;

import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import com.google.gson.Gson;
import com.kkl.kklplus.b2b.viomi.entity.SysLog;
import com.kkl.kklplus.b2b.viomi.mapper.B2BProcesslogMapper;
import com.kkl.kklplus.b2b.viomi.mapper.SysLogMapper;
import com.kkl.kklplus.b2b.viomi.utils.QuarterUtils;
import com.kkl.kklplus.entity.b2b.common.B2BProcessFlag;
import com.kkl.kklplus.entity.b2b.rpt.B2BProcesslog;
import com.kkl.kklplus.entity.b2bcenter.rpt.B2BOrderProcesslog;
import com.kkl.kklplus.entity.b2bcenter.rpt.B2BProcessLogSearchModel;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;

@Service
@Slf4j
@Transactional(propagation = Propagation.NOT_SUPPORTED)
public class B2BProcesslogService {

    @Resource
    private B2BProcesslogMapper b2BProcesslogMapper;

    @Autowired
    private SysLogService sysLogService;

    /**
     * 添加原始数据
     * @param
     */
    public Long insert(B2BOrderProcesslog b2BProcessLog){
    return b2BProcesslogMapper.insert(b2BProcessLog);
    }

    public void updateProcessFlag(B2BOrderProcesslog b2BProcessLog) {
        try{
            b2BProcessLog.preUpdate();
            b2BProcessLog.setProcessComment(StringUtils.left(b2BProcessLog.getProcessComment(),200));
            b2BProcesslogMapper.updateProcessFlag(b2BProcessLog);
        }catch (Exception e) {
            log.error("原始数据结果修改错误:{}", b2BProcessLog.toString(),e);
            SysLog sysLog = new SysLog();
            sysLog.setCreateDt(System.currentTimeMillis());
            sysLog.setType(1);
            sysLog.setCreateById(1L);
            sysLog.setParams(new Gson().toJson(b2BProcessLog));
            sysLog.setException( e.getMessage());
            sysLog.setTitle("原始数据结果修改错误");
            sysLog.setQuarter(QuarterUtils.getQuarter(sysLog.getCreateDt()));
            sysLogService.insert(sysLog);
        }
    }

    public Page<B2BOrderProcesslog> getList(B2BProcessLogSearchModel processLogSearchModel, String code) {
        if (processLogSearchModel.getPage() != null) {
            PageHelper.startPage(processLogSearchModel.getPage().getPageNo(), processLogSearchModel.getPage().getPageSize());
            return b2BProcesslogMapper.getList(processLogSearchModel,code);
        }
        else {
            return null;
        }
    }

    public B2BOrderProcesslog insertPushApiLog(String json,String interfaceName){
        B2BOrderProcesslog processlog = new B2BOrderProcesslog();
        processlog.setInterfaceName(interfaceName);
        processlog.setInfoJson(json);
        processlog.setProcessFlag(B2BProcessFlag.PROCESS_FLAG_ACCEPT.value);
        processlog.setProcessTime(0);
        processlog.setCreateById(1L);
        processlog.setUpdateById(1L);
        processlog.preInsert();
        processlog.setQuarter(QuarterUtils.getQuarter(processlog.getCreateDt()));
        b2BProcesslogMapper.insert(processlog);
        return processlog;
    }
}
