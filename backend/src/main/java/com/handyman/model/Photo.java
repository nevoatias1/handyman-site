package com.handyman.model;
import jakarta.persistence.*;
import java.time.Instant;
@Entity @Table(name="photos")
public class Photo {
 @Id @GeneratedValue(strategy=GenerationType.IDENTITY) public Long id;
 @ManyToOne(optional=false,fetch=FetchType.LAZY) @JoinColumn(name="category_id",nullable=false) public Category category;
 @Column(nullable=false,unique=true,length=255) public String cloudinaryPublicId;
 @Column(length=200) public String caption;
 @Column(nullable=false) public int sortOrder;
 @Column(nullable=false,updatable=false) public Instant createdAt = Instant.now();
}
