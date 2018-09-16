package sviolet.slate.common.util.spi;

import sviolet.thistle.util.spi.DefaultSpiLogger;
import sviolet.thistle.util.spi.SpiLogger;

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
            isSlf4jSupported = true;
        } catch (Exception e) {
            // 否则使用默认方式输出日志
            provider = new DefaultSpiLogger();
            isSlf4jSupported = false;
        }
    }

    private static SpiLogger provider;
    private static boolean isSlf4jSupported;

    public SlfSpiLogger() {
        if (isSlf4jSupported) {
            print("? ThistleSpi | SlfSpiLogger: Slf4j logger enabled");
        } else {
            print("? ThistleSpi | SlfSpiLogger: Slf4j logger disabled");
        }
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
