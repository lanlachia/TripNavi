package com.tripnavi;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "inquiries")
public class Inquiry {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false) private Long userId;
    @Column(nullable = false, length = 120) private String title;
    @Column(nullable = false, columnDefinition = "TEXT") private String message;
    @Column(nullable = false) private LocalDateTime createdAt = LocalDateTime.now();
    public Long getId() { return id; }
    public Long getUserId() { return userId; }
    public void setUserId(Long value) { userId = value; }
    public String getTitle() { return title; }
    public void setTitle(String value) { title = value; }
    public String getMessage() { return message; }
    public void setMessage(String value) { message = value; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}
