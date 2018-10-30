package sviolet.slate.springboot.x.net.loadbalance.auto;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import sviolet.slate.common.x.bean.mbrproc.EnableMemberProcessor;
import sviolet.slate.springboot.auto.SlateProperties;

/**
 * <p>HttpClients配置: 自动配置SimpleOkHttpClient</p>
 * <p>配置前缀: slate.httpclients</p>
 *
 * @author S.Violet
 */
@Configuration
@EnableMemberProcessor(HttpClientMemberProcessor.class)//开启@HttpClient注解注入
public class HttpClientsConfig {

    public static final String HTTP_CLIENTS_NAME = "slate.springboot.httpClients";

    /**
     * <p>自动配置HttpClients</p>
     * <p>我们可以用如下方式获得所有客户端(包括运行时动态添加的):</p>
     *
     * <pre>
     *     private SimpleOkHttpClient client;
     *     <code>@Autowired</code>
     *     public Constructor(HttpClients httpClients) {
     *         this.client = httpClients.get("tagname");
     *     };
     * </pre>
     */
    @Bean(HTTP_CLIENTS_NAME)
    public HttpClients httpClientsContainer(SlateProperties slateProperties){
        return new HttpClientsImpl(slateProperties.getHttpclients());
    }

}
