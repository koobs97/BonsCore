package com.koo.bonscore;

import org.jasypt.encryption.pbe.PooledPBEStringEncryptor;
import org.jasypt.salt.RandomSaltGenerator;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.security.SecureRandom;
import java.util.Base64;

@SpringBootApplication
public class BonsCoreApplication {

    public static void main(String[] args) {
        SpringApplication.run(BonsCoreApplication.class, args);
    }

}
