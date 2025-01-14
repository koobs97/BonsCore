package com.koo.bonscore;

import com.koo.bonscore.sample.controller.SampleController;
import com.koo.bonscore.sample.service.SampleService;
import com.koo.bonscore.sample.vo.SampleVo;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;

@SpringBootApplication
public class BonsCoreApplication {

    public static void main(String[] args) {
        ApplicationContext context = SpringApplication.run(BonsCoreApplication.class, args);
        SampleController sampleController = context.getBean(SampleController.class);

        SampleVo vo = new SampleVo(9);
        Boolean response = sampleController.test(vo);
        System.out.println("Response from SampleController: " + response);
    }

}
