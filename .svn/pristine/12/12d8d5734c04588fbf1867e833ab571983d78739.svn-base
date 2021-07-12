package com.kkl.kklplus.b2b.viomi.fallback;

import com.kkl.kklplus.b2b.viomi.feign.WebFeign;
import com.kkl.kklplus.common.exception.MSErrorCode;
import com.kkl.kklplus.common.response.MSResponse;
import com.kkl.kklplus.entity.b2bconfig.sd.B2BConfigRouting;
import feign.hystrix.FallbackFactory;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Component
public class WebFeignFallbackFactory implements FallbackFactory<WebFeign> {

    @Override
    public WebFeign create(Throwable throwable) {
        return new WebFeign() {

            @Override
            public MSResponse<String> createOrderNo() {
                return new MSResponse<>(MSErrorCode.FALLBACK_FAILURE);
            }

            @Override
            public MSResponse<String> generateComplainNo() {
                return new MSResponse<>(MSErrorCode.FALLBACK_FAILURE);
            }
        };
    }
}
