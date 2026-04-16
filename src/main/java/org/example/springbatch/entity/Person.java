package org.example.springbatch.entity;

import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection = "person_in")
@Getter
@Setter
public class Person {

    @Id
    private String id;
    private String name;
}
