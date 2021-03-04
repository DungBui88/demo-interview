package com.example.nab.demo.controllers;

import com.example.nab.demo.dtos.CreateVoucherResponse;
import com.example.nab.demo.dtos.UpdateVoucherInstruction;
import com.example.nab.demo.dtos.VoucherGeneratingInstruction;
import com.example.nab.demo.models.Voucher;
import com.example.nab.demo.models.VoucherStatus;
import com.example.nab.demo.service.PhoneService;
import com.example.nab.demo.service.VoucherManagementService;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.hateoas.server.mvc.WebMvcLinkBuilder;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import io.swagger.v3.oas.annotations.Operation;

import javax.ws.rs.BadRequestException;
import java.time.LocalDateTime;
import java.util.Collection;

import static com.example.nab.demo.utils.StringSupport.isValidPhoneNumber;

@RestController
@RequestMapping("/vouchers")
public class VoucherController {

    @Autowired
    private VoucherManagementService voucherService;
    @Autowired
    private PhoneService phoneService;

    @GetMapping
    @Operation(summary = "Get vouchers")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Ok"),
            @ApiResponse(responseCode = "400", description = "Invalid id supplied", content = @Content),
            @ApiResponse(responseCode = "404", description = "Not found", content = @Content) })
    public ResponseEntity<Collection<Voucher>> getVouchers(
            @RequestParam(value = "phoneNumber", required = false) String phoneNumber,
            @RequestParam(value = "paymentTransactionId", required = false) String paymentTransactionId) {
        if (phoneNumber != null && !isValidPhoneNumber(phoneNumber)) {
            throw new BadRequestException("Invalid phone number"); // should not go with detail error message as sensitive data
        }

        /*
            the API expected should only phoneNumber or paymentTransactionId input
         */
        if (paymentTransactionId != null) {
            // this should be common case
            return ResponseEntity.ok(voucherService.getByPaymentTransactionId(paymentTransactionId));
        }

        if (phoneNumber != null) {
            return ResponseEntity.ok(voucherService.getByPhoneNumber(phoneNumber));
        }

        // assume this case should never happen, if we really need to used this, we should paging
        return ResponseEntity.ok(voucherService.getAll());
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get voucher by Id")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Ok"),
            @ApiResponse(responseCode = "400", description = "Invalid id supplied", content = @Content),
            @ApiResponse(responseCode = "404", description = "Not found", content = @Content) })
    public ResponseEntity<Voucher> getVoucherById(@PathVariable("id") String id) {
        Voucher voucher = voucherService.getById(id);
        if (voucher == null) {
            return ResponseEntity.badRequest().build();
        }
        return ResponseEntity.ok(voucher);
    }

    /**
     * When we receive create voucher request, we will create temporary voucher with ID for tracking and return in
     * the response.
     * The {@link VoucherManagementService#generateSerialNumber(Voucher)} will process in async and update the voucher
     * details in the database.
     * If the instruction on request has user Phone Number, will send phone message after finished generate serial number
     * @param voucherData
     * @return
     */
    @PutMapping
    @Operation(summary = "Generate new voucher")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Created"),
            @ApiResponse(responseCode = "202", description = "Accepted"),
            @ApiResponse(responseCode = "400", description = "Invalid id supplied", content = @Content),
            @ApiResponse(responseCode = "404", description = "Not found", content = @Content) })
    public ResponseEntity<CreateVoucherResponse> createVoucher(@RequestBody VoucherGeneratingInstruction voucherData) {
        Voucher voucher = voucherService.createVoucher(voucherData.getUserName(), voucherData.getUserPhoneNumber(),
                voucherData.getVoucherType(), voucherData.getPaymentTransactionId());
        voucherService.generateSerialNumber(voucher);
        CreateVoucherResponse response = new CreateVoucherResponse();
        response.setId(voucher.getVoucherId());
        return ResponseEntity.created(WebMvcLinkBuilder.linkTo(VoucherController.class).slash(voucher.getVoucherId()).withSelfRel().toUri())
                .body(response);
    }

    @PatchMapping("/{voucherId}")
    @Operation(summary = "Get vouchers")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Ok"),
            @ApiResponse(responseCode = "400", description = "Invalid id supplied", content = @Content),
            @ApiResponse(responseCode = "404", description = "Not found", content = @Content) })
    public ResponseEntity<Void> updateVouchers(
            @PathVariable String voucherId,
            @RequestBody UpdateVoucherInstruction voucherData) {
        String phoneNumber = voucherData.getPhoneNumber();
        if (phoneNumber != null && !isValidPhoneNumber(phoneNumber)) {
            throw new BadRequestException("Invalid phone number"); // should not go with detail error message as sensitive data
        }

        Voucher toBeUpdated = voucherService.getById(voucherId);
        if (toBeUpdated == null) {
            throw new BadRequestException("Invalid voucher");
        }

        if (toBeUpdated.getUserPhoneNumber() != null) {
            throw new BadRequestException("Cannot update voucher");
        }

        if (toBeUpdated.getExpiredTime() != null && toBeUpdated.getExpiredTime().isBefore(LocalDateTime.now())) {
            throw new BadRequestException("Voucher was expired");
        }

        toBeUpdated.setUserPhoneNumber(phoneNumber);
        voucherService.updateVoucher(toBeUpdated);

        if (toBeUpdated.getStatus() == VoucherStatus.CREATED) {
            phoneService.sendVoucherMessage(toBeUpdated);
        }

        return ResponseEntity.ok().build();
    }
}
