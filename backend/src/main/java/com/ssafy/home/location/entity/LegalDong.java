package com.ssafy.home.location.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "legal_dongs")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class LegalDong {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 10)
    private String code;

    @Column(nullable = false, length = 30)
    private String sido;

    @Column(length = 50)
    private String gugun;

    @Column(length = 50)
    private String dong;

    @Column(nullable = false, length = 120)
    private String fullName;

    @Column(nullable = false)
    private boolean active;
}
