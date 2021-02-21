package com.example.nab.demo.service;

import com.example.nab.demo.clients.VoucherGeneratorClient;
import com.example.nab.demo.dtos.VoucherGeneratorCreateVoucherResponse;
import com.example.nab.demo.models.Voucher;
import com.example.nab.demo.models.VoucherStatus;
import com.example.nab.demo.repositories.VoucherRepository;
import lombok.Data;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.retry.annotation.Recover;
import org.springframework.retry.annotation.Retryable;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

@Service
@Log4j2
@Data
public class VoucherManagementService {

    @Autowired
    private VoucherRepository voucherRepository;
    @Autowired
    private PhoneService phoneService;
    @Autowired
    private VoucherGeneratorClient voucherGeneratorClient;

    public List<Voucher> getByPhoneNumber(String phoneNumber) {
        return voucherRepository.findAllByUserPhoneNumber(phoneNumber);
    }

    public List<Voucher> getAll() {
        return StreamSupport.stream(voucherRepository.findAll().spliterator(), false).collect(Collectors.toList());
    }

    public Voucher getById(String id) {
        return getVoucherRepository().findById(id).orElse(null);
    }

    public List<Voucher> getByPaymentTransactionId(String paymentTransactionId) {
        Optional<Voucher> maybeVoucher = voucherRepository.findByPaymentTransactionId(paymentTransactionId);
        if (maybeVoucher.isPresent()) {
            return Arrays.asList(maybeVoucher.get());
        }

        return Collections.emptyList();
    }

    public void updateVoucher(Voucher toBeUpdated) {
        voucherRepository.save(toBeUpdated);
    }

    public Voucher createVoucher(String userName, String userPhoneNumber, String voucherType, String paymentTransactionId) {
        Voucher toBeCreated = new Voucher(userName, userPhoneNumber, voucherType, paymentTransactionId);
        return getVoucherRepository().save(toBeCreated);
    }

    @Async
    @Retryable(maxAttempts = 1, recover = "recoverGenerateVoucher")
    public void generateVoucherSerial(Voucher voucher) {
        // expect this will be long, thread will wait
        VoucherGeneratorCreateVoucherResponse responseData = getVoucherGeneratorClient().generateVoucher(voucher.getVoucherType());
        voucher.setSeriesNumbers(responseData.getSeriesNumber());
        voucher.setExpiredTime(responseData.getExpiredDate());
        voucher.setStatus(VoucherStatus.CREATED);
        voucher = getVoucherRepository().save(voucher);
        if (voucher.getUserPhoneNumber() != null) {
            getPhoneService().sendVoucherMessage(voucher);
        }
    }

    @Recover
    public void recoverGenerateVoucher(Exception ex, Voucher voucher) {
        log.warn("Recover fail create Voucher");
        log.error(ex);

        voucher.setStatus(VoucherStatus.CREATING_ERROR);
        getVoucherRepository().save(voucher);
        if (voucher.getUserPhoneNumber() != null) {
            phoneService.sendErrorMessage(voucher);
        }
    }
}