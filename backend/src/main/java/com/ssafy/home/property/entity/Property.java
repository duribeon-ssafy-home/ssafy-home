package com.ssafy.home.property.entity;

import com.ssafy.home.common.entity.BaseTimeEntity;
import com.ssafy.home.property.dto.PropertyUpdateRequest;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "properties")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class Property extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy= GenerationType.IDENTITY)
    private Long propertyId;

    @Column(name="owner_id")
    private Long ownerId;

    private String title;

    @Column(nullable=false)
    private String address;

    private String roadAddress;

    @Column(nullable=false)
    private String sido;

    @Column(nullable=false)
    private String gugun;

    @Column(nullable=false)
    private String dong;

    private BigDecimal latitude;
    private BigDecimal longitude;

    @Enumerated(EnumType.STRING)
    private RentType rentType;

    @Enumerated(EnumType.STRING)
    private RoomType roomType;

    private Long deposit;
    private Integer monthlyRent;
    private BigDecimal area;
    private Integer floor;
    private Integer buildYear;

    @Enumerated(EnumType.STRING)
    @Column(nullable=false)
    private DataSource dataSource;

    private LocalDate dealDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable=false)
    private PropertyStatus status;

    public void update(PropertyUpdateRequest request) {
        this.title = request.title();
        this.address = request.address();
        this.roadAddress = request.roadAddress();
        this.sido = request.sido();
        this.gugun = request.gugun();
        this.dong = request.dong();
        this.latitude = request.latitude();
        this.longitude = request.longitude();
        this.rentType = request.rentType();
        this.roomType = request.roomType();
        this.deposit = request.deposit();
        this.monthlyRent = request.monthlyRent();
        this.area = request.area();
        this.floor = request.floor();
        this.buildYear = request.buildYear();
        this.dealDate = request.dealDate();
    }

    public void delete() {
        this.status = PropertyStatus.DELETED;
    }
}
