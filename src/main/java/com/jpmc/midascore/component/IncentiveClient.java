package com.jpmc.midascore.component;

import com.jpmc.midascore.foundation.Incentive;
import com.jpmc.midascore.foundation.Transaction;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

@Component
public class IncentiveClient {

    private final RestTemplate restTemplate;

    // Per task: local service on 8080
    private static final String INCENTIVE_URL = "http://localhost:8080/incentive";

    public IncentiveClient(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    public float fetchIncentiveAmount(Transaction tx) {
        Incentive response = restTemplate.postForObject(INCENTIVE_URL, tx, Incentive.class);
        if (response == null) return 0f;
        return Math.max(0f, response.getAmount());
    }
}