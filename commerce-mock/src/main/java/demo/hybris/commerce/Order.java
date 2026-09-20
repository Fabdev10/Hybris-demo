package demo.hybris.commerce;

import java.time.Instant;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "commerce_orders")
public class Order {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String productCode;
    private int quantity;
    private String status;
    private String erpOrderId;
    private Instant createdAt;

    protected Order() {
    }

    public Order(String productCode, int quantity) {
        this.productCode = productCode;
        this.quantity = quantity;
        this.status = "CREATED";
        this.createdAt = Instant.now();
    }

    public Long getId() { return id; }
    public String getProductCode() { return productCode; }
    public int getQuantity() { return quantity; }
    public String getStatus() { return status; }
    public String getErpOrderId() { return erpOrderId; }
    public Instant getCreatedAt() { return createdAt; }

    public void confirm(String status, String erpOrderId) {
        this.status = status;
        this.erpOrderId = erpOrderId;
    }
}
