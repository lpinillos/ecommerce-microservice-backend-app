package com.selimhorri.app.integration;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.web.server.LocalServerPort;
import org.springframework.http.*;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.util.StreamUtils;
import org.springframework.web.client.DefaultResponseErrorHandler;

import com.selimhorri.app.dto.ProductDto;
import com.selimhorri.app.dto.CategoryDto;
import com.selimhorri.app.dto.response.collection.DtoCollectionResponse;

import java.io.IOException;
import java.nio.charset.Charset;
import java.util.Objects;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {"spring.profiles.active=test",
                "spring.datasource.url=jdbc:h2:mem:testdb",
                "spring.datasource.driver-class-name=org.h2.Driver"})
public class ProductControllerTest {

    @LocalServerPort
    private int port;

    @Autowired
    private TestRestTemplate restTemplate;

    private String baseUrl;

    @BeforeEach
    public void setUp() {
        baseUrl = String.format("http://localhost:%d/product-service/api/products", port);

        restTemplate.getRestTemplate().setErrorHandler(new DefaultResponseErrorHandler() {
            @Override
            public void handleError(ClientHttpResponse response) throws IOException {
                String responseBody = StreamUtils.copyToString(response.getBody(), Charset.defaultCharset());
                System.err.println("Response error: " + response.getStatusCode());
                System.err.println("Response body: " + responseBody);
                super.handleError(response);
            }
        });
    }

    @Test
    public void testFindAllProducts() {
        ResponseEntity<DtoCollectionResponse> response = restTemplate.exchange(
                baseUrl,
                HttpMethod.GET,
                null,
                DtoCollectionResponse.class
        );

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertNotNull(response.getBody().getCollection());
    }

    @Test
    public void testCreateProduct() {
        ProductDto productDto = createSampleProduct();
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<ProductDto> request = new HttpEntity<>(productDto, headers);

        ResponseEntity<ProductDto> response = restTemplate.exchange(
                baseUrl,
                HttpMethod.POST,
                request,
                ProductDto.class
        );

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(productDto.getProductTitle(), response.getBody().getProductTitle());
        assertEquals(productDto.getSku(), response.getBody().getSku());
        assertEquals(productDto.getPriceUnit(), response.getBody().getPriceUnit());
    }

    @Test
    public void testGetProductById() {
        Integer productId = 1;
        ResponseEntity<ProductDto> response = restTemplate.exchange(
                baseUrl + "/" + productId,
                HttpMethod.GET,
                null,
                ProductDto.class
        );

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(productId, response.getBody().getProductId());
    }

    @Test
    public void testDeleteProduct() {
        // Primero crear un producto para poder eliminarlo
        ProductDto newProduct = createSampleProduct();
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<ProductDto> createRequest = new HttpEntity<>(newProduct, headers);

        ResponseEntity<ProductDto> createResponse = restTemplate.exchange(
                baseUrl,
                HttpMethod.POST,
                createRequest,
                ProductDto.class
        );

        assertEquals(HttpStatus.OK, createResponse.getStatusCode());
        assertNotNull(createResponse.getBody());
        Integer createdProductId = createResponse.getBody().getProductId();

        // Ahora eliminar el producto creado
        ResponseEntity<Boolean> deleteResponse = restTemplate.exchange(
                baseUrl + "/" + createdProductId,
                HttpMethod.DELETE,
                null,
                Boolean.class
        );

        assertEquals(HttpStatus.OK, deleteResponse.getStatusCode());
        assertTrue(Objects.requireNonNull(deleteResponse.getBody()));
    }

    private ProductDto createSampleProduct() {
        CategoryDto categoryDto = CategoryDto.builder()
                .categoryId(1)
                .categoryTitle("Electronics")
                .imageUrl("http://example.com/categories/electronics.jpg")
                .build();

        return ProductDto.builder()
                .productTitle("Test Product")
                .imageUrl("http://example.com/products/test-product.jpg")
                .sku("PRD-TEST-001")
                .priceUnit(299.99)
                .quantity(10)
                .categoryDto(categoryDto)
                .build();
    }
}