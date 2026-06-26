package com.allegro.dicomback.entity.User;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "hospitals")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Hospital {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "HospitalId")
    private Long hospitalId;

    @Column(name = "HospitalName", length = 150)
    private String hospitalName;

    @Column(name = "HospitalAddress", length = 150)
    private String hospitalAddress;

    @Column(name = "HospitalPhone", length = 150)
    private String hospitalPhone;

    @Column(name = "HospitalPass")
    private String hospitalPass;
}