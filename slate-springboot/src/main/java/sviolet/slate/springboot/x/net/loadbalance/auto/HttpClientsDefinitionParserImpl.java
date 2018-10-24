package sviolet.slate.springboot.x.net.loadbalance.auto;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.util.Map;

/**
 * 将定义文本解析为HttpClients配置
 *
 * @author S.Violet
 */
class HttpClientsDefinitionParserImpl implements HttpClientsDefinitionParser {

    private Gson gson = new GsonBuilder().create();

    /**
     * 将定义文本解析为HttpClients配置
     * @param definition 定义文本
     * @return HttpClients配置
     */
    @Override
    public Map<String, HttpClientProperties> parse(String definition) throws Exception {
//        @SuppressWarnings("unchecked")
//        Map<String, Object> from = gson.fromJson(definition, Map.class);
//        Map<String, HttpClientProperties> to = new HashMap<>(from.size());
//
//        for (Map.Entry<String, Object> entry : from.entrySet()) {
//
//            String hosts = String.valueOf(from.get("hosts"));
//            if (hosts != null) {
//
//            }
//
//        }

        return null;
    }

}
