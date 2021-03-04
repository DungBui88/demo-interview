package com.example.nab.demo.dtos;

import lombok.Data;

import javax.validation.constraints.NotNull;

@Data
public class VoucherGeneratingInstruction {
    /**
     * Optional, for supporting purpose if user logged in when process the payment
     */
    private String userName;

    /**
     * Optional as user may want to send the code to his phone from beginning
     * the website will have option like checkbox sending voucher direct to phone instead of showing on the website
     */
    // TODO: validate phone format
    private String userPhoneNumber;

    /**
     * Identify which type of voucher we should generate
     */
    @NotNull
    private String voucherType;

    /**
     * After user purchase successfully, store a payment transaction code for, this for supporting user only
     * we may no be able to validate this code on server side.
     */
    @NotNull
    private String paymentTransactionId;
}
