package sviolet.slate.common.utilx.txtimer.def;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import sviolet.thistle.util.judge.CheckUtils;

/**
 * 用于Spring容器中修改配置, 将该类注册为Bean即可(仅支持部分配置)
 *
 * @author S.Violet
 */
@Component
public class DefaultTxTimerSpringConfig {

    @Value("${slate.txtimer.reportall.interval:}")
    private void setReportAllInterval(String reportAllInterval){
        if (!CheckUtils.isEmptyOrBlank(reportAllInterval)){
            DefaultTxTimerConfig.setReportAllInterval(reportAllInterval);
        }
    }

    @Value("${slate.txtimer.threshold.avg:}")
    private void setThresholdAvg(String avg){
        if (!CheckUtils.isEmptyOrBlank(avg)){
            DefaultTxTimerConfig.setThresholdAvg(avg);
        }
    }

    @Value("${slate.txtimer.threshold.max:}")
    private void setThresholdMax(String max){
        if (!CheckUtils.isEmptyOrBlank(max)){
            DefaultTxTimerConfig.setThresholdMax(max);
        }
    }

    @Value("${slate.txtimer.threshold.min:}")
    private void setThresholdMin(String min){
        if (!CheckUtils.isEmptyOrBlank(min)){
            DefaultTxTimerConfig.setThresholdMin(min);
        }
    }

}
