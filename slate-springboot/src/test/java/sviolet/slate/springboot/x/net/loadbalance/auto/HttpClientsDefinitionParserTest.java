package sviolet.slate.springboot.x.net.loadbalance.auto;

import org.junit.Test;

import java.util.Map;

public class HttpClientsDefinitionParserTest {

    private static final String DEFINITION = "{\n" +
            "    \"default\":{\n" +
            "        \"hosts\":\"http://baidu.com,https://github.com\",\n" +
            "        \"initiativeInspectInterval\":5000,\n" +
            "        \"returnNullIfAllBlocked\":false,\n" +
            "        \"httpGetInspectorUrlSuffix\":\"+telnet+\",\n" +
            "        \"inspectorVerboseLog\":false,\n" +
            "        \"passiveBlockDuration\":6000,\n" +
            "        \"mediaType\":\"application/json;charset=utf-8\",\n" +
            "        \"encode\":\"utf-8\",\n" +
            "        \"headers\":{\n" +
            "            \"header1\":\"header1\",\n" +
            "            \"header2\":\"header2\"\n" +
            "        },\n" +
            "        \"recoveryCoefficient\":10,\n" +
            "        \"maxIdleConnections\":16,\n" +
            "        \"maxThreads\":200,\n" +
            "        \"maxThreadsPerHost\":200,\n" +
            "        \"connectTimeout\":3000,\n" +
            "        \"writeTimeout\":10000,\n" +
            "        \"readTimeout\":10000,\n" +
            "        \"maxReadLength\":10485760,\n" +
            "        \"httpCodeNeedBlock\":\"400,500\",\n" +
            "        \"verboseLog\":false,\n" +
            "        \"txTimerEnabled\":false\n" +
            "    },\n" +
            "    \"backup\":{\n" +
            "        \"hosts\":\"http://baidu.com,https://github.com\",\n" +
            "        \"initiativeInspectInterval\":5000,\n" +
            "        \"returnNullIfAllBlocked\":false,\n" +
            "        \"httpGetInspectorUrlSuffix\":\"+telnet+\",\n" +
            "        \"inspectorVerboseLog\":false,\n" +
            "        \"passiveBlockDuration\":6000,\n" +
            "        \"mediaType\":\"application/json;charset=utf-8\",\n" +
            "        \"encode\":\"utf-8\",\n" +
            "        \"headers\":{\n" +
            "            \"header1\":\"header1\",\n" +
            "            \"header2\":\"header2\"\n" +
            "        },\n" +
            "        \"recoveryCoefficient\":10,\n" +
            "        \"maxIdleConnections\":16,\n" +
            "        \"maxThreads\":200,\n" +
            "        \"maxThreadsPerHost\":200,\n" +
            "        \"connectTimeout\":3000,\n" +
            "        \"writeTimeout\":10000,\n" +
            "        \"readTimeout\":10000,\n" +
            "        \"maxReadLength\":10485760,\n" +
            "        \"httpCodeNeedBlock\":\"400,500\",\n" +
            "        \"verboseLog\":false,\n" +
            "        \"txTimerEnabled\":false\n" +
            "    }\n" +
            "}";

    @Test
    public void test() throws Exception {
//        HttpClientsDefinitionParser parser = new HttpClientsDefinitionParserImpl();
//        Map<String, HttpClientProperties> propertiesMap = parser.parse(DEFINITION);
//        System.out.println(propertiesMap);
    }

}
