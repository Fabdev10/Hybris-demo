package demo.hybris.commerce;

import java.math.BigDecimal;
import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/occ/v2")
public class CommerceController {

    private final ProductRepository productRepository;
    private final OrderRepository orderRepository;
    private final ErpClient erpClient;

    public CommerceController(ProductRepository productRepository, OrderRepository orderRepository,
                              ErpClient erpClient) {
        this.productRepository = productRepository;
        this.orderRepository = orderRepository;
        this.erpClient = erpClient;
    }

    @GetMapping("/products/search")
    public List<ProductView> searchProducts(@RequestParam String query) {
        return productRepository.search(query).stream()
                .map(product -> new ProductView(product, erpClient.getPrice(product.getCode()),
                        erpClient.getStock(product.getCode())))
                .toList();
    }

    @PostMapping("/orders")
    public OrderView createOrder(@RequestBody OrderRequest request) {
        if (request.productCode() == null || request.productCode().isBlank() || request.quantity() <= 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "productCode and positive quantity are required");
        }
        Order order = orderRepository.save(new Order(request.productCode(), request.quantity()));
        ErpClient.ErpOrder erpOrder = erpClient.createOrder(order.getId(), order.getProductCode(), order.getQuantity());
        order.confirm(erpOrder.status(), erpOrder.erpOrderId());
        return toView(orderRepository.save(order));
    }

    @GetMapping("/orders/{id}")
    public OrderView getOrder(@PathVariable Long id) {
        return orderRepository.findById(id)
                .map(this::toView)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Order not found: " + id));
    }

    private OrderView toView(Order order) {
        return new OrderView(order.getId(), order.getProductCode(), order.getQuantity(), order.getStatus(),
                order.getErpOrderId(), order.getCreatedAt());
    }

    public record ProductView(String code, String name, String description, String category,
                              BigDecimal price, int availableToPromise) {
        ProductView(Product product, BigDecimal price, int availableToPromise) {
            this(product.getCode(), product.getName(), product.getDescription(), product.getCategory(),
                    price, availableToPromise);
        }
    }

    public record OrderRequest(String productCode, int quantity) { }

    public record OrderView(Long id, String productCode, int quantity, String status,
                            String erpOrderId, java.time.Instant createdAt) { }
}
