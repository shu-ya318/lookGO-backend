package com.mli.lookgo.core.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.X509Certificate;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManagerBuilder;
import org.apache.hc.client5.http.ssl.DefaultClientTlsStrategy;
import org.apache.hc.client5.http.ssl.TlsSocketStrategy;
import org.apache.hc.core5.http.HttpHost;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;

/**
 * 設定使用第三方服務需要的資訊。
 *
 * @author D5042101
 * @since 2026.06.24
 */
@Configuration
public class RailClientConfig {

    @Value("${rail.proxy.host}")
    private String proxyHost;

    @Value("${rail.proxy.port}")
    private int proxyPort;

    /**
     * 配置並建立一個有客製化 SSL 和 proxy 設定的 {@link RestTemplate} Bean。
     * 
     * @param builder {@link RestTemplateBuilder} (配置和建立{@link RestTemplate} 的建構器。)
     * @return 封裝客製化配置的 {@link RestTemplate} 物件實體。
     */
    @Bean
    RestTemplate railRestTemplate(RestTemplateBuilder builder) {
        TlsSocketStrategy tlsStrategy = new DefaultClientTlsStrategy(generateSslContext(), (hostname, session) -> true);

        CloseableHttpClient httpClient = HttpClients.custom().setProxy(new HttpHost(proxyHost, proxyPort))
                .setConnectionManager(
                        PoolingHttpClientConnectionManagerBuilder.create().setTlsSocketStrategy(tlsStrategy).build())
                .build();

        HttpComponentsClientHttpRequestFactory factory = new HttpComponentsClientHttpRequestFactory(httpClient);

        return builder.requestFactory(() -> factory).build();
    }

    /**
     * 透過產生直接信任全部憑證的安全通訊協定上下文，繞過 SSL 憑證的驗證。需避免在生產環境使用，避免重大的資安風險。
     * 
     * @return SSLContext
     * @throws RuntimeException
     */
    private SSLContext generateSslContext() {
        try {
            TrustManager[] trustAllCertificates = new TrustManager[] { new X509TrustManager() {
                public X509Certificate[] getAcceptedIssuers() {
                    return new X509Certificate[0];
                }

                public void checkClientTrusted(X509Certificate[] certs, String authType) {
                }

                public void checkServerTrusted(X509Certificate[] certs, String authType) {
                }
            } };

            SSLContext sslContext = SSLContext.getInstance("TLS");
            sslContext.init(null, trustAllCertificates, new java.security.SecureRandom());

            return sslContext;
        } catch (NoSuchAlgorithmException | KeyManagementException exception) {
            throw new RuntimeException("SSL 初始化失敗!");
        }
    }
}
