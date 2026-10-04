package com.rbctcsworld.ecommerce.returns;

import com.rbctcsworld.ecommerce.returns.ReturnDtos.CreateReturnRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * POST /api/orders/{id}/return {reason, comment?}  201 | 400 bad reason / window closed | 404 | 409
 * GET  /api/orders/{id}/return                     200 | 404
 */
@RestController
@RequestMapping("/api/orders/{id}/return")
public class ReturnController {

    private final ReturnService service;

    public ReturnController(ReturnService service) {
        this.service = service;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ReturnRequest request(Authentication auth, @PathVariable Long id, @Valid @RequestBody CreateReturnRequest r) {
        return service.request(auth.getName(), id, r.reason(), r.comment());
    }

    @GetMapping
    public ReturnRequest get(Authentication auth, @PathVariable Long id) {
        return service.forOrder(auth.getName(), id);
    }
}
