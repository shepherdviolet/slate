package sviolet.slate.common.x.monitor.txtimer.def;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import sviolet.thistle.util.judge.CheckUtils;

/**
 * 用于Spring容器中修改配置(仅支持部分配置), 引入本配置类即可.
 *
 * <code>@Import(DefaultTxTimerSpringConfig.class)</code>
 *
 * @author S.Violet
 */
@Configuration
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
