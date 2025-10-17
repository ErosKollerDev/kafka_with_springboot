package com.learnkafka.entity;


import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;


@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
public class UIdDocument {


    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;

    private String documentId;

//    @OneToOne(mappedBy = "uIdDocument")
//    private Person person;

}
