package com.koo.bonscore;

import com.koo.bonscore.sample.SampleService;
import org.jasypt.encryption.pbe.PooledPBEStringEncryptor;
import org.jasypt.salt.RandomSaltGenerator;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ApplicationContext;

import java.security.SecureRandom;
import java.util.Base64;

@SpringBootApplication
public class BonsCoreApplication {

    public static void main(String[] args) {
        ApplicationContext context = SpringApplication.run(BonsCoreApplication.class, args);

        SampleService sampleService = context.getBean(SampleService.class);
        sampleService.test();
    }

}
