package demo.hybris.commerce;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

@Configuration
public class HttpClientConfig {

    @Bean
    RestClient erpRestClient(RestClient.Builder builder, @Value("${erp.base-url}") String erpBaseUrl) {
        return builder.baseUrl(erpBaseUrl).build();
    }
}
