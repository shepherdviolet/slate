package sviolet.slate.common.x.conversion.beanutil;

import org.junit.Test;
import sviolet.thistle.util.common.EnvironmentUtils;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

public class SlateBeanUtilsFromToMapTest {

    @Test
    public void toMap(){
//        System.out.println(EnvironmentUtils.PID);
        System.setProperty("slate.beanutils.log", "false");
        System.setProperty("thistle.spi.loglv", "error");
        Bean bean = new Bean();
        bean.p1 = "lalala";
        bean.p2 = new BigDecimal("321.333");
        bean.p3 = 321;
        bean.p4 = 0x41;
        String result = String.valueOf(SlateBeanUtils.beanToMap(bean));
//        System.out.println(result);
    }

    @Test
    public void fromMap() throws InterruptedException {
//        System.out.println(EnvironmentUtils.PID);
        System.setProperty("slate.beanutils.log", "false");
        System.setProperty("thistle.spi.loglv", "error");
        Map<String, Object> map = new HashMap<>();
        map.put("p1", "lalala");
        map.put("p2", 123.333);
        map.put("p3", 321);
        map.put("p4", (char)0x41);
        String result = String.valueOf(SlateBeanUtils.mapToBean(map, Bean.class, true, true));
//        System.out.println(result);
    }

    private static class Bean {
        private String p1;
        private BigDecimal p2;
        private int p3;
        private char p4;

        public String getP1() {
            return p1;
        }

        public void setP1(String p1) {
            this.p1 = p1;
        }

        public BigDecimal getP2() {
            return p2;
        }

        public void setP2(BigDecimal p2) {
            this.p2 = p2;
        }

        public int getP3() {
            return p3;
        }

        public void setP3(int p3) {
            this.p3 = p3;
        }

        public char getP4() {
            return p4;
        }

        public void setP4(char p4) {
            this.p4 = p4;
        }

        @Override
        public String toString() {
            return "Bean{" +
                    "p1='" + p1 + '\'' +
                    ", p2=" + p2 +
                    ", p3=" + p3 +
                    ", p4=" + p4 +
                    '}';
        }
    }

}
