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
 // Access identifiers through a method so Hibernate lazy proxies can resolve them.
 public Long getId() { return id; }
 public void setId(Long value) { this.id=value; }
 public String getName() { return name; }
 public void setName(String value) { this.name=value; }
 public String getSlug() { return slug; }
 public void setSlug(String value) { this.slug=value; }
 public int getSortOrder() { return sortOrder; }
 public void setSortOrder(int value) { this.sortOrder=value; }
 public Instant getCreatedAt() { return createdAt; }
 public void setCreatedAt(Instant value) { this.createdAt=value; }
}
