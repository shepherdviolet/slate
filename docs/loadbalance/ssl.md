# HttpClient SSL相关文档

## 访问自签名的服务端 (根证书非官方)

* 如果我们要访问一个服务器, 它的证书不是正规CA签发的, 我们可以向对方获取根证书, 然后用如下方法添加信任的根证书

```text
    // 这里硬编码为例, 实际请按照文档配置为Spring Bean
    SimpleOkHttpClient simpleOkHttpClient = (SimpleOkHttpClient) new SimpleOkHttpClient()
            .setHosts("https://127.0.0.1/");
    
    // 信任CERT1和CERT2两张根证书 (我们自己签发的根证书)
    SslUtils.setCustomIssuers(simpleOkHttpClient, new X509Certificate[]{
            CertificateUtils.parseX509ToCertificate(Base64Utils.decode(CERT1)),
            CertificateUtils.parseX509ToCertificate(Base64Utils.decode(CERT2))
    });
```

## 跳过域名验证

* 如果我们通过代理访问一个服务器, 例如Nginx的Stream代理, 我们访问的地址肯定和证书的CN不符, 有两种办法

```text
    // 方法1
    可以设置服务器的hosts, 把证书CN声明的域名映射为代理服务器IP
```

```text
    // 方法2
    // 这里硬编码为例, 实际请按照文档配置为Spring Bean
    SimpleOkHttpClient simpleOkHttpClient = (SimpleOkHttpClient) new SimpleOkHttpClient()
            .setHosts("https://127.0.0.1/")//代理服务器地址
            .setHostnameVerifier(new SimpleHostnameVerifier() {
                @Override
                protected boolean isHostnameMatch(String hostname, String cn) {
                    //写死证书CN声明的域名
                    return super.isHostnameMatch("www.baidu.com", cn);
                }
            });
```

* 如果我们希望限定服务端的DN为指定值, 即不信任其他任何DN的话, 可以这样做

```text
    // 这里硬编码为例, 实际请按照文档配置为Spring Bean
    SimpleOkHttpClient simpleOkHttpClient = (SimpleOkHttpClient) new SimpleOkHttpClient()
            .setHosts("https://127.0.0.1/")
            .setHostnameVerifier(new HostnameVerifier() {
                public boolean verify(String hostname, SSLSession session) {
                    try {
                        Certificate[] certificates = session.getPeerCertificates();
                        if (certificates == null || certificates.length <= 0) {
                            return false;
                        }
                        //第一个证书是站点证书
                        X509Certificate x509Certificate = (X509Certificate) certificates[0];
                        String dn = x509Certificate.getSubjectX500Principal().getName();
                        return "你指定的DN".equals(dn);
                    } catch (Throwable ignored) {
                    }
                    return false;
                }
            });
```

## 自定义SSL验证逻辑

```text
//1.可以自定义x509TrustManager
SslUtils.setX509TrustManager(simpleOkHttpClient, x509TrustManager);
//2.可以自定义SSLSocketFactory (X509TrustManager需要同事设置)
simpleOkHttpClient.setSSLSocketFactory(sslSocketFactory);
simpleOkHttpClient.setX509TrustManager(x509TrustManager)
```
