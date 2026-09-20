package demo.hybris.erp;

import java.math.BigDecimal;
import java.util.Map;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/erp")
public class ErpController {

    private static final Logger log = LoggerFactory.getLogger(ErpController.class);

    private final Map<String, ProductData> products = Map.of(
            "SKU-100", new ProductData(new BigDecimal("129.90"), 42),
            "SKU-200", new ProductData(new BigDecimal("249.00"), 7),
            "SKU-300", new ProductData(new BigDecimal("19.95"), 120)
    );

    @GetMapping("/products/{code}/price")
    public PriceResponse price(@PathVariable String code) {
        return new PriceResponse(code, product(code).price());
    }

    @GetMapping("/products/{code}/stock")
    public StockResponse stock(@PathVariable String code) {
        return new StockResponse(code, product(code).stock());
    }

    @PostMapping("/orders")
    public ErpOrderResponse createOrder(@RequestBody ErpOrderRequest request) {
        String erpOrderId = "ERP-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        log.info("ERP order processed: erpOrderId={}, commerceOrderId={}, product={}, quantity={}",
                erpOrderId, request.commerceOrderId(), request.productCode(), request.quantity());
        return new ErpOrderResponse(erpOrderId, request.commerceOrderId(), "CONFIRMED");
    }

    private ProductData product(String code) {
        ProductData product = products.get(code);
        if (product == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "ERP product not found: " + code);
        }
        return product;
    }

    private record ProductData(BigDecimal price, int stock) {
    }

    public record PriceResponse(String productCode, BigDecimal price) {
    }

    public record StockResponse(String productCode, int availableToPromise) {
    }

    public record ErpOrderRequest(String commerceOrderId, String productCode, int quantity) {
    }

    public record ErpOrderResponse(String erpOrderId, String commerceOrderId, String status) {
    }
}
