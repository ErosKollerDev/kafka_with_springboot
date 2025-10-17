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
public class Proposal {

    @Id
    @GeneratedValue(strategy = jakarta.persistence.GenerationType.AUTO)
    private Long proposalId;

    @OneToOne
    @JoinColumn(name = "leadId")
    private Lead lead;

    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(
            name="proposal_product",
            joinColumns = @JoinColumn(name = "proposalId"),inverseJoinColumns = @JoinColumn(name = "productId")
    )
    private List<Product> products;

}
