package com.handyman.model;
import jakarta.persistence.*;
import java.time.Instant;
@Entity @Table(name="categories")
public class Category {
 @Id @GeneratedValue(strategy=GenerationType.IDENTITY) public Long id;
 @Column(nullable=false,length=120) public String name;
 @Column(nullable=false,unique=true,length=100) public String slug;
 @Column(nullable=false) public int sortOrder;
 @Column(nullable=false,updatable=false) public Instant createdAt = Instant.now();
}
