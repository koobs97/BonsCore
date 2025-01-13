package com.koo.bonscore;

import com.koo.bonscore.sample.controller.SampleController;
import com.koo.bonscore.sample.service.SampleService;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;

@SpringBootApplication
public class BonsCoreApplication {

    public static void main(String[] args) {
        ApplicationContext context = SpringApplication.run(BonsCoreApplication.class, args);
    }

    @Bean
    public CommandLineRunner commandLineRunner(ApplicationContext ctx) {
        return args -> {
            // ApplicationContext를 통해 SampleService 빈 가져오기
            SampleService sampleService = ctx.getBean(SampleService.class);

            sampleService.test();
        };
    }

}
