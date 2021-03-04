package com.example.nab.demo.controllers;

import com.example.nab.demo.dtos.ResendVoucherRequest;
import com.example.nab.demo.models.Voucher;
import com.example.nab.demo.service.PhoneService;
import com.example.nab.demo.service.VoucherManagementService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.history.Revision;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.ws.rs.BadRequestException;

import java.util.List;
import java.util.stream.Collectors;

import static com.example.nab.demo.utils.StringSupport.isValidPhoneNumber;

@RestController
@RequestMapping("/support")
// TODO: can extend which more support method
public class SupportController {

    @Autowired
    private VoucherManagementService voucherService;
    @Autowired
    private PhoneService phoneService;

    @PostMapping("/voucher/{voucherId}/send-message") // TODO: better name ?
    @Operation(summary = "re-send the voucher to user phone number")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "202", description = "Accepted"),
            @ApiResponse(responseCode = "400", description = "Invalid id supplied", content = @Content),
            @ApiResponse(responseCode = "404", description = "Not found", content = @Content) })
    public ResponseEntity<Void> sendPhoneMessage(
            @PathVariable("voucherId") String voucherId,
            @RequestBody ResendVoucherRequest instruction) {
        String phoneNumber = instruction.getPhoneNumber();
        if (!isValidPhoneNumber(phoneNumber)) {
            throw new BadRequestException("Invalid phone number");
        }

        Voucher voucher = voucherService.getById(voucherId);
        if (voucher == null) {
            throw new BadRequestException("Invalid voucher ID");
        }

        voucher.setUserPhoneNumber(phoneNumber);
        voucherService.updateVoucher(voucher);
        phoneService.sendVoucherMessage(voucher);

        return ResponseEntity.accepted().build();
    }

    @GetMapping("/audit/{voucherId}") // TODO: better name ?
    @Operation(summary = "Get audit detail of Voucher")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Ok"),
            @ApiResponse(responseCode = "404", description = "Not found", content = @Content) })
    public ResponseEntity<List<Voucher>> getVoucherAudit(@PathVariable("voucherId") String voucherId) {
        // TODO: should make proper DTO with included revision number and revision instant
        return ResponseEntity.ok(voucherService.getRevisions(voucherId).stream().map(Revision::getEntity).collect(Collectors.toList()));
    }
}
