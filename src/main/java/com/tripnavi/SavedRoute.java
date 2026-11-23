package com.tripnavi;
import jakarta.persistence.*;
import java.time.LocalDateTime;
@Entity @Table(name="saved_routes")
public class SavedRoute {
 @Id @GeneratedValue(strategy=GenerationType.IDENTITY) public Long id;
 @Column(nullable=false) public Long userId;
 @Column(nullable=false) public String title;
 @Lob @Column(columnDefinition="LONGTEXT") public String placesJson;
 public LocalDateTime createdAt=LocalDateTime.now();
}
