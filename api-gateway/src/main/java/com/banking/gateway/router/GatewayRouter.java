package com.banking.gateway.router;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;

@RestController
public class GatewayRouter {

    private final List<RestClient> accountClients;
    private final AtomicInteger accountRoundRobin = new AtomicInteger(0);
    private final RestClient transferClient;

    public GatewayRouter(@Value("${account.service.urls}") String accountUrls,
                         @Value("${transfer.service.url}") String transferUrl) {
        this.accountClients = List.of(accountUrls.split(",")).stream()
                .map(url -> RestClient.builder().baseUrl(url.trim()).build())
                .toList();
        this.transferClient = RestClient.builder().baseUrl(transferUrl).build();
    }

    @RequestMapping("/accounts/**")
    public ResponseEntity<String> routeToAccountService(HttpServletRequest request,
                                                         @RequestBody(required = false) String body) {
        int index = accountRoundRobin.getAndIncrement() % accountClients.size();
        return forward(accountClients.get(index), request, body);
    }

    @RequestMapping("/transfers/**")
    public ResponseEntity<String> routeToTransferService(HttpServletRequest request,
                                                          @RequestBody(required = false) String body) {
        return forward(transferClient, request, body);
    }

    private ResponseEntity<String> forward(RestClient client,
                                            HttpServletRequest request,
                                            String body) {
        String path = request.getRequestURI();
        String query = request.getQueryString();
        String uri = query != null ? path + "?" + query : path;

        RestClient.RequestBodySpec spec = client
                .method(HttpMethod.valueOf(request.getMethod()))
                .uri(uri)
                .contentType(org.springframework.http.MediaType.APPLICATION_JSON);

        return Optional.ofNullable(body)
                .map(b -> spec.body(b).retrieve().toEntity(String.class))
                .orElse(spec.retrieve().toEntity(String.class));
    }
}
