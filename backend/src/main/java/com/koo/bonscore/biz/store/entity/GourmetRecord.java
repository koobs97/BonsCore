package com.koo.bonscore.biz.store.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * <pre>
 * GourmetRecord.java
 * 설명 : 맛집 방문 기록을 저장하는 메인 엔티티
 *       (가게 정보, 방문 날짜, 평점, 메모 등을 관리)
 * </pre>
 *
 * @author  : koobonsang
 * @version : 1.0
 * @since   : 2025-11-24
 */
@Entity
@Table(name = "GOURMET_RECORDS")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class GourmetRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "SEQ_GOURMET_RECORDS_GEN")
    @SequenceGenerator(name = "SEQ_GOURMET_RECORDS_GEN", sequenceName = "SEQ_GOURMET_RECORDS", allocationSize = 1)
    @Column(name = "RECORD_ID")
    private Long recordId;

    @Column(name = "USER_ID", nullable = false)
    private String userId;

    @Column(name = "STORE_NAME", nullable = false)
    private String storeName;

    @Column(name = "CATEGORY")
    private String category;

    @Column(name = "VISIT_DATE")
    private LocalDate visitDate; // DB타입에 맞춰 Date or LocalDate

    @Column(name = "RATING")
    private int rating;

    @Column(name = "MEMO")
    private String memo;

    @Column(name = "REFERENCE_URL")
    private String referenceUrl;

    @CreationTimestamp
    @Column(name = "CREATED_AT", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "UPDATED_AT")
    private LocalDateTime updatedAt;

    // 양방향 매핑, 부모 저장 시 자식도 저장/삭제 (CascadeType.ALL, orphanRemoval = true)
    @OneToMany(mappedBy = "gourmetRecord", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<GourmetImage> images = new ArrayList<>();

    @Builder
    public GourmetRecord(String userId, String storeName, String category, LocalDate visitDate, int rating, String memo, String referenceUrl) {
        this.userId = userId;
        this.storeName = storeName;
        this.category = category;
        this.visitDate = visitDate;
        this.rating = rating;
        this.memo = memo;
        this.referenceUrl = referenceUrl;
    }

    // 데이터 수정 메서드
    public void update(String storeName, String category, LocalDate visitDate, int rating, String memo, String referenceUrl) {
        this.storeName = storeName;
        this.category = category;
        this.visitDate = visitDate;
        this.rating = rating;
        this.memo = memo;
        this.referenceUrl = referenceUrl;
    }

    // 이미지 연관관계 편의 메서드
    public void addImage(GourmetImage image) {
        this.images.add(image);
        image.assignRecord(this);
    }
}
