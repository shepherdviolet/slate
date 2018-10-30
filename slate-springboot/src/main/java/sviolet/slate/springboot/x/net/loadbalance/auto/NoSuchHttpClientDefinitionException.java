package sviolet.slate.springboot.x.net.loadbalance.auto;

/**
 * 请求客户端未定义异常
 */
public class NoSuchHttpClientDefinitionException extends RuntimeException {

    public NoSuchHttpClientDefinitionException(String message) {
        super(message);
    }

    public NoSuchHttpClientDefinitionException(String message, Throwable cause) {
        super(message, cause);
    }

}
