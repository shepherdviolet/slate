package sviolet.slate.common.x.conversion.beanutil;

import ch.qos.logback.classic.Level;
import org.junit.Assert;
import org.junit.Test;
import sviolet.slate.common.helper.logback.LogbackHelper;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SlateBeanUtilsFromToMapTest {

    @Test
    public void toMap(){
//        System.out.println(EnvironmentUtils.PID);
        System.setProperty("slate.beanutils.log", "false");
        Bean bean = new Bean();
        bean.p1 = "lalala";
        bean.p2 = new BigDecimal("321.333");
        bean.p3 = 321;
        bean.p4 = 0x41;
        String result = String.valueOf(SlateBeanUtils.beanToMap(bean));
//        System.out.println(result);
        Assert.assertEquals("{p1=lalala, p2=321.333, p3=321, p4=A, p5=null, p6=null}",
                result);
    }

    @Test
    public void fromMap() throws InterruptedException {
//        System.out.println(EnvironmentUtils.PID);
        System.setProperty("slate.beanutils.log", "false");
        LogbackHelper.setLevel("com.github.shepherdviolet.glaciion", Level.OFF);
        Map<String, Object> map = new HashMap<>();
        map.put("p1", "lalala");
        map.put("p2", 123.333);
        map.put("p3", 321);
        map.put("p4", (char)0x41);
        map.put("p5", new ArrayList<Object>(){{add(new Bean()); add(new Bean());}});
        map.put("p6", new HashMap<String, Object>(){{put("a", new Bean()); put("b", new Bean());}});
        String result = String.valueOf(SlateBeanUtils.mapToBean(map, Bean.class, true, true));
//        System.out.println(result);
        Assert.assertEquals("Bean{p1='lalala', p2=123.333, p3=321, p4=A, p5=[Bean{p1='null', p2=null, p3=0, p4=B, p5=null, p6=null}, Bean{p1='null', p2=null, p3=0, p4=B, p5=null, p6=null}], p6={a=Bean{p1='null', p2=null, p3=0, p4=B, p5=null, p6=null}, b=Bean{p1='null', p2=null, p3=0, p4=B, p5=null, p6=null}}}",
                result);
    }

    private static class Bean {
        private String p1;
        private BigDecimal p2;
        private int p3;
        private char p4 = 0x42;
        private List<Bean> p5;
        private Map<String, Bean> p6;

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

        public List<Bean> getP5() {
            return p5;
        }

        public void setP5(List<Bean> p5) {
            this.p5 = p5;
        }

        public Map<String, Bean> getP6() {
            return p6;
        }

        public void setP6(Map<String, Bean> p6) {
            this.p6 = p6;
        }

        @Override
        public String toString() {
            return "Bean{" +
                    "p1='" + p1 + '\'' +
                    ", p2=" + p2 +
                    ", p3=" + p3 +
                    ", p4=" + p4 +
                    ", p5=" + p5 +
                    ", p6=" + p6 +
                    '}';
        }
    }

}
