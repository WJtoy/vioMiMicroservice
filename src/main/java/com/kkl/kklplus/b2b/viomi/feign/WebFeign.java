package com.kkl.kklplus.b2b.viomi.feign;

import com.kkl.kklplus.b2b.viomi.fallback.WebFeignFallbackFactory;
import com.kkl.kklplus.common.response.MSResponse;
import org.springframework.cloud.netflix.feign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;

@FeignClient(name = "kklplus-web", fallbackFactory = WebFeignFallbackFactory.class)
public interface WebFeign {

    @GetMapping(value = "/ms/b2bCenter/createOrderNo")
    MSResponse<String> createOrderNo();

    @GetMapping(value = "/ms/b2bCenter/generateComplainNo")
    MSResponse<String> generateComplainNo();
}
