package com.example.ecommerce.backend.inventory.controller;

import com.example.ecommerce.backend.common.constants.ApiEndpoints;
import com.example.ecommerce.backend.common.dto.response.ApiResponse;
import com.example.ecommerce.backend.inventory.dto.request.InventoryCreateRequest;
import com.example.ecommerce.backend.inventory.dto.request.InventoryQuantityRequest;
import com.example.ecommerce.backend.inventory.dto.response.InventoryResponse;
import com.example.ecommerce.backend.inventory.service.InventoryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST controller for managing product inventory.
 *
 * <p>Exposes versioned inventory endpoints under
 * {@link ApiEndpoints.Inventory#BASE_INVENTORY} and wraps successful responses
 * with the common {@link ApiResponse} structure used by the API.</p>
 *
 * @author Pial Kanti Samadder
 */
@RestController
@RequestMapping(ApiEndpoints.Inventory.BASE_INVENTORY)
@RequiredArgsConstructor
@Tag(
        name = "Inventory",
        description = "Operations for managing product inventory quantities and reservations"
)
public class InventoryController {
    private final InventoryService inventoryService;

    /**
     * Creates inventory for an existing product.
     *
     * @param request validated inventory creation payload
     * @return response containing the created inventory
     * @throws com.example.ecommerce.backend.common.exception.ResourceConflictException when inventory already exists
     * @throws jakarta.persistence.EntityNotFoundException when the product does not exist
     */
    @Operation(
            summary = "Create product inventory",
            description = "Creates inventory for an existing product. Each product can have one inventory record.",
            requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    required = true,
                    description = "Inventory creation payload containing product identifier and starting total quantity.",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = InventoryCreateRequest.class),
                            examples = @ExampleObject(
                                    name = "Create inventory",
                                    value = """
                                            {
                                              "productId": 1,
                                              "totalQuantity": 100
                                            }
                                            """
                            )
                    )
            ),
            responses = {
                    @io.swagger.v3.oas.annotations.responses.ApiResponse(
                            responseCode = "200",
                            description = "Inventory created successfully",
                            content = @Content(
                                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                                    schema = @Schema(implementation = InventoryResponse.class)
                            )
                    ),
                    @io.swagger.v3.oas.annotations.responses.ApiResponse(
                            responseCode = "400",
                            description = "Invalid inventory creation payload",
                            content = @Content
                    ),
                    @io.swagger.v3.oas.annotations.responses.ApiResponse(
                            responseCode = "404",
                            description = "No product exists for the supplied identifier",
                            content = @Content
                    ),
                    @io.swagger.v3.oas.annotations.responses.ApiResponse(
                            responseCode = "409",
                            description = "Inventory already exists for the product",
                            content = @Content
                    )
            }
    )
    @PostMapping
    public ResponseEntity<ApiResponse<InventoryResponse>> createInventory(
            @Valid @RequestBody InventoryCreateRequest request) {
        return ResponseEntity.ok(ApiResponse.success(inventoryService.create(request)));
    }

    /**
     * Retrieves inventory by product identifier.
     *
     * @param productId product identifier
     * @return response containing the matching inventory
     * @throws jakarta.persistence.EntityNotFoundException when no inventory exists for the product
     */
    @Operation(
            summary = "Get inventory by product ID",
            description = "Retrieves the inventory record that belongs to the supplied product identifier.",
            responses = {
                    @io.swagger.v3.oas.annotations.responses.ApiResponse(
                            responseCode = "200",
                            description = "Inventory retrieved successfully",
                            content = @Content(
                                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                                    schema = @Schema(implementation = InventoryResponse.class)
                            )
                    ),
                    @io.swagger.v3.oas.annotations.responses.ApiResponse(
                            responseCode = "404",
                            description = "No inventory exists for the supplied product identifier",
                            content = @Content
                    )
            }
    )
    @GetMapping("/{productId}")
    public ResponseEntity<ApiResponse<InventoryResponse>> getInventoryByProductId(
            @Parameter(description = "Unique identifier of the product.", example = "1", required = true)
            @PathVariable Long productId) {
        return ResponseEntity.ok(ApiResponse.success(inventoryService.getByProductId(productId)));
    }

    /**
     * Increases total inventory quantity.
     *
     * @param productId product identifier
     * @param request validated quantity adjustment payload
     * @return response containing the updated inventory
     */
    @Operation(
            summary = "Increase inventory quantity",
            description = "Adds the supplied quantity to total inventory quantity.",
            requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    required = true,
                    description = "Positive quantity to add to total inventory.",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = InventoryQuantityRequest.class),
                            examples = @ExampleObject(
                                    name = "Increase inventory",
                                    value = """
                                            {
                                              "quantity": 10
                                            }
                                            """
                            )
                    )
            ),
            responses = {
                    @io.swagger.v3.oas.annotations.responses.ApiResponse(
                            responseCode = "200",
                            description = "Inventory quantity increased successfully",
                            content = @Content(
                                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                                    schema = @Schema(implementation = InventoryResponse.class)
                            )
                    ),
                    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid quantity payload", content = @Content),
                    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "No inventory exists for the product", content = @Content),
                    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "Concurrent inventory update conflict", content = @Content)
            }
    )
    @PostMapping("/{productId}/increase")
    public ResponseEntity<ApiResponse<InventoryResponse>> increaseInventory(
            @Parameter(description = "Unique identifier of the product.", example = "1", required = true)
            @PathVariable Long productId,
            @Valid @RequestBody InventoryQuantityRequest request) {
        return ResponseEntity.ok(ApiResponse.success(inventoryService.increase(productId, request)));
    }

    /**
     * Decreases total inventory quantity.
     *
     * @param productId product identifier
     * @param request validated quantity adjustment payload
     * @return response containing the updated inventory
     */
    @Operation(
            summary = "Decrease inventory quantity",
            description = "Subtracts the supplied quantity from total inventory without dropping below reserved quantity.",
            requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    required = true,
                    description = "Positive quantity to subtract from total inventory.",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = InventoryQuantityRequest.class),
                            examples = @ExampleObject(
                                    name = "Decrease inventory",
                                    value = """
                                            {
                                              "quantity": 5
                                            }
                                            """
                            )
                    )
            ),
            responses = {
                    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Inventory quantity decreased successfully", content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = InventoryResponse.class))),
                    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid quantity payload", content = @Content),
                    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "No inventory exists for the product", content = @Content),
                    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "Decrease would violate reserved stock or a concurrent update occurred", content = @Content)
            }
    )
    @PostMapping("/{productId}/decrease")
    public ResponseEntity<ApiResponse<InventoryResponse>> decreaseInventory(
            @Parameter(description = "Unique identifier of the product.", example = "1", required = true)
            @PathVariable Long productId,
            @Valid @RequestBody InventoryQuantityRequest request) {
        return ResponseEntity.ok(ApiResponse.success(inventoryService.decrease(productId, request)));
    }

    /**
     * Reserves available inventory quantity.
     *
     * @param productId product identifier
     * @param request validated quantity reservation payload
     * @return response containing the updated inventory
     */
    @Operation(
            summary = "Reserve inventory quantity",
            description = "Moves available inventory quantity into reserved inventory.",
            requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    required = true,
                    description = "Positive quantity to reserve from available inventory.",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = InventoryQuantityRequest.class),
                            examples = @ExampleObject(
                                    name = "Reserve inventory",
                                    value = """
                                            {
                                              "quantity": 2
                                            }
                                            """
                            )
                    )
            ),
            responses = {
                    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Inventory reserved successfully", content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = InventoryResponse.class))),
                    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid quantity payload", content = @Content),
                    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "No inventory exists for the product", content = @Content),
                    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "Insufficient available stock or a concurrent update occurred", content = @Content)
            }
    )
    @PostMapping("/{productId}/reserve")
    public ResponseEntity<ApiResponse<InventoryResponse>> reserveInventory(
            @Parameter(description = "Unique identifier of the product.", example = "1", required = true)
            @PathVariable Long productId,
            @Valid @RequestBody InventoryQuantityRequest request) {
        return ResponseEntity.ok(ApiResponse.success(inventoryService.reserve(productId, request)));
    }

    /**
     * Releases reserved inventory quantity.
     *
     * @param productId product identifier
     * @param request validated quantity release payload
     * @return response containing the updated inventory
     */
    @Operation(
            summary = "Release reserved inventory quantity",
            description = "Moves reserved inventory quantity back into available inventory.",
            requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    required = true,
                    description = "Positive quantity to release from reserved inventory.",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = InventoryQuantityRequest.class),
                            examples = @ExampleObject(
                                    name = "Release inventory",
                                    value = """
                                            {
                                              "quantity": 1
                                            }
                                            """
                            )
                    )
            ),
            responses = {
                    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Reserved inventory released successfully", content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = InventoryResponse.class))),
                    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid quantity payload", content = @Content),
                    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "No inventory exists for the product", content = @Content),
                    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "Release would exceed reserved stock or a concurrent update occurred", content = @Content)
            }
    )
    @PostMapping("/{productId}/release")
    public ResponseEntity<ApiResponse<InventoryResponse>> releaseInventory(
            @Parameter(description = "Unique identifier of the product.", example = "1", required = true)
            @PathVariable Long productId,
            @Valid @RequestBody InventoryQuantityRequest request) {
        return ResponseEntity.ok(ApiResponse.success(inventoryService.release(productId, request)));
    }
}
