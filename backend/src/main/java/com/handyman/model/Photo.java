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
 public Long getId() { return id; }
 public void setId(Long value) { this.id=value; }
 public Category getCategory() { return category; }
 public void setCategory(Category value) { this.category=value; }
 public String getCloudinaryPublicId() { return cloudinaryPublicId; }
 public void setCloudinaryPublicId(String value) { this.cloudinaryPublicId=value; }
 public String getCaption() { return caption; }
 public void setCaption(String value) { this.caption=value; }
 public int getSortOrder() { return sortOrder; }
 public void setSortOrder(int value) { this.sortOrder=value; }
 public Instant getCreatedAt() { return createdAt; }
 public void setCreatedAt(Instant value) { this.createdAt=value; }
}
