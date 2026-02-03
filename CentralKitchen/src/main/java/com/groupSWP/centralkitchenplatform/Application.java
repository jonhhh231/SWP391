package com.groupSWP.centralkitchenplatform;

import com.groupSWP.centralkitchenplatform.entities.auth.Account;
// import com.groupSWP.centralkitchenplatform.entities.kitchen.Ingredient; // Nếu bạn chưa tạo Entity này thì cứ comment lại
import com.groupSWP.centralkitchenplatform.repositories.AccountRepository;
// import com.groupSWP.centralkitchenplatform.repositories.IngredientRepository; // Nếu chưa có Repo này thì comment lại
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.UUID; // Dùng để tạo ID ngẫu nhiên nếu cần

@SpringBootApplication

public class Application {

    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }

    // DATA SEEDING: Chạy tự động 1 lần khi khởi động Server
    @Bean
    CommandLineRunner initData(AccountRepository accountRepository,
                               // IngredientRepository ingredientRepository, // Mở ra khi nào bạn làm tới phần Nguyên liệu
                               PasswordEncoder passwordEncoder) {
        return args -> {
            // -----------------------------------------------------------
            // 1. TẠO TÀI KHOẢN ADMIN (Để login lấy Token)
            // -----------------------------------------------------------
            Account admin = accountRepository.findByUsername("admin")
                    .orElseGet(() -> {
                        Account newAdmin = new Account();
                        // Chuẩn: Lấy trực tiếp UUID
                        newAdmin.setAccountId(UUID.randomUUID());  // Tạo ID ngẫu nhiên (quan trọng nếu DB không tự tăng)
                        newAdmin.setUsername("admin");
                        return newAdmin;
                    });

            // Cập nhật thông tin Admin (Pass: 123456)
            admin.setPassword(passwordEncoder.encode("123456"));
            admin.setRole("ADMIN"); // Lưu ý: Nếu DB của bạn dùng Enum thì phải set đúng Enum, nếu String thì để "ADMIN" hoặc "ROLE_ADMIN"

            // Nếu Entity Account có trường storeId và yêu cầu unique, hãy để null cho Admin
            // admin.setStoreId(null);

            accountRepository.save(admin);
            System.out.println("✅ ADMIN USER UPDATED: admin / 123456");


            // -----------------------------------------------------------
            // 2. TẠO NGUYÊN LIỆU (Mở phần này khi bạn đã có Entity Ingredient)
            // -----------------------------------------------------------
            /*
            if (ingredientRepository.count() == 0) {
                 // Đảm bảo Entity Ingredient có constructor tương ứng
                 // ingredientRepository.save(...);
                 System.out.println("✅ ĐÃ NHẬP KHO NGUYÊN LIỆU MẪU");
            }
            */
        };
    }
}