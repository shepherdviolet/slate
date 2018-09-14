package sviolet.slate.common.util.spi;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import sviolet.thistle.util.spi.SpiLogger;

/**
 * ThistleSpi采用SLF4J输出日志(提供者)
 *
 * @author S.Violet
 */
class SlfSpiLoggerProvider implements SpiLogger {

    private static final Logger logger = LoggerFactory.getLogger(SlfSpiLogger.class);

    SlfSpiLoggerProvider() {
    }

    @Override
    public void print(String s) {
        logger.info(s);
    }

    @Override
    public void print(String s, Throwable throwable) {
        logger.error(s, throwable);
    }

}
