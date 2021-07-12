package com.kkl.kklplus.b2b.viomi.controller;

import com.kkl.kklplus.b2b.viomi.service.VioMiOrderSendSmsService;
import com.kkl.kklplus.common.exception.MSErrorCode;
import com.kkl.kklplus.common.response.MSResponse;
import com.kkl.kklplus.entity.viomi.sd.VioMiOrderSendSms;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;


/**
 * @author wln
 */
@Slf4j
@RestController
@RequestMapping("/viomiOrderInfo/sendSms")
public class VioMiOrderSendSmsController {

    @Autowired
    private VioMiOrderSendSmsService vioMiOrderSendSmsService;

    @PostMapping("")
    public MSResponse sendSms(@RequestBody VioMiOrderSendSms vioMiOrderSendSms){
        return vioMiOrderSendSmsService.sendSmsToVioMi(vioMiOrderSendSms,"2905228");
    }
    @PostMapping("cancelValidateCode")
    public MSResponse cancelValidateCode(@RequestBody VioMiOrderSendSms vioMiOrderSendSms){
        String reason = vioMiOrderSendSms.getReason();
        if(StringUtils.isBlank(reason)){
            return new MSResponse(new MSErrorCode(MSErrorCode.FAILURE.getCode(),"撤销原因不能为空"));
        }
        return vioMiOrderSendSmsService.sendSmsToVioMi(vioMiOrderSendSms,"1003");
    }

}
