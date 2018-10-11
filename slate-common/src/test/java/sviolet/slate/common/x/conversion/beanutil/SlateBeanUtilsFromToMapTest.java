package sviolet.slate.common.x.conversion.beanutil;

import org.junit.Test;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

public class SlateBeanUtilsFromToMapTest {

    @Test
    public void toMap(){
        System.setProperty("slate.beanutils.log", "true");
        System.setProperty("thistle.spi.loglv", "error");
        Bean bean = new Bean();
        bean.name = "lalala";
        bean.no = new BigDecimal("321.333");
        System.out.println(SlateBeanUtils.toMap(bean));
    }

    @Test
    public void fromMap(){
        System.setProperty("slate.beanutils.log", "true");
        System.setProperty("thistle.spi.loglv", "error");
        Map<String, Object> map = new HashMap<>();
        map.put("name", "lalala");
        map.put("no", 123.333);
        System.out.println(SlateBeanUtils.fromMap(map, Bean.class, true));
    }

    private static class Bean {
        private String name;
        private BigDecimal no;
        public String getName() {
            return name;
        }
        public void setName(String name) {
            this.name = name;
        }
        public BigDecimal getNo() {
            return no;
        }
        public void setNo(BigDecimal no) {
            this.no = no;
        }
        @Override
        public String toString() {
            return "Bean{" +
                    "name='" + name + '\'' +
                    ", no=" + no +
                    '}';
        }
    }

}
