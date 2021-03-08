package com.example.nab.demo.clients;

import com.example.nab.demo.dtos.VoucherGeneratorCreateVoucherResponse;
import io.netty.channel.ChannelOption;
import io.netty.handler.timeout.ReadTimeoutHandler;
import io.netty.handler.timeout.WriteTimeoutHandler;
import lombok.Data;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import reactor.netty.http.client.HttpClient;

import javax.annotation.PostConstruct;

@Component
@Data
public class VoucherGeneratorClient {

    @Value("${client.voucher-generator.url:http://localhost:9090}")
    private String url;
    @Value("${client.voucher-generator.response-timeout:120}")
    private int clientResponseTimeOut;

    private WebClient client;

    @PostConstruct
    public void init() {
        setClient(WebClient.builder()
                .baseUrl(getUrl())
                .clientConnector(new ReactorClientHttpConnector(HttpClient.create()
                        .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, getClientResponseTimeOut() * 1000)
                        .doOnConnected(c -> c.addHandlerLast(new ReadTimeoutHandler(getClientResponseTimeOut()))
                                .addHandlerLast(new WriteTimeoutHandler(getClientResponseTimeOut())))))
                .build());
    }

    public VoucherGeneratorCreateVoucherResponse generateVoucher(String voucherType) {
        Mono<VoucherGeneratorCreateVoucherResponse> voucherMono = getClient().post()
                .uri("/generate/${voucherType}", voucherType)
                .retrieve()
                .bodyToMono(VoucherGeneratorCreateVoucherResponse.class);

        return voucherMono.block();
    }
}
