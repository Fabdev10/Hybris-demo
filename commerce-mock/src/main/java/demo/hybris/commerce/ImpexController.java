package demo.hybris.commerce;

import java.nio.charset.StandardCharsets;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/impex")
public class ImpexController {

    private final ProductRepository productRepository;

    public ImpexController(ProductRepository productRepository) {
        this.productRepository = productRepository;
    }

    @PostMapping(value = "/import", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ImportResponse importProducts(@RequestPart("file") MultipartFile file) {
        try {
            int imported = 0;
            for (String line : new String(file.getBytes(), StandardCharsets.UTF_8).lines().toList()) {
                if (line.isBlank() || line.startsWith("code,")) {
                    continue;
                }
                String[] columns = line.split(",", -1);
                if (columns.length != 4) {
                    throw new IllegalArgumentException("Expected 4 CSV columns: " + line);
                }
                Product product = productRepository.findByCode(columns[0])
                        .orElseGet(() -> new Product(columns[0], columns[1], columns[2], columns[3]));
                product.update(columns[1], columns[2], columns[3]);
                productRepository.save(product);
                imported++;
            }
            return new ImportResponse(imported);
        } catch (Exception exception) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid product CSV", exception);
        }
    }

    public record ImportResponse(int importedProducts) { }
}