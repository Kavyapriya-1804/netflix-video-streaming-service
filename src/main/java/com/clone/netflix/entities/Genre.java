package com.clone.netflix.entities;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "genres")
public class Genre {

    @Id
    private  String id;

    private  String title;

//    @OneToMany(mappedBy = "genres")
//    private List<Video> list=new ArrayList<>();
}
