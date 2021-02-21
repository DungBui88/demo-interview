package com.example.nab.demo.controllers;

import com.example.nab.demo.clients.VoucherGeneratorClient;
import com.example.nab.demo.dtos.CreateVoucherResponse;
import com.example.nab.demo.dtos.VoucherGeneratingInstruction;
import com.example.nab.demo.dtos.VoucherGeneratorCreateVoucherResponse;
import com.example.nab.demo.models.Voucher;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.lang3.RandomStringUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.web.server.LocalServerPort;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.time.LocalDateTime;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
class IntegrationTests {

    @Value("${client.voucher-generator.response-timeout:120000}")
    private Long clientResponseTimeOut;

    @LocalServerPort
    private int port;
    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private VoucherGeneratorClient voucherGeneratorClient;

    @Autowired
    private ObjectMapper mapper;

    @BeforeEach
    void init() {
        voucherGeneratorClient.setUrl("http://localhost:" + port);
        voucherGeneratorClient.setClientResponseTimeOut(clientResponseTimeOut + 1);
        voucherGeneratorClient.init();
    }

    @Test
    void testSuccessGenerateVoucherAfterPeriodOfTime() throws Exception {
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
        assertNull(voucher.getSeriesNumbers());

        waitForSecond(clientResponseTimeOut + 1);
        result = mockMvc.perform(get("/vouchers/" + voucherId)).andExpect(status().is2xxSuccessful()).andReturn();
        voucher = mapper.readValue(result.getResponse().getContentAsString(), Voucher.class);
        assertNotNull(voucher.getSeriesNumbers());
    }

    private void waitForSecond(long i) {
        try {
            TimeUnit.MILLISECONDS.sleep(i);
        } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
        }
    }

    private VoucherGeneratorCreateVoucherResponse createClientResponse(String type) {
        VoucherGeneratorCreateVoucherResponse generatedVoucher = new VoucherGeneratorCreateVoucherResponse();
        generatedVoucher.setSeriesNumber(RandomStringUtils.randomAlphanumeric(10));
        generatedVoucher.setExpiredDate(LocalDateTime.now().plusMonths(1));
        generatedVoucher.setType(type);
        return generatedVoucher;
    }
}
