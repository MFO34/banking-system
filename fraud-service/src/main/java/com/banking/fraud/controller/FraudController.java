package com.banking.fraud.controller;

import com.banking.fraud.dto.FraudCheckRequest;
import com.banking.fraud.dto.FraudCheckResponse;
import com.banking.fraud.service.FraudService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/fraud")
@RequiredArgsConstructor
public class FraudController {

    private final FraudService fraudService;

    @PostMapping("/check")
    public FraudCheckResponse check(@RequestBody FraudCheckRequest request) {
        return fraudService.check(request);
    }
}
