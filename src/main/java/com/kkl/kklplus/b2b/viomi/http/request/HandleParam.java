package com.kkl.kklplus.b2b.viomi.http.request;

import com.kkl.kklplus.entity.viomi.sd.VioMiParts;
import lombok.Data;

import java.util.List;

/**
 *办理信息
 * @author chenxj
 * @date 2020/09/18
 */
@Data
public class HandleParam {
    /**
     *办理方式
     */
    private String way;
    /**
     * 操作方式
     */
    private String node;
    /**
     *操作人
     */
    private String operator;
    /**
     *备注信息
     */
    private String remarks = "";
    /**
     * 撤销原因
     */
    private String reason;
    /**
     *撤销申请验证码
     */
    private String code;
    /**
     * 配件列表
     */
    private List<VioMiParts> parts;
}
