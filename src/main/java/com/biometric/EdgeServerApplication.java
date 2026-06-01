package com.biometric;

import ch.qos.logback.classic.Level;
import lombok.extern.slf4j.Slf4j;
import org.cloudsimplus.util.Log;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.transaction.annotation.EnableTransactionManagement;

@SpringBootApplication
@EnableTransactionManagement
@Slf4j
public class EdgeServerApplication {
    public static void main(String[] args) {
        Log.setLevel(Level.INFO);
        SpringApplication.run(EdgeServerApplication.class, args);
    }
}
