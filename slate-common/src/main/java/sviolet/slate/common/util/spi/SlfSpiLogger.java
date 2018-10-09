package sviolet.slate.common.util.spi;

import sviolet.thistle.x.common.thistlespi.DefaultSpiLogger;
import sviolet.thistle.x.common.thistlespi.SpiLogger;

/**
 * ThistleSpi采用SLF4J输出日志
 *
 * @author S.Violet
 */
public class SlfSpiLogger implements SpiLogger {

    static {
        // 若org.slf4j.Logger类存在则使用slf4j输出日志
        try {
            Class.forName("org.slf4j.Logger");
            provider = (SpiLogger) Class.forName("sviolet.slate.common.util.spi.SlfSpiLoggerProvider").newInstance();
        } catch (Exception e) {
            // 否则使用默认方式输出日志
            provider = new DefaultSpiLogger();
        }
    }

    private static SpiLogger provider;

    public SlfSpiLogger() {
    }

    @Override
    public void print(String s) {
        provider.print(s);
    }

    @Override
    public void print(String s, Throwable throwable) {
        provider.print(s, throwable);
    }

}
