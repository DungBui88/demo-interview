package com.example.nab.demo.controllers;

import com.example.nab.demo.dtos.VoucherGeneratorCreateVoucherResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import org.apache.commons.lang3.RandomStringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.concurrent.TimeUnit;

/**
 * TODO: This API should be on 3rd party or another service instead.
 * For demo purpose, including this API in this app.
 */
@RestController
@RequestMapping("/generate")
public class VoucherGeneratorController {

    @Value("${client.voucher-generator.delay-response-time:110}")
    private int delayTime;

    @PostMapping("/{voucherType}") // TODO: better name ?
    @Operation(summary = "Generate new voucher by type")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "202", description = "Ok"),
            @ApiResponse(responseCode = "400", description = "Invalid id supplied", content = @Content),
            @ApiResponse(responseCode = "404", description = "Not found", content = @Content) })
    public ResponseEntity<VoucherGeneratorCreateVoucherResponse> sendPhoneMessage(
            @PathVariable("voucherType") String voucherType) {
        VoucherGeneratorCreateVoucherResponse generatedVoucher = new VoucherGeneratorCreateVoucherResponse();
        generatedVoucher.setSeriesNumber(RandomStringUtils.randomAlphanumeric(10));
        generatedVoucher.setExpiredDate(LocalDateTime.now().plusMonths(1));
        generatedVoucher.setType(voucherType);

        try {
            TimeUnit.SECONDS.sleep(delayTime);
        } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
        }

        return ResponseEntity.ok(generatedVoucher);
    }
}
