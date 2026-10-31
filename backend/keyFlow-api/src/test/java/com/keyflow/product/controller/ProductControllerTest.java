package com.keyflow.product.controller;

import com.keyflow.common.exception.ConflictException;
import com.keyflow.common.exception.GlobalExceptionHandler;
import com.keyflow.common.exception.ResourceNotFoundException;
import com.keyflow.product.dto.CreateProductRequest;
import com.keyflow.product.dto.UpdateProductRequest;
import com.keyflow.product.entity.Product;
import com.keyflow.product.service.ProductService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ProductController.class)
@Import(GlobalExceptionHandler.class)
class ProductControllerTest {

    @Autowired
    MockMvc mockMvc;

    @MockitoBean
    ProductService productService;

    private final UUID sellerId = UUID.randomUUID();
    private final UUID productId = UUID.randomUUID();

    private static String body(String sellerId, String name) {
        return "{\"sellerId\":" + (sellerId == null ? "null" : "\"" + sellerId + "\"")
                + ",\"name\":\"" + name + "\"}";
    }

    // ---- create ----

    @Test
    void createReturns201WithLocationAndDraftStatus() throws Exception {
        Product product = new Product(sellerId, "Game Key", null);
        when(productService.create(any(CreateProductRequest.class))).thenReturn(product);

        mockMvc.perform(post("/api/v1/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body(sellerId.toString(), "Game Key")))
                .andExpect(status().isCreated())
                .andExpect(header().exists("Location"))
                .andExpect(jsonPath("$.name").value("Game Key"))
                .andExpect(jsonPath("$.status").value("DRAFT"))
                .andExpect(jsonPath("$.sellerId").value(sellerId.toString()));
    }

    @Test
    void blankNameReturns400() throws Exception {
        mockMvc.perform(post("/api/v1/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body(sellerId.toString(), "   ")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"))
                .andExpect(jsonPath("$.fieldErrors[0].field").value("name"));
    }

    @Test
    void missingSellerIdReturns400() throws Exception {
        mockMvc.perform(post("/api/v1/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body(null, "Game Key")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors[0].field").value("sellerId"));
    }

    @Test
    void tooLongNameReturns400() throws Exception {
        mockMvc.perform(post("/api/v1/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body(sellerId.toString(), "x".repeat(201))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"));
    }

    @Test
    void malformedSellerIdReturns400() throws Exception {
        mockMvc.perform(post("/api/v1/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body("not-a-uuid", "Game Key")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("MALFORMED_REQUEST"));
    }

    @Test
    void unknownSellerReturns404() throws Exception {
        when(productService.create(any(CreateProductRequest.class)))
                .thenThrow(new ResourceNotFoundException("Seller not found."));

        mockMvc.perform(post("/api/v1/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body(sellerId.toString(), "Game Key")))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("RESOURCE_NOT_FOUND"));
    }

    // ---- list ----

    @Test
    void listReturnsPagedResponse() throws Exception {
        Product product = new Product(sellerId, "Game Key", null);
        when(productService.list(any(UUID.class), anyInt(), anyInt()))
                .thenReturn(new PageImpl<>(List.of(product), PageRequest.of(0, 20), 1));

        mockMvc.perform(get("/api/v1/products").param("sellerId", sellerId.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].name").value("Game Key"))
                .andExpect(jsonPath("$.page").value(0))
                .andExpect(jsonPath("$.size").value(20))
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.totalPages").value(1));
    }

    @Test
    void listRejectsOversizedPage() throws Exception {
        mockMvc.perform(get("/api/v1/products")
                        .param("sellerId", sellerId.toString())
                        .param("size", "101"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void listRequiresSellerId() throws Exception {
        mockMvc.perform(get("/api/v1/products"))
                .andExpect(status().isBadRequest());
    }

    // ---- get ----

    @Test
    void getReturnsProduct() throws Exception {
        when(productService.get(productId, sellerId)).thenReturn(new Product(sellerId, "Game Key", "desc"));

        mockMvc.perform(get("/api/v1/products/{id}", productId).param("sellerId", sellerId.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Game Key"))
                .andExpect(jsonPath("$.description").value("desc"));
    }

    @Test
    void getUnknownProductReturns404() throws Exception {
        when(productService.get(productId, sellerId))
                .thenThrow(new ResourceNotFoundException("Product not found."));

        mockMvc.perform(get("/api/v1/products/{id}", productId).param("sellerId", sellerId.toString()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("RESOURCE_NOT_FOUND"));
    }

    @Test
    void getWithMalformedIdReturns400() throws Exception {
        mockMvc.perform(get("/api/v1/products/not-a-uuid").param("sellerId", sellerId.toString()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_PARAMETER"));
    }

    // ---- update ----

    @Test
    void updateReturnsUpdatedProduct() throws Exception {
        when(productService.update(any(UUID.class), any(UUID.class), any(UpdateProductRequest.class)))
                .thenReturn(new Product(sellerId, "New Name", null));

        mockMvc.perform(patch("/api/v1/products/{id}", productId)
                        .param("sellerId", sellerId.toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"New Name\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("New Name"));
    }

    @Test
    void updateWithNoFieldsReturns400() throws Exception {
        mockMvc.perform(patch("/api/v1/products/{id}", productId)
                        .param("sellerId", sellerId.toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"));
    }

    @Test
    void updatingArchivedProductReturns409() throws Exception {
        when(productService.update(any(UUID.class), any(UUID.class), any(UpdateProductRequest.class)))
                .thenThrow(new ConflictException("PRODUCT_ARCHIVED", "Archived products cannot be modified."));

        mockMvc.perform(patch("/api/v1/products/{id}", productId)
                        .param("sellerId", sellerId.toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"New\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("PRODUCT_ARCHIVED"));
    }

    // ---- archive ----

    @Test
    void archiveReturnsArchivedProduct() throws Exception {
        Product product = new Product(sellerId, "Game Key", null);
        product.archive();
        when(productService.archive(productId, sellerId)).thenReturn(product);

        mockMvc.perform(post("/api/v1/products/{id}/archive", productId)
                        .param("sellerId", sellerId.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ARCHIVED"));
    }

    @Test
    void archivingTwiceReturns409() throws Exception {
        when(productService.archive(productId, sellerId))
                .thenThrow(new ConflictException("INVALID_STATUS_TRANSITION", "Product is already archived."));

        mockMvc.perform(post("/api/v1/products/{id}/archive", productId)
                        .param("sellerId", sellerId.toString()))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("INVALID_STATUS_TRANSITION"));
    }
}