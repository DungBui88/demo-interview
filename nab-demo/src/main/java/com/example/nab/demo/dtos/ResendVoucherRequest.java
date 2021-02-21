package com.example.nab.demo.dtos;

import lombok.Data;

import javax.validation.constraints.NotNull;

@Data
public class ResendVoucherRequest {
    @NotNull
    private String phoneNumber;
}
