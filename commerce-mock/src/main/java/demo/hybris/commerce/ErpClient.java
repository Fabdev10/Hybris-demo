package demo.hybris.commerce;

import java.math.BigDecimal;

import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

@Service
public class ErpClient {

    private final RestClient restClient;

    public ErpClient(RestClient erpRestClient) {
        this.restClient = erpRestClient;
    }

    public BigDecimal getPrice(String productCode) {
        ErpPrice response = restClient.get()
                .uri("/erp/products/{code}/price", productCode)
                .retrieve()
                .body(ErpPrice.class);
        return response.price();
    }

    public int getStock(String productCode) {
        ErpStock response = restClient.get()
                .uri("/erp/products/{code}/stock", productCode)
                .retrieve()
                .body(ErpStock.class);
        return response.availableToPromise();
    }

    public ErpOrder createOrder(Long commerceOrderId, String productCode, int quantity) {
        return restClient.post()
                .uri("/erp/orders")
                .body(new ErpOrderRequest(commerceOrderId.toString(), productCode, quantity))
                .retrieve()
                .body(ErpOrder.class);
    }

    private record ErpPrice(String productCode, BigDecimal price) { }
    private record ErpStock(String productCode, int availableToPromise) { }
    private record ErpOrderRequest(String commerceOrderId, String productCode, int quantity) { }
    public record ErpOrder(String erpOrderId, String commerceOrderId, String status) { }
}
