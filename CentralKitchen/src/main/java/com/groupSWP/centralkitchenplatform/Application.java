package com.groupSWP.centralkitchenplatform;

import com.groupSWP.centralkitchenplatform.entities.auth.Account;
import com.groupSWP.centralkitchenplatform.repositories.AccountRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.security.crypto.password.PasswordEncoder;

@SpringBootApplication
public class Application {

    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }

//    @Bean
//    CommandLineRunner initData(AccountRepository accountRepository,
//                               PasswordEncoder passwordEncoder) {
//        return args -> {
//            // Kiểm tra xem admin đã tồn tại chưa
//            if (accountRepository.findByUsername("admin").isEmpty()) {
//                Account admin = new Account();
//
//                // --- BỎ DÒNG SET ID ĐỂ DATABASE TỰ SINH ---
//                // admin.setAccountId(...);
//                admin.setAccountId(java.util.UUID.randomUUID());
//                admin.setUsername("admin");
//                admin.setPassword(passwordEncoder.encode("123456"));
//                admin.setRole("ADMIN");
//                // admin.setEmail("admin@test.com"); // Mở ra nếu cần email
//                accountRepository.save(admin);
//                System.out.println("---------------------------------------------");
//                System.out.println("✅ ĐÃ TẠO USER ADMIN THÀNH CÔNG!");
//                System.out.println("👉 Username: admin");
//                System.out.println("👉 Password: 123456");
//                System.out.println("---------------------------------------------");
//            } else {
//                System.out.println("ℹ️ ADMIN USER ĐÃ TỒN TẠI (Không cần tạo lại)");
//            }
//        };
//    }
}