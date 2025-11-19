package com.openfinance.payment.reconciliation;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication(scanBasePackages = {
        "com.openfinance.payment.reconciliation",
        "com.openfinance.payment.common"
})
@EnableJpaRepositories(basePackages = "com.openfinance.payment.common.repository")
@EntityScan(basePackages = "com.openfinance.payment.common.entity")
@EnableScheduling
public class ReconciliationServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(ReconciliationServiceApplication.class, args);
    }
}
