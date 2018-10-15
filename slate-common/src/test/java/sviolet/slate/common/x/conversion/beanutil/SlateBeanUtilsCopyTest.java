package sviolet.slate.common.x.conversion.beanutil;

import org.junit.Test;
import sviolet.thistle.util.common.EnvironmentUtils;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

public class SlateBeanUtilsCopyTest {

    @Test
    public void test() throws InterruptedException {
        System.out.println(EnvironmentUtils.PID);
        System.setProperty("slate.beanutils.log", "true");
        System.setProperty("thistle.spi.loglv", "error");
        System.out.println(copyOnce());
    }

    private static To copyOnce() {
        From from = new From();
        from.p1 = "abc";
        from.p2 = 21;
        from.p3 = new BigDecimal("5656.222");
        from.p4 = new ArrayList<String>(3);
        from.p4.add("wo1");
        from.p4.add("wo2");
        from.p4.add("wo3");
        from.p5 = new HashMap<String, Object>(2);
        from.p5.put("m1", "v1");
        from.p5.put("m2", "v2");
        from.p6 = 0x41;
        return SlateBeanUtils.copy(from, To.class);
    }

    public static class From {
        private String p1;
        private int p2;
        private BigDecimal p3;
        private List p4;
        private Map p5;
        private char p6;

        public String getP1() {
            return p1;
        }

        public void setP1(String p1) {
            this.p1 = p1;
        }

        public int getP2() {
            return p2;
        }

        public void setP2(int p2) {
            this.p2 = p2;
        }

        public BigDecimal getP3() {
            return p3;
        }

        public void setP3(BigDecimal p3) {
            this.p3 = p3;
        }

        public List<String> getP4() {
            return p4;
        }

        public void setP4(List<String> p4) {
            this.p4 = p4;
        }

        public Map<String, Object> getP5() {
            return p5;
        }

        public void setP5(Map<String, Object> p5) {
            this.p5 = p5;
        }

        public char getP6() {
            return p6;
        }

        public void setP6(char p6) {
            this.p6 = p6;
        }
    }

    public static class To {
        private String p1;
        private int p2;
        private BigDecimal p3;
        private List p4;
        private Map p5;
        private char p6;

        public String getP1() {
            return p1;
        }

        public void setP1(String p1) {
            this.p1 = p1;
        }

        public int getP2() {
            return p2;
        }

        public void setP2(int p2) {
            this.p2 = p2;
        }

        public BigDecimal getP3() {
            return p3;
        }

        public void setP3(BigDecimal p3) {
            this.p3 = p3;
        }

        public List<String> getP4() {
            return p4;
        }

        public void setP4(List<String> p4) {
            this.p4 = p4;
        }

        public Map<String, Object> getP5() {
            return p5;
        }

        public void setP5(Map<String, Object> p5) {
            this.p5 = p5;
        }

        public char getP6() {
            return p6;
        }

        public void setP6(char p6) {
            this.p6 = p6;
        }

        @Override
        public String toString() {
            return "To{" +
                    "p1='" + p1 + '\'' +
                    ", p2=" + p2 +
                    ", p3=" + p3 +
                    ", p4=" + p4 +
                    ", p5=" + p5 +
                    ", p6=" + p6 +
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
