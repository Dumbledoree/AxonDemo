package org.Dumbledoree.AxonDemo.model;


import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.Setter;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;

@Entity
@AllArgsConstructor
@NoArgsConstructor
@Data
public class KeyboardStock {

    @Id
    @GeneratedValue
    private Long id;


    private String name;

    private Integer account;





}
