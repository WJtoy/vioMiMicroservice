package com.kkl.kklplus.b2b.viomi;

import com.kkl.kklplus.b2b.viomi.service.ViomiApiLogService;
import com.kkl.kklplus.b2b.viomi.utils.QuarterUtils;
import com.kkl.kklplus.entity.viomi.sd.VioMiApiLog;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.Date;
import java.util.List;

@RunWith(SpringRunner.class)
@SpringBootTest
@ActiveProfiles("dev")
public class B2BVioMIApplicationTests {

    @Autowired
    ViomiApiLogService viomiApiLogService;


    @Test
    public void test() {
        VioMiApiLog vioMiApiLog = new VioMiApiLog();
        vioMiApiLog.setB2bOrderId(1310463623860195328l);
        vioMiApiLog.setOperatingStatus(1);
        vioMiApiLog.setCreateDt(System.currentTimeMillis());
        vioMiApiLog.setUpdateDt(System.currentTimeMillis());
        vioMiApiLog.setCreateBy(1l);
        vioMiApiLog.setUpdateBy(1L);
        vioMiApiLog.setInfoJson("{\"data\": {\"location\": \"123,456\"}, \"handle\": {\"way\": \"上门测试\", \"node\": \"打卡测试\", \"remarks\": \"\", \"operator\": \"李四\"}, \"order_number\": \"A147258369\"}");
        vioMiApiLog.setInterfaceName("测试");
        vioMiApiLog.setProcessComment("测试?存在");
        vioMiApiLog.setProcessFlag(1);
        vioMiApiLog.setProcessTime(1);
        vioMiApiLog.setQuarter(QuarterUtils.getQuarter(new Date()));
        vioMiApiLog.setResultJson("{\"msg\": \"测试存在啦\", \"res\": 5, \"data\": []}");
        viomiApiLogService.insert(vioMiApiLog);

    }

    @Test
    public void test2(){
        VioMiApiLog vioMiApiLog = new VioMiApiLog();
        vioMiApiLog.setId(1321652251021938688L);
        vioMiApiLog.setProcessFlag(3);
        vioMiApiLog.setResultJson("[{}]");
        vioMiApiLog.setUpdateBy(System.currentTimeMillis());
        viomiApiLogService.updateProcessFlag(vioMiApiLog);
    }

    @Test
    public void test3(){
        List<VioMiApiLog> result = viomiApiLogService.getVioMiApiLogList(1318141143229272065L);
        System.out.println(result);
    }

}
