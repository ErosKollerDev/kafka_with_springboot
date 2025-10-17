package com.learnkafka.entity;


import jakarta.persistence.*;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

//@Inheritance(strategy = InheritanceType.JOINED)
//@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
//@DiscriminatorColumn(name = "person_type")
@Inheritance(strategy = InheritanceType.TABLE_PER_CLASS)
@Entity
//@AllArgsConstructor
//@NoArgsConstructor
@Getter
@Setter
public abstract class Person {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    protected Long id;

    protected String name;
    protected String email;
    protected int age;

    @Enumerated(EnumType.STRING)
    protected Gender gender;

    @Version
    private int version;

    public Person() {

    }

    public Person(Long personId, String name, String email, int age, Gender gender, int version) {
        this.id = personId;
        this.name = name;
        this.email = email;
        this.age = age;
        this.gender = gender;
        this.version = version;
    }
}

