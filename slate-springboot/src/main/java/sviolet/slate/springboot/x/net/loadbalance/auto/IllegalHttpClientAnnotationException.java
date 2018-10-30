package sviolet.slate.springboot.x.net.loadbalance.auto;

/**
 * <code>@HttpClient注解用法错误异常</code>
 */
public class IllegalHttpClientAnnotationException extends RuntimeException {

    public IllegalHttpClientAnnotationException(String message) {
        super(message);
    }

    public IllegalHttpClientAnnotationException(String message, Throwable cause) {
        super(message, cause);
    }

}
