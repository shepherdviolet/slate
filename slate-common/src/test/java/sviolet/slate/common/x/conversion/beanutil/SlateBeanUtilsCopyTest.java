package sviolet.slate.common.x.conversion.beanutil;

import org.junit.Test;

import java.math.BigDecimal;
import java.util.concurrent.atomic.AtomicInteger;

public class SlateBeanUtilsCopyTest {

    @Test
    public void test(){
        System.setProperty("slate.beanutils.log", "true");
        System.setProperty("thistle.spi.loglv", "error");
        System.out.println(copyOnce());
    }

    private static To copyOnce() {
        From from = new From();
        from.name = "lalala";
        from.num = new BigDecimal("321");
        return SlateBeanUtils.copy(from, To.class);
    }

    public static class From {
        private String name;
        private BigDecimal num;
        public String getName() {
            return name;
        }
        public void setName(String name) {
            this.name = name;
        }
        public BigDecimal getNum() {
            return num;
        }
        public void setNum(BigDecimal num) {
            this.num = num;
        }
    }

    public static class To {
        private String name;
        private long num;
        public String getName() {
            return name;
        }
        public void setName(String name) {
            this.name = name;
        }
        public long getNum() {
            return num;
        }
        public void setNum(long num) {
            this.num = num;
        }
        @Override
        public String toString() {
            return "To{" +
                    "name='" + name + '\'' +
                    ", num=" + num +
                    '}';
        }
    }

    private static final AtomicInteger counter = new AtomicInteger(0);

    public static void main(String[] args) {
        System.setProperty("slate.beanutils.log", "true");
        System.setProperty("thistle.spi.loglv", "error");

        for (int i = 0 ; i < 1000 ; i++) {
            new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        Thread.sleep(100L);
                    } catch (InterruptedException ignored) {
                    }
                    long startTime = System.currentTimeMillis();
                    while (System.currentTimeMillis() - startTime < 10000) {
                        copyOnce();
                        counter.incrementAndGet();
                    }
                }
            }).start();
        }

        long startTime = System.currentTimeMillis();
        while (System.currentTimeMillis() - startTime < 11000) {
            Thread.yield();
        }
        System.out.println(counter.get());
    }

}
