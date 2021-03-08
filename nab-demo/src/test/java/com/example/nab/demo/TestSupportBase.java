package com.example.nab.demo;

import com.example.nab.demo.dtos.VoucherGeneratorCreateVoucherResponse;
import org.apache.commons.lang3.RandomStringUtils;

import java.time.LocalDateTime;
import java.util.concurrent.TimeUnit;

public abstract class TestSupportBase {

    protected void waitForPeriod(long i) {
        try {
            TimeUnit.SECONDS.sleep(i);
        } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
        }
    }

    protected VoucherGeneratorCreateVoucherResponse createClientResponse(String type) {
        VoucherGeneratorCreateVoucherResponse generatedVoucher = new VoucherGeneratorCreateVoucherResponse();
        generatedVoucher.setSeriesNumber(RandomStringUtils.randomAlphanumeric(10));
        generatedVoucher.setExpiredDate(LocalDateTime.now().plusMonths(1));
        generatedVoucher.setType(type);
        return generatedVoucher;
    }
}
