package com.groupSWP.centralkitchenplatform.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

@Configuration
@EnableJpaAuditing // Kích hoạt tính năng tự động ghi ngày giờ
public class JpaAuditingConfig {
}