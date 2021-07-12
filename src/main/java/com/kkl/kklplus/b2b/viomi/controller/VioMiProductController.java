package com.kkl.kklplus.b2b.viomi.controller;


import com.google.gson.FieldNamingPolicy;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import com.kkl.kklplus.b2b.viomi.entity.VioMiOrderSnCode;
import com.kkl.kklplus.b2b.viomi.http.command.OperationCommand;
import com.kkl.kklplus.b2b.viomi.http.request.ProductPartsRequestParam;
import com.kkl.kklplus.b2b.viomi.http.request.RequestParam;
import com.kkl.kklplus.b2b.viomi.http.response.FaultTypeResponse;
import com.kkl.kklplus.b2b.viomi.http.response.ProductResponse;
import com.kkl.kklplus.b2b.viomi.http.response.ResponseBody;
import com.kkl.kklplus.b2b.viomi.http.utils.OkHttpUtils;
import com.kkl.kklplus.b2b.viomi.service.B2BProcesslogService;
import com.kkl.kklplus.b2b.viomi.service.SysLogService;
import com.kkl.kklplus.b2b.viomi.service.VioMiOrderSnCodeService;
import com.kkl.kklplus.b2b.viomi.utils.QuarterUtils;
import com.kkl.kklplus.common.exception.MSErrorCode;
import com.kkl.kklplus.common.response.MSResponse;
import com.kkl.kklplus.entity.b2bcenter.rpt.B2BOrderProcesslog;
import com.kkl.kklplus.entity.viomi.sd.FaultType;
import com.kkl.kklplus.entity.viomi.sd.ProductParts;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Date;
import java.util.List;

@Slf4j
@RestController
@RequestMapping("/vioMiProduct")
public class VioMiProductController {

    @Autowired
    private VioMiOrderSnCodeService vioMiOrderSnCodeService;

    @Autowired
    private B2BProcesslogService b2BProcesslogService;

    @Autowired
    private SysLogService sysLogService;

    private static Gson gson = new GsonBuilder().setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES).create();

    @ApiOperation("获取产品配件")
    @GetMapping("/getProductParts/{product69Code}/{createById}")
    public MSResponse<List<ProductParts>> getProductParts(@PathVariable("product69Code")String product69Code,
                                      @PathVariable("createById")Long createById){
        if(product69Code == null || createById == null){
            return new MSResponse(new MSErrorCode(1000, "参数错误，产品或操作人不能为空"));
        }
        try {
            ProductPartsRequestParam productPartsRequestParam = new ProductPartsRequestParam();
            productPartsRequestParam.setProduct_69Code(product69Code);
            String json = gson.toJson(productPartsRequestParam);
            B2BOrderProcesslog b2BProcesslog = new B2BOrderProcesslog();
            b2BProcesslog.setInfoJson(json);
            b2BProcesslog.setInterfaceName(OperationCommand.OperationCode.PRODUCT_PARTS.apiUrl);
            b2BProcesslog.setCreateById(createById);
            b2BProcesslog.setUpdateById(createById);
            b2BProcesslog.setCreateDt(System.currentTimeMillis());
            b2BProcesslog.setUpdateDt(System.currentTimeMillis());
            b2BProcesslog.setQuarter(QuarterUtils.getQuarter(new Date()));
            b2BProcesslog.setProcessFlag(0);
            b2BProcesslog.setProcessTime(0);
            b2BProcesslogService.insert(b2BProcesslog);
            OperationCommand command = OperationCommand.newInstance
                    (OperationCommand.OperationCode.PRODUCT_PARTS, productPartsRequestParam);
            ResponseBody<ProductResponse> resBody = OkHttpUtils.postSyncGenericNew(command, ProductResponse.class);
            List<ProductParts> productParts = null;
            if ( resBody != null && resBody.getErrorCode() == ResponseBody.ErrorCode.SUCCESS.code && resBody.getData().getData() != null) {
                b2BProcesslog.setProcessComment(resBody.getErrorMsg());
                b2BProcesslog.setUpdateDt(System.currentTimeMillis());
                b2BProcesslog.setResultJson(resBody.getOriginalJson());
                b2BProcesslog.setProcessFlag(4);
                b2BProcesslogService.updateProcessFlag(b2BProcesslog);
                productParts = resBody.getData().getData();
            }else {
                b2BProcesslog.setProcessComment(resBody.getErrorMsg());
                b2BProcesslog.setUpdateDt(System.currentTimeMillis());
                b2BProcesslog.setResultJson(resBody.getOriginalJson());
                b2BProcesslog.setProcessFlag(3);
                b2BProcesslogService.updateProcessFlag(b2BProcesslog);
                return new MSResponse(new MSErrorCode(1000,StringUtils.left(resBody.getErrorMsg(),200)));
            }
            return new MSResponse(productParts);
        }catch (Exception e){
            log.error("获取产品配件:{}",product69Code,e);
            sysLogService.insert(1L,product69Code, e.getMessage(),
                    "获取产品配件","vioMiProduct/getProductParts", "POST");
            return new MSResponse(new MSErrorCode(1000, StringUtils.left(e.getMessage(),200)));
        }
    }


    @ApiOperation("获取故障类别")
    @PostMapping("/getFaultType")
    public MSResponse<List<FaultType>> getFaultType(){
        try {
            RequestParam requestParam = new RequestParam();
            B2BOrderProcesslog b2BProcesslog = new B2BOrderProcesslog();
            b2BProcesslog.setInterfaceName(OperationCommand.OperationCode.FAULT_TYPE.apiUrl);
            b2BProcesslog.setCreateById(1L);
            b2BProcesslog.setUpdateById(1L);
            b2BProcesslog.setCreateDt(System.currentTimeMillis());
            b2BProcesslog.setUpdateDt(System.currentTimeMillis());
            b2BProcesslog.setQuarter(QuarterUtils.getQuarter(new Date()));
            b2BProcesslog.setProcessFlag(0);
            b2BProcesslog.setProcessTime(0);
            b2BProcesslogService.insert(b2BProcesslog);
            OperationCommand command = OperationCommand.newInstance
                    (OperationCommand.OperationCode.FAULT_TYPE, requestParam);
            ResponseBody<FaultTypeResponse> resBody = OkHttpUtils.postSyncGenericNew(command, FaultTypeResponse.class);
            List<FaultType> faultTypes = null;
            if (resBody !=null && resBody.getErrorCode() == ResponseBody.ErrorCode.SUCCESS.code &&  resBody.getData().getData() != null) {

                b2BProcesslog.setProcessComment(resBody.getErrorMsg());
                b2BProcesslog.setUpdateDt(System.currentTimeMillis());
                b2BProcesslog.setResultJson(resBody.getOriginalJson());
                b2BProcesslog.setProcessFlag(4);
                b2BProcesslogService.updateProcessFlag(b2BProcesslog);
                faultTypes = resBody.getData().getData();
            }else {
                b2BProcesslog.setProcessComment(resBody.getErrorMsg());
                b2BProcesslog.setUpdateDt(System.currentTimeMillis());
                b2BProcesslog.setResultJson(resBody.getOriginalJson());
                b2BProcesslog.setProcessFlag(3);
                b2BProcesslogService.updateProcessFlag(b2BProcesslog);
                new MSErrorCode(1000,StringUtils.left(resBody.getErrorMsg(),200));
            }
            return new MSResponse(faultTypes);
        }catch (Exception e){
            log.error("获取故障类型:{}","",e);
            sysLogService.insert(1L,"", e.getMessage(),
                    "获取故障类型","vioMiProduct/getFaultType", "POST");
            return new MSResponse(new MSErrorCode(1000, StringUtils.left(e.getMessage(),200)));
        }
    }

    /**
     * 产品SN码验证
     * @param vioMiOrderSnCode
     * @return
     */
    @PostMapping("getGradeSn")
    public MSResponse getGradeSn(@RequestBody VioMiOrderSnCode vioMiOrderSnCode) {
        return vioMiOrderSnCodeService.getGradeSnToVioMi(vioMiOrderSnCode);
    }

}
