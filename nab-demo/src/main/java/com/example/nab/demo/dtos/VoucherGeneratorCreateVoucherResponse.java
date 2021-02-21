package com.example.nab.demo.dtos;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class VoucherGeneratorCreateVoucherResponse {
    private String seriesNumber;
    private String type;
    private LocalDateTime expiredDate;
}
