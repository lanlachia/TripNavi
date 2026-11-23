package com.tripnavi;
import jakarta.persistence.*;
import java.time.LocalDateTime;
@Entity @Table(name="saved_places")
public class SavedPlace {
 @Id @GeneratedValue(strategy=GenerationType.IDENTITY) public Long id;
 @Column(nullable=false) public Long userId;
 @Column(nullable=false) public String name;
 public String address;
 public Double lat;
 public Double lon;
 @Column(length=2000) public String reason;
 public LocalDateTime createdAt=LocalDateTime.now();
}
