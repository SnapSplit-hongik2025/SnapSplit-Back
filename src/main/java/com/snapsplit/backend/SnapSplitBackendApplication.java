package com.snapsplit.backend;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@EntityScan(basePackages = "com.snapsplit.backend")
@EnableJpaRepositories(basePackages = "com.snapsplit.backend")
@SpringBootApplication
public class SnapSplitBackendApplication {

    public static void main(String[] args) {
        SpringApplication.run(SnapSplitBackendApplication.class, args);
    }

}
