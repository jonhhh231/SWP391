package com.groupSWP.centralkitchenplatform;
import jakarta.annotation.PostConstruct;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

import java.util.TimeZone;


@SpringBootApplication
@EnableScheduling
public class Application {
    // Hàm này sẽ chạy ngay khi Spring Boot vừa khởi động lên
    @PostConstruct
    public void init() {
        // Ép toàn bộ JVM của Server chạy theo giờ Việt Nam
        TimeZone.setDefault(TimeZone.getTimeZone("Asia/Ho_Chi_Minh"));
    }
    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }
}