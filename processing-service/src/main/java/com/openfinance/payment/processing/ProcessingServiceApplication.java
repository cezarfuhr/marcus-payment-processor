package com.openfinance.payment.processing;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication(scanBasePackages = {
        "com.openfinance.payment.processing",
        "com.openfinance.payment.common"
})
@EnableJpaRepositories(basePackages = "com.openfinance.payment.common.repository")
@EntityScan(basePackages = "com.openfinance.payment.common.entity")
@EnableScheduling
public class ProcessingServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(ProcessingServiceApplication.class, args);
    }
}
