package sviolet.slate.springboot.x.net.loadbalance.auto;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.Map;

/**
 * 将定义文本解析为HttpClients配置
 *
 * @author S.Violet
 */
public class HttpClientsDefinitionParserImpl implements HttpClientsDefinitionParser {

    private static final Type TYPE = new TypeToken<Map<String, HttpClientProperties>>(){}.getType();

    private Gson gson = new GsonBuilder().create();

    /**
     * 将定义文本解析为HttpClients配置
     * @param definition 定义文本
     * @return HttpClients配置
     */
    @Override
    public Map<String, HttpClientProperties> parse(String definition) throws Exception {
        return gson.fromJson(definition, TYPE);
    }

}
