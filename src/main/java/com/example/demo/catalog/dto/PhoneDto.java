package com.example.demo.catalog.dto;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

@Getter
@Setter
public class PhoneDto {

    private UUID id;

    private String name;

    private String model;

    private String processor;

    private BigDecimal screenSize;

    private Integer battery;

    private String camera;

    private String operatingSystem;

    private LocalDate releaseDate;

    private UUID brandId;

    private UUID categoryId;

}
