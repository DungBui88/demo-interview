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
import java.time.Duration;
import java.util.concurrent.TimeUnit;

@Component
@Data
public class VoucherGeneratorClient {

    @Value("${client.voucher-generator.url:http://localhost:9090}")
    private String url;
    @Value("${client.voucher-generator.response-timeout:120000}")
    private Long clientResponseTimeOut;

    private WebClient client;

    @PostConstruct
    public void init() {
        HttpClient httpClient = HttpClient.create()
                .baseUrl(getUrl())
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, getClientResponseTimeOut().intValue())
                .responseTimeout(Duration.ofMillis(getClientResponseTimeOut()))
                .doOnConnected(conn ->
                        conn.addHandlerLast(new ReadTimeoutHandler(getClientResponseTimeOut(), TimeUnit.MILLISECONDS))
                                .addHandlerLast(new WriteTimeoutHandler(getClientResponseTimeOut(), TimeUnit.MILLISECONDS)));

        setClient(WebClient.builder()
                .clientConnector(new ReactorClientHttpConnector(httpClient))
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
