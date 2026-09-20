package demo.hybris.commerce;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ProductRepository extends JpaRepository<Product, Long> {

    Optional<Product> findByCode(String code);

    @Query("select p from Product p where lower(p.code) like lower(concat('%', :query, '%')) "
            + "or lower(p.name) like lower(concat('%', :query, '%')) "
            + "or lower(p.description) like lower(concat('%', :query, '%'))")
    List<Product> search(@Param("query") String query);
}
