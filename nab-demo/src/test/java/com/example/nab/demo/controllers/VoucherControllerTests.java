package com.example.nab.demo.controllers;

import com.example.nab.demo.clients.VoucherGeneratorClient;
import com.example.nab.demo.dtos.CreateVoucherResponse;
import com.example.nab.demo.dtos.UpdateVoucherInstruction;
import com.example.nab.demo.dtos.VoucherGeneratingInstruction;
import com.example.nab.demo.dtos.VoucherGeneratorCreateVoucherResponse;
import com.example.nab.demo.models.Voucher;
import com.example.nab.demo.service.PhoneService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.lang3.RandomStringUtils;
import org.junit.jupiter.api.Test;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import javax.ws.rs.InternalServerErrorException;
import java.time.LocalDateTime;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Test {@link VoucherController}
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class VoucherControllerTests {

    @MockBean
    private VoucherGeneratorClient voucherGeneratorClient;
    @MockBean
    private PhoneService phoneService;
    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper mapper;

    /**
     * The standard work flow for generate voucher:
     * Case:
     * Arrange: user purchase without log in.
     * Act:
     * - After user finish payment process on the website, the website will send request generate new voucher
     * with voucher type and payment transaction code
     * - BE received request PUT /vouchers
     * - BE generate new voucher ID (for tracking and callback)
     * - BE store to the database
     * - BE Process async call third 3rd (VoucherGeneratorClient) for generate new voucher code
     * - BE return accepted status TODO: adding callback link too !!
     * - FE will call GET /vouchers/{ID} for period of time.
     * Assert:
     * - FE will success received new voucher data on time.
     */
    @Test
    void testSuccessGenerateVoucherTransactionFlow() throws Exception {
        when(voucherGeneratorClient.generateVoucher(anyString())).thenReturn(createClientResponse(("type1")));
        VoucherGeneratingInstruction requestContent = new VoucherGeneratingInstruction();
        requestContent.setVoucherType("type1");
        requestContent.setPaymentTransactionId("123");
        MvcResult result = mockMvc.perform(put("/vouchers")
                .accept(MediaType.APPLICATION_JSON)
                .contentType(MediaType.APPLICATION_JSON)
                .content(mapper.writeValueAsString(requestContent)))
                .andExpect(status().isCreated()).andReturn();

        String voucherId = mapper.readValue(result.getResponse().getContentAsString(), CreateVoucherResponse.class).getId();
        assertNotNull(voucherId);
        result = mockMvc.perform(get("/vouchers/" + voucherId)).andExpect(status().is2xxSuccessful()).andReturn();
        Voucher voucher = mapper.readValue(result.getResponse().getContentAsString(), Voucher.class);
        assertNotNull(voucher.getSeriesNumbers());
    }

    /**
     * The standard work flow for generate voucher:
     * Case:
     * Arrange: user purchase without log in.
     * Act:
     * - After user finish payment process on the website, the website will send request generate new voucher
     * with voucher type and payment transaction code
     * - BE received request PUT /vouchers
     * - BE generate new voucher ID (for tracking and callback)
     * - BE store to the database
     * - BE Process async call third 3rd (VoucherGeneratorClient) for generate new voucher code
     * - BE return accepted status and voucher ID TODO: adding callback link too !!
     * - FE will call GET /vouchers/{ID} for period of time.
     * - FE will not success received new voucher data on time.
     * - FE will send the PATCH /vouchers/{ID} which user phone number
     * Assert:
     * After BE finished generating voucher, will send the voucher code to user via Phone Number,
     * the record should be updated on the database as well
     */
    @Test
    void testSuccessGenerateVoucherAfterFEWaitingTimeout() throws Exception {
        when(voucherGeneratorClient.generateVoucher(anyString())).thenReturn(createClientResponse(("type1")));

        VoucherGeneratingInstruction requestContent = new VoucherGeneratingInstruction();
        requestContent.setVoucherType("type1");
        requestContent.setPaymentTransactionId("123");
        MvcResult result = mockMvc.perform(put("/vouchers")
                .accept(MediaType.APPLICATION_JSON)
                .contentType(MediaType.APPLICATION_JSON)
                .content(mapper.writeValueAsString(requestContent)))
                .andExpect(status().isCreated()).andReturn();
        verify(phoneService, times(0)).sendVoucherMessage(any());

        String voucherId = mapper.readValue(result.getResponse().getContentAsString(), CreateVoucherResponse.class).getId();
        UpdateVoucherInstruction requestUpdateContent = new UpdateVoucherInstruction();
        requestUpdateContent.setPhoneNumber("1234567890");
        mockMvc.perform(patch("/vouchers/" + voucherId)
                .accept(MediaType.APPLICATION_JSON)
                .contentType(MediaType.APPLICATION_JSON)
                .content(mapper.writeValueAsString(requestUpdateContent)))
                .andExpect(status().isOk());

        result = mockMvc.perform(get("/vouchers/" + voucherId)).andExpect(status().is2xxSuccessful()).andReturn();
        Voucher voucher = mapper.readValue(result.getResponse().getContentAsString(), Voucher.class);
        assertNotNull(voucher.getSeriesNumbers());
        assertEquals("1234567890", voucher.getUserPhoneNumber());
        verify(phoneService, times(1)).sendVoucherMessage(any());
    }

    /**
     * The standard work flow for generate voucher:
     * Case:
     * Arrange: user purchase without log in.
     * Act:
     * - After user finish payment process on the website, the website will send request generate new voucher
     * with voucher type and payment transaction code
     * - BE received request PUT /vouchers
     * - BE generate new voucher ID (for tracking and callback)
     * - BE store to the database
     * - BE Process async call third 3rd (VoucherGeneratorClient) for generate new voucher code
     * - BE return accepted status and voucher ID TODO: adding callback link too !!
     * - FE will call GET /vouchers/{ID} for period of time.
     * - FE will not success received new voucher data on time.
     * - FE will send the PATCH /vouchers/{ID} with user phone number
     * Assert:
     * When failed to generate the voucher, BE will send the message with payment code to the user's Phone Number,
     * the record should be updated on the database as well
     */
    @Test
    void testFailsGenerateVoucherAfterFEWaitingTimeout() throws Exception {
        when(voucherGeneratorClient.generateVoucher(anyString())).thenThrow(new InternalServerErrorException());

        VoucherGeneratingInstruction requestContent = new VoucherGeneratingInstruction();
        requestContent.setVoucherType("type1");
        requestContent.setPaymentTransactionId("123");
        requestContent.setUserPhoneNumber("1234567890");
        MvcResult result = mockMvc.perform(put("/vouchers")
                .accept(MediaType.APPLICATION_JSON)
                .contentType(MediaType.APPLICATION_JSON)
                .content(mapper.writeValueAsString(requestContent)))
                .andExpect(status().isCreated()).andReturn();
        String voucherId = mapper.readValue(result.getResponse().getContentAsString(), CreateVoucherResponse.class).getId();

        result = mockMvc.perform(get("/vouchers/" + voucherId)).andExpect(status().is2xxSuccessful()).andReturn();
        Voucher voucher = mapper.readValue(result.getResponse().getContentAsString(), Voucher.class);
        assertNull(voucher.getSeriesNumbers());
        verify(phoneService, times(1)).sendErrorMessage(any());
    }

    private VoucherGeneratorCreateVoucherResponse createClientResponse(String type) {
        VoucherGeneratorCreateVoucherResponse generatedVoucher = new VoucherGeneratorCreateVoucherResponse();
        generatedVoucher.setSeriesNumber(RandomStringUtils.randomAlphanumeric(10));
        generatedVoucher.setExpiredDate(LocalDateTime.now().plusMonths(1));
        generatedVoucher.setType(type);
        return generatedVoucher;
    }
}
