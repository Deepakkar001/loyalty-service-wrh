package com.loyaltyos.merchants.controller;

import com.loyaltyos.merchants.dto.MerchantInviteAcceptRequest;
import com.loyaltyos.merchants.dto.MerchantInviteAcceptResponse;
import com.loyaltyos.merchants.dto.MerchantInviteValidateResponse;
import com.loyaltyos.merchants.service.MerchantInviteService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.Objects;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/merchant/invite")
@Tag(name = "Merchant Invite", description = "Public merchant portal invite acceptance")
@ConditionalOnProperty(name = "loyaltyos.merchant.enabled", havingValue = "true", matchIfMissing = true)
public class MerchantInviteController {

    private final MerchantInviteService inviteService;

    public MerchantInviteController(MerchantInviteService inviteService) {
        this.inviteService = Objects.requireNonNull(inviteService, "inviteService");
    }

    @GetMapping("/validate")
    public ResponseEntity<MerchantInviteValidateResponse> validate(@RequestParam String token) {
        return ResponseEntity.ok(inviteService.validateToken(token));
    }

    @PostMapping("/accept")
    public ResponseEntity<MerchantInviteAcceptResponse> accept(
        @Valid @RequestBody MerchantInviteAcceptRequest request
    ) {
        return ResponseEntity.ok(inviteService.acceptInvite(request.getToken(), request.getNewPassword()));
    }
}
