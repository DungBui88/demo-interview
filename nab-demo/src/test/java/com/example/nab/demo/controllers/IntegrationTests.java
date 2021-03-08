package com.example.nab.demo.controllers;

import com.example.nab.demo.TestSupportBase;
import com.example.nab.demo.clients.VoucherGeneratorClient;
import com.example.nab.demo.dtos.CreateVoucherResponse;
import com.example.nab.demo.dtos.VoucherGeneratingInstruction;
import com.example.nab.demo.models.Voucher;
import com.fasterxml.jackson.databind.ObjectMapper;
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

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
class IntegrationTests extends TestSupportBase {

    @Value("${client.voucher-generator.delay-response-time:110}")
    private int delayTime;

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
        voucherGeneratorClient.init();
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
     * - BE return created status with voucher id
     * - FE will call GET /vouchers/{ID} for period of time.
     * Assert:
     * - FE will success received new voucher data after period of time.
     */
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
        assertNull(voucher.getSerialNumber());

        waitForPeriod(delayTime + 3);

        result = mockMvc.perform(get("/vouchers/" + voucherId)).andExpect(status().is2xxSuccessful()).andReturn();
        voucher = mapper.readValue(result.getResponse().getContentAsString(), Voucher.class);
        assertNotNull(voucher.getSerialNumber());
    }
}
