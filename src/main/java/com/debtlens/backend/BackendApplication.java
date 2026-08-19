package com.debtlens.backend;

import com.debtlens.backend.config.Auth0Config;
import com.debtlens.backend.config.Auth0RoleConfig;
import com.debtlens.backend.config.GithubConfig;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication
@EnableConfigurationProperties({
        Auth0Config.class,
        Auth0RoleConfig.class,
        GithubConfig.class
})
public class BackendApplication {

    public static void main(String[] args) {
        SpringApplication.run(BackendApplication.class, args);
    }
}