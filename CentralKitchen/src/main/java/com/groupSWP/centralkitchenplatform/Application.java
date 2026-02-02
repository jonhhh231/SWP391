package com.groupSWP.centralkitchenplatform;

//import com.groupSWP.centralkitchenplatform.entities.auth.Account;
//import com.groupSWP.centralkitchenplatform.entities.kitchen.Ingredient;
//import com.groupSWP.centralkitchenplatform.repositories.AccountRepository;
//import com.groupSWP.centralkitchenplatform.repositories.IngredientRepository;
//import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
//import org.springframework.context.annotation.Bean;
//import org.springframework.security.crypto.password.PasswordEncoder;
//
//import java.math.BigDecimal;
//import java.util.List;

@SpringBootApplication
public class Application {

    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }

    // DATA SEEDING: Chạy tự động khi khởi động
//    @Bean
//    CommandLineRunner initData(AccountRepository accountRepository,
//                               IngredientRepository ingredientRepository,
//                               PasswordEncoder passwordEncoder) {
//        return args -> {
//            // 1. XỬ LÝ ADMIN (Kiểu gì cũng update lại cho đúng ý Sếp)
//            Account admin = accountRepository.findByUsername("admin")
//                    .orElseGet(() -> {
//                        Account newAdmin = new Account();
//                        newAdmin.setUsername("admin");
//                        return newAdmin;
//                    });
//
//            // Set lại thông tin mới nhất (ghi đè cái cũ)
//            admin.setPassword(passwordEncoder.encode("123456"));
//            admin.setRole("ROLE_ADMIN"); // QUAN TRỌNG: Phải có chữ ROLE_ thì Spring Security mới hiểu
//            // admin.setStore(null);
//
//            accountRepository.save(admin);
//            System.out.println("ĐÃ CẬP NHẬT ADMIN: admin / 123456 (Role: ROLE_ADMIN)");
//
//
//            // 2. TẠO NGUYÊN LIỆU (Giữ nguyên)
//            if (ingredientRepository.count() == 0) {
//                List<Ingredient> ingredients = List.of(
//                        new Ingredient("THIT-BO", "Thịt Bò Tươi", BigDecimal.valueOf(100.0), "kg", BigDecimal.valueOf(250000), BigDecimal.valueOf(5.0), null, null),
//                        new Ingredient("BANH-PHO", "Bánh Phở", BigDecimal.valueOf(200.0), "kg", BigDecimal.valueOf(15000), BigDecimal.valueOf(5.0), null, null),
//                        new Ingredient("HANH-TAY", "Hành Tây", BigDecimal.valueOf(50.0), "kg", BigDecimal.valueOf(20000), BigDecimal.valueOf(1.0), null, null)
//                );
//                ingredientRepository.saveAll(ingredients);
//                System.out.println("ĐÃ NHẬP KHO: Thịt bò, Bánh phở, Hành tây...");
//            }
//        };
//    }
}