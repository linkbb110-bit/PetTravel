package com.nfu.pettravel;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

// 告诉 MyBatis-Plus 去哪里扫描 Mapper 接口
@MapperScan("com.nfu.pettravel.mapper")
@SpringBootApplication
@EnableScheduling//核心开关：允许 Spring 执行定时任务
public class PetTravelApplication {

    public static void main(String[] args) {
        SpringApplication.run(PetTravelApplication.class, args);
    }
}