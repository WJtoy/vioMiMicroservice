package com.kkl.kklplus.b2b.viomi.controller;

import com.kkl.kklplus.b2b.viomi.service.VioMiOrderReviewService;
import com.kkl.kklplus.common.response.MSResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("orderReview")
public class VioMiOrderReviewController {

    @Autowired
    private VioMiOrderReviewService vioMiOrderReviewService;

    /**
     * 重送订单到云米
     * @param apiLogId
     * @param operator
     * @return
     */
    @PostMapping("resend")
    public MSResponse<Integer> resend(@RequestParam Long apiLogId, @RequestParam String operator, @RequestParam Long operatorId) {
        return vioMiOrderReviewService.resend(apiLogId, operator, operatorId);
    }

}
