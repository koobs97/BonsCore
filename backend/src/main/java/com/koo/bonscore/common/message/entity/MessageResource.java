package com.koo.bonscore.common.message.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "MESSAGE_RESOURCE", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"CODE", "LOCALE"})
})
public class MessageResource {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "CODE", nullable = false)
    private String code; // 예: login.title

    @Column(name = "LOCALE", nullable = false, length = 10)
    private String locale; // 예: ko, en

    @Lob
    @Column(name = "MESSAGE", nullable = false)
    private String message; // 예: 로그인

    @CreationTimestamp
    @Column(name = "CREATED_AT", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "UPDATED_AT")
    private LocalDateTime updatedAt;
}
