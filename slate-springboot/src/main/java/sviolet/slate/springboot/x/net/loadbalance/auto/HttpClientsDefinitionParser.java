package sviolet.slate.springboot.x.net.loadbalance.auto;

import java.util.Map;

/**
 * 将定义文本解析为HttpClients配置
 *
 * @author S.Violet
 */
public interface HttpClientsDefinitionParser {

    /**
     * 将定义文本解析为HttpClients配置
     * @param definition 定义文本
     * @return HttpClients配置
     * @throws Exception 解析错误
     */
    Map<String, HttpClientProperties> parse(String definition) throws Exception;

}
