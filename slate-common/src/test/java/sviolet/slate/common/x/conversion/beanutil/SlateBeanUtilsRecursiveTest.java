package sviolet.slate.common.x.conversion.beanutil;

import org.junit.Assert;
import org.junit.Test;

import java.math.BigDecimal;
import java.sql.Date;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SlateBeanUtilsRecursiveTest {

    @Test
    public void test(){
        Assert.assertEquals("{cardList=[{date=2018-10-19, no=5465135464, percent=0.998}, {date=2018-10-19, no=5465135464, percent=0.998}], amount=3215.555, cardMap={1={date=2018-10-19, no=5465135464, percent=0.998}, 2={date=2018-10-19, no=5465135464, percent=0.998}}, name=wangwang, type=C, age=21, card={date=2018-10-19, no=5465135464, percent=0.998}}",
                String.valueOf(SlateBeanUtils.beanOrMapToMapRecursively(new Bean())));
    }

    public static class Bean {

        private String name = "wangwang";
        private int age = 21;
        private BigDecimal amount = new BigDecimal("3215.555");
        private Type type = Type.C;
        private List<Card> cardList = new ArrayList<>();
        private Map<String, Card> cardMap = new HashMap<>();
        private Card card = new Card();

        public Bean() {
            cardList.add(new Card());
            cardList.add(new Card());
            cardMap.put("1", new Card());
            cardMap.put("2", new Card());
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public int getAge() {
            return age;
        }

        public void setAge(int age) {
            this.age = age;
        }

        public BigDecimal getAmount() {
            return amount;
        }

        public void setAmount(BigDecimal amount) {
            this.amount = amount;
        }

        public Type getType() {
            return type;
        }

        public void setType(Type type) {
            this.type = type;
        }

        public List<Card> getCardList() {
            return cardList;
        }

        public void setCardList(List<Card> cardList) {
            this.cardList = cardList;
        }

        public Map<String, Card> getCardMap() {
            return cardMap;
        }

        public void setCardMap(Map<String, Card> cardMap) {
            this.cardMap = cardMap;
        }

        public Card getCard() {
            return card;
        }

        public void setCard(Card card) {
            this.card = card;
        }
    }

    public static class Card {

        private String no = "5465135464";
        private float percent = 0.998f;
        private Date date = new Date(new java.util.Date().getTime());

        public String getNo() {
            return no;
        }

        public void setNo(String no) {
            this.no = no;
        }

        public float getPercent() {
            return percent;
        }

        public void setPercent(float percent) {
            this.percent = percent;
        }

        public Date getDate() {
            return date;
        }

        public void setDate(Date date) {
            this.date = date;
        }
    }

    public static enum Type {
        A,
        B,
        C
    }

}
