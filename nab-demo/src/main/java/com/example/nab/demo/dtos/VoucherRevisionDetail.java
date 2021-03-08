package com.example.nab.demo.dtos;

import com.example.nab.demo.models.Voucher;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class VoucherRevisionDetail extends Voucher {
    private int revisionNumber;
    private Instant revisionInstant;
}
