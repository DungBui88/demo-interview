package com.example.nab.demo.controllers;

import com.example.nab.demo.TestSupportBase;
import com.example.nab.demo.clients.VoucherGeneratorClient;
import com.example.nab.demo.dtos.CreateVoucherResponse;
import com.example.nab.demo.dtos.UpdateVoucherInstruction;
import com.example.nab.demo.dtos.VoucherGeneratingInstruction;
import com.example.nab.demo.models.Voucher;
import com.example.nab.demo.models.VoucherStatus;
import com.example.nab.demo.service.PhoneService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import javax.ws.rs.InternalServerErrorException;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Test {@link VoucherController}
 */
@SpringBootTest
@AutoConfigureMockMvc
class VoucherControllerTests extends TestSupportBase {

    @MockBean
    private VoucherGeneratorClient voucherGeneratorClient;
    @MockBean
    private PhoneService phoneService;

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper mapper;

    /**
     * PUT /vouchers {@link VoucherGeneratingInstruction}
     * Success create new voucher without user name or user phone number
     */
    @Test
    void testSuccessCreateVoucher() throws Exception {
        when(voucherGeneratorClient.generateVoucher(anyString())).thenReturn(createClientResponse(("type1")));
        VoucherGeneratingInstruction requestContent = new VoucherGeneratingInstruction();
        requestContent.setVoucherType("type1");
        requestContent.setPaymentTransactionId("123");
        MvcResult response = mockMvc.perform(put("/vouchers")
                .accept(MediaType.APPLICATION_JSON)
                .contentType(MediaType.APPLICATION_JSON)
                .content(mapper.writeValueAsString(requestContent)))
                .andExpect(status().isCreated()).andReturn();

        String voucherId = mapper.readValue(response.getResponse().getContentAsString(), CreateVoucherResponse.class).getId();
        assertNotNull(voucherId);
        verify(voucherGeneratorClient, times(1)).generateVoucher(eq("type1"));

        response = mockMvc.perform(get("/vouchers/" + voucherId)).andExpect(status().is2xxSuccessful()).andReturn();
        Voucher result = mapper.readValue(response.getResponse().getContentAsString(), Voucher.class);
        assertNotNull(result.getVoucherId());
        assertEquals("type1", result.getVoucherType());
        assertEquals("123", result.getPaymentTransactionId());
        assertNull(result.getUserName());
        assertNull(result.getUserPhoneNumber());
        assertNotNull(result.getSerialNumber());
    }

    /**
     * PUT /vouchers {@link VoucherGeneratingInstruction}
     * Create voucher without Phone number, expect not send phone message.
     * PATCH /vouchers/{id} {@link UpdateVoucherInstruction}
     * Update voucher Phone Number, expect send phone message with serial number.
     */
    @Test
    void testSuccessCreateVoucherAndSendPhoneMessageWhenUpdatePhoneNumber() throws Exception {
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
        verify(phoneService, times(1)).sendVoucherMessage(any());

        result = mockMvc.perform(get("/vouchers/" + voucherId)).andExpect(status().is2xxSuccessful()).andReturn();
        Voucher voucher = mapper.readValue(result.getResponse().getContentAsString(), Voucher.class);
        assertNotNull(voucher.getSerialNumber());
        assertEquals("1234567890", voucher.getUserPhoneNumber());
    }

    /**
     * Create voucher with user phone number.
     * Expected send error messages to phone number when voucher generator client has exception.
     */
    @Test
    void testFailsCreateVoucherClientError() throws Exception {
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
        verify(voucherGeneratorClient, times(1)).generateVoucher(eq("type1"));

        // make sure async and recover method finished
        waitForPeriod(1);

        String voucherId = mapper.readValue(result.getResponse().getContentAsString(), CreateVoucherResponse.class).getId();
        result = mockMvc.perform(get("/vouchers/" + voucherId)).andExpect(status().is2xxSuccessful()).andReturn();
        Voucher voucher = mapper.readValue(result.getResponse().getContentAsString(), Voucher.class);
        assertEquals(VoucherStatus.CREATING_ERROR, voucher.getStatus());
        assertNull(voucher.getSerialNumber());
        verify(phoneService, times(1)).sendErrorMessage(any());
    }
}
