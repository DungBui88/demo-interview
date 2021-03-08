package com.example.nab.demo.service;

import com.example.nab.demo.TestSupportBase;
import com.example.nab.demo.clients.VoucherGeneratorClient;
import com.example.nab.demo.dtos.VoucherGeneratorCreateVoucherResponse;
import com.example.nab.demo.models.Voucher;
import com.example.nab.demo.models.VoucherStatus;
import com.example.nab.demo.repositories.VoucherRepository;
import org.apache.commons.lang3.RandomStringUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;

import javax.ws.rs.InternalServerErrorException;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.concurrent.Future;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@SpringBootTest
class VoucherManagementServiceTests extends TestSupportBase {

    @Autowired
    private VoucherManagementService voucherManagementService;
    @Autowired
    private VoucherRepository voucherRepository;
    @MockBean
    private VoucherGeneratorClient voucherGeneratorClient;
    @MockBean
    private PhoneService phoneService;

    @BeforeEach
    void init() {
        voucherRepository.deleteAll();
    }

    /**
     * Test {@link VoucherManagementService#createVoucher(String, String, String, String)}
     * Should success and get correct database record.
     */
    @Test
    void testCreateVoucherSuccess() {
        String userName = "admin";
        String userPhoneNumber = "1234567890";
        String voucherType = "type1";
        String paymentId = "pay001";
        Voucher voucher = voucherManagementService.createVoucher(userName, userPhoneNumber, voucherType, paymentId);
        Optional<Voucher> result = voucherRepository.findById(voucher.getVoucherId());
        assertTrue(result.isPresent());
        assertNotNull(result.get().getVoucherId());
        assertEquals(userName, result.get().getUserName());
        assertEquals(userPhoneNumber, result.get().getUserPhoneNumber());
        assertEquals(voucherType, result.get().getVoucherType());
        assertEquals(paymentId, result.get().getPaymentTransactionId());
        assertEquals(VoucherStatus.CREATING, result.get().getStatus());
    }

    /**
     * Test {@link VoucherManagementService#createVoucher(String, String, String, String)}
     * Should success even when all input parameters was null. This method expect to have no specific logic.
     */
    @Test
    void testCreateVoucherSuccessWhenAllInputAreNull() {
        Voucher voucher = voucherManagementService.createVoucher(null, null, null, null);
        Optional<Voucher> result = voucherRepository.findById(voucher.getVoucherId());
        assertTrue(result.isPresent());
        assertNotNull(result.get().getVoucherId());
        assertNull(result.get().getUserName());
        assertNull(result.get().getUserPhoneNumber());
        assertNull(result.get().getVoucherType());
        assertNull(result.get().getPaymentTransactionId());
        assertEquals(VoucherStatus.CREATING, result.get().getStatus());
    }

    /**
     * Test {@link VoucherManagementService#updateVoucher(Voucher)}
     * Should throw exception if voucher not exits
     */
    @Test
    void testUpdateVoucherShouldThrowExceptionWhenVoucherNotExits() {
        assertThrows(IllegalArgumentException.class, () -> voucherManagementService.updateVoucher(new Voucher()));

        Voucher voucher = new Voucher();
        voucher.setVoucherId("001");
        assertThrows(IllegalArgumentException.class, () -> voucherManagementService.updateVoucher(voucher));
    }

    /**
     * Test {@link VoucherManagementService#updateVoucher(Voucher)}
     * Should throw exception if voucher not exits
     */
    @Test
    void testUpdateVoucherShouldSuccess() {
        Voucher voucher = new Voucher();
        voucher.setVoucherId("001");
        voucherRepository.save(voucher);
        assertNull(voucher.getSerialNumber());

        String newSerialNumber = "12345";
        voucher.setSerialNumber(newSerialNumber);
        Voucher result = voucherManagementService.updateVoucher(voucher);
        assertEquals(newSerialNumber, result.getSerialNumber());
    }

    /**
     * Test {@link VoucherManagementService#generateSerialNumber(Voucher)}
     * Should have new serial number and expired time after.
     * @throws Exception
     */
    @Test
    void testGenerateSerialNumberSuccess() throws Exception {
        when(voucherGeneratorClient.generateVoucher(any())).thenAnswer(invocation -> {
            waitForPeriod(2);
            VoucherGeneratorCreateVoucherResponse response = new VoucherGeneratorCreateVoucherResponse();
            response.setSeriesNumber(RandomStringUtils.randomAlphanumeric(10));
            response.setExpiredDate(LocalDateTime.now().plusMonths(1));
            return response;
        });

        Voucher voucher = new Voucher();
        voucher.setVoucherType("type1");
        assertNull(voucher.getSerialNumber());
        assertNull(voucher.getExpiredTime());
        voucherRepository.saveAndFlush(voucher);

        Future<Voucher> voucherFuture = voucherManagementService.generateSerialNumber(voucher);
        Voucher result = voucherFuture.get();
        assertEquals(voucher.getVoucherId(), result.getVoucherId());
        assertEquals("type1", result.getVoucherType());
        assertNotNull(result.getSerialNumber());
        assertNotNull(result.getExpiredTime());
    }

    /**
     * Test {@link VoucherManagementService#generateSerialNumber(Voucher)}
     * Should send phone message when done.
     * @throws Exception
     */
    @Test
    void testGenerateSerialNumberSuccessShouldSendPhoneMessage() throws Exception {
        when(voucherGeneratorClient.generateVoucher(any())).thenAnswer(invocation -> {
            VoucherGeneratorCreateVoucherResponse response = new VoucherGeneratorCreateVoucherResponse();
            response.setSeriesNumber(RandomStringUtils.randomAlphanumeric(10));
            response.setExpiredDate(LocalDateTime.now().plusMonths(1));
            return response;
        });

        Voucher voucher = new Voucher();
        voucher.setVoucherType("type1");
        voucher.setUserPhoneNumber("1234567890");
        voucherRepository.saveAndFlush(voucher);

        Future<Voucher> voucherFuture = voucherManagementService.generateSerialNumber(voucher);
        voucherFuture.get();
        verify(phoneService, times(1)).sendVoucherMessage(any());
    }

    /**
     * Test {@link VoucherManagementService#generateSerialNumber(Voucher)}
     * Should have status VoucherStatus.CREATING_ERROR when exception occur.
     * @throws Exception
     */
    @Test
    void testGenerateSerialNumberHasClientExceptionShouldUpdateStatusToCreatingError() throws Exception {
        when(voucherGeneratorClient.generateVoucher(any())).thenThrow(new InternalServerErrorException());

        Voucher voucher = new Voucher();
        voucher.setVoucherType("type1");
        assertNull(voucher.getSerialNumber());
        assertNull(voucher.getExpiredTime());
        voucherRepository.saveAndFlush(voucher);

        Future<Voucher> voucherFuture = voucherManagementService.generateSerialNumber(voucher);
        Voucher result = voucherFuture.get();
        assertEquals(voucher.getVoucherId(), result.getVoucherId());
        assertEquals("type1", result.getVoucherType());
        assertEquals(VoucherStatus.CREATING_ERROR, voucher.getStatus());
        assertNull(result.getSerialNumber());
        assertNull(result.getExpiredTime());
    }

    /**
     * Test {@link VoucherManagementService#generateSerialNumber(Voucher)}
     * Should send phone message about the error when an Exception occur.
     * @throws Exception
     */
    @Test
    void testGenerateSerialNumberHasExceptionShouldSendPhoneErrorMessage() throws Exception {
        when(voucherGeneratorClient.generateVoucher(any())).thenThrow(new InternalServerErrorException());

        Voucher voucher = new Voucher();
        voucher.setVoucherType("type1");
        voucher.setUserPhoneNumber("1234567890");
        voucherRepository.saveAndFlush(voucher);

        Future<Voucher> voucherFuture = voucherManagementService.generateSerialNumber(voucher);
        voucherFuture.get();
        verify(phoneService, times(0)).sendVoucherMessage(any());
        verify(phoneService, times(1)).sendErrorMessage(any());
    }
}
