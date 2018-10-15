package sviolet.slate.common.x.conversion.beanutil.safe.date;

import org.junit.Test;
import sviolet.slate.common.x.conversion.beanutil.MappingRuntimeException;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

import static org.junit.Assert.*;

public class SBUMapperString2UtilDateTest {

    private SBUMapperString2UtilDate mapper = new SBUMapperString2UtilDate();

    @Test
    public void testSucceed() throws ParseException {
        test("2018-10-15 09:26:21.333", "2018-10-15 09:26:21.333");
        test("2018-10-15 09:26:21.000", "2018-10-15 09:26:21");
        test("2018-10-15 00:00:00.000", "2018-10-15");
        test("2018-10-15 09:26:21.333", "2018-10-15 09:26:21,333");
        test("2018-10-15 09:26:21.000", "20181015092621");
        test("2018-10-15 00:00:00.000", "20181015");
    }

    @Test(expected = MappingRuntimeException.class)
    public void testFailed1() throws ParseException {
        test("2018-10-15 09:26:21.333", "2018-10-15 09:26:21?333");
    }

    @Test(expected = MappingRuntimeException.class)
    public void testFailed2() throws ParseException {
        test("2018-10-15 09:26:21.333", "2018-10-15 09:26:21.33");
    }

    @Test(expected = MappingRuntimeException.class)
    public void testFailed3() throws ParseException {
        test("2018-10-15 09:26:21.333", "2018-10-1509:26:21");
    }

    private void test(String expected, String actual) throws ParseException {
        assertEquals(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS").parse(expected),
                map(actual));
    }

    private Date map(String dateString){
        return (Date) mapper.map(dateString, Date.class, null, false);
    }

}
