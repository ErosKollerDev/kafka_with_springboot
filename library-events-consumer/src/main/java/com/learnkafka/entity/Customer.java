package com.learnkafka.entity;


import jakarta.persistence.*;
import lombok.*;

import java.util.List;


@Getter
@Setter
//@Builder
//@AllArgsConstructor
//@RequiredArgsConstructor
//@NoArgsConstructor
@Entity
public class Customer extends Person {

    @Column(name = "customer_id")
    protected Long customer_id;

    @Embedded
    private Address address;

    @Embedded
    private Audit audit;


}
