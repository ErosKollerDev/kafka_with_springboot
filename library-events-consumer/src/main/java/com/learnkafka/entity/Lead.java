package com.learnkafka.entity;


import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Entity
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Lead {

    @Id
    @GeneratedValue(strategy = jakarta.persistence.GenerationType.AUTO)
    private Long leadId;


    @OneToOne(mappedBy = "lead")
    private Proposal proposal;

    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(name = "Lead_Product",
            joinColumns = @JoinColumn(name = "leadId"), inverseJoinColumns = @JoinColumn(name = "productId"))
    private List<Product> products;


}
