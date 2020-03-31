/*
 * Copyright (C) 2015-2020 S.Violet
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * Project GitHub: https://github.com/shepherdviolet/slate
 * Email: shepherdviolet@163.com
 */

package sviolet.slate.common.x.net.loadbalance.classic;

import okhttp3.internal.Util;
import okhttp3.internal.platform.Platform;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.X509TrustManager;
import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.Arrays;

/**
 * MultiHostOkHttpClient专用工具: 自定义SSL方式
 *
 * @author S.Violet
 */
public class SslUtils {

    /**
     * 给MultiHostOkHttpClient设置自定义的X509TrustManager, 用于实现自定义的SSL验证逻辑
     * @param multiHostOkHttpClient multiHostOkHttpClient
     * @param trustManager trustManager
     */
    public static void setX509TrustManager(MultiHostOkHttpClient multiHostOkHttpClient, X509TrustManager trustManager) {
        if (multiHostOkHttpClient == null) {
            return;
        }
        try {
            SSLContext sslContext = Platform.get().getSSLContext();
            sslContext.init(null, new TrustManager[]{trustManager}, null);
            multiHostOkHttpClient.setSSLSocketFactory(sslContext.getSocketFactory());
            multiHostOkHttpClient.setX509TrustManager(trustManager);
        } catch (KeyManagementException e) {
            throw new RuntimeException("Error while initializing SSLSocketFactory and setting X509TrustManager to MultiHostOkHttpClient", e);
        }
    }

    /**
     * 给MultiHostOkHttpClient添加自定义的根证书, 用于验证自签名的服务器.
     * 如果我们访问的服务端的证书是自己签发的, 根证书不合法, 可以用这个方法, 添加服务端的根证书为受信任的证书.
     *
     * @param multiHostOkHttpClient multiHostOkHttpClient
     * @param customIssuers 自定义的根证书
     */
    public static void setCustomIssuers(MultiHostOkHttpClient multiHostOkHttpClient, X509Certificate[] customIssuers) throws CertificateException {
        if (customIssuers == null) {
            return;
        }
        setX509TrustManager(multiHostOkHttpClient, CustomIssuersX509TrustManager.newInstance(customIssuers));
    }

    /**
     * 能够添加自定义根证书的X509TrustManager
     */
    public static class CustomIssuersX509TrustManager implements X509TrustManager {

        private final X509TrustManager systemTrustManager;
        private final X509TrustManager customTrustManager;
        private final X509Certificate[] acceptedIssuers;

        /**
         * 能够添加自定义根证书的X509TrustManager
         *
         * @param customIssuers 自定义根证书
         */
        public static X509TrustManager newInstance(X509Certificate[] customIssuers) throws CertificateException {
            if (customIssuers == null) {
                throw new IllegalArgumentException("customIssuers is null");
            }

            // X509Certificates to KeyStore
            KeyStore keyStore;
            try {
                keyStore = KeyStore.getInstance(KeyStore.getDefaultType());
                keyStore.load(null, null);
                int i = 0;
                for (Certificate certificate : customIssuers) {
                    keyStore.setCertificateEntry(String.valueOf(i++), certificate);
                }
            } catch (Exception e) {
                throw new CertificateException("Error while converting X509Certificates to KeyStore", e);
            }

            return new CustomIssuersX509TrustManager(keyStore);
        }

        /**
         * 能够添加自定义根证书的X509TrustManager
         *
         * @param customKeyStore 自定义根证书的KeyStore
         */
        public static X509TrustManager newInstance(KeyStore customKeyStore) throws CertificateException {
            return new CustomIssuersX509TrustManager(customKeyStore);
        }

        private CustomIssuersX509TrustManager(KeyStore customKeyStore) throws CertificateException {
            if (customKeyStore == null) {
                throw new IllegalArgumentException("customKeyStore is null");
            }

            // Get system TrustManager
            systemTrustManager = Util.platformTrustManager();

            // Build TrustManager by KeyStore
            try {
                TrustManagerFactory trustManagerFactory = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
                trustManagerFactory.init(customKeyStore);
                TrustManager[] trustManagers = trustManagerFactory.getTrustManagers();
                if (trustManagers.length != 1 || !(trustManagers[0] instanceof X509TrustManager)) {
                    throw new IllegalStateException("Unexpected default trust managers:" + Arrays.toString(trustManagers));
                }
                customTrustManager = (X509TrustManager) trustManagers[0];
            } catch (Exception e) {
                throw new CertificateException("Error while building TrustManager by X509Certificates (KeyStore)", e);
            }

            X509Certificate[] systemIssuers = systemTrustManager.getAcceptedIssuers();
            X509Certificate[] customIssuers = customTrustManager.getAcceptedIssuers();
            acceptedIssuers = new X509Certificate[systemIssuers.length + customIssuers.length];
            System.arraycopy(customIssuers, 0, acceptedIssuers, 0, customIssuers.length);
            System.arraycopy(systemIssuers, 0, acceptedIssuers, customIssuers.length, systemIssuers.length);
        }

        @Override
        public void checkClientTrusted(X509Certificate[] chain, String authType) throws CertificateException {
            // Custom first
            CertificateException throwable;
            try {
                customTrustManager.checkClientTrusted(chain, authType);
                // Verified
                return;
            } catch (CertificateException t) {
                throwable = t;
            }
            // System
            try {
                systemTrustManager.checkClientTrusted(chain, authType);
            } catch (CertificateException t) {
                throwable.addSuppressed(t);
                throw throwable;
            }
        }

        @Override
        public void checkServerTrusted(X509Certificate[] chain, String authType) throws CertificateException {
            // Custom first
            CertificateException throwable;
            try {
                customTrustManager.checkServerTrusted(chain, authType);
                // Verified
                return;
            } catch (CertificateException t) {
                throwable = t;
            }
            // System
            try {
                systemTrustManager.checkServerTrusted(chain, authType);
            } catch (CertificateException t) {
                throwable.addSuppressed(t);
                throw throwable;
            }
        }

        @Override
        public X509Certificate[] getAcceptedIssuers() {
            return acceptedIssuers;
        }

    }


}
