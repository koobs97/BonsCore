package com.koo.bonscore.biz.store.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/**
 * <pre>
 * GourmetImage.java
 * 설명 : 맛집 기록(GourmetRecord)에 첨부된 이미지 정보를 관리하는 엔티티
 *       (실제 이미지 파일은 S3나 로컬 스토리지에 저장하고, 여기엔 경로만 저장)
 * </pre>
 *
 * @author  : koobonsang
 * @version : 1.0
 * @since   : 2025-11-24
 */
@Entity
@Table(name = "GOURMET_IMAGES")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class GourmetImage {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "SEQ_GOURMET_IMAGES_GEN")
    @SequenceGenerator(name = "SEQ_GOURMET_IMAGES_GEN", sequenceName = "SEQ_GOURMET_IMAGES", allocationSize = 1)
    @Column(name = "IMAGE_ID")
    private Long imageId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "RECORD_ID")
    private GourmetRecord gourmetRecord;

    @Column(name = "IMAGE_ORDER")
    private int imageOrder;

    @Column(name = "ORIGINAL_FILE_NAME")
    private String originalFileName;

    @Column(name = "STORED_FILE_NAME")
    private String storedFileName;

    @Column(name = "IMAGE_URL")
    private String imageUrl;

    @Column(name = "FILE_SIZE")
    private Long fileSize;

    @CreationTimestamp
    @Column(name = "CREATED_AT", updatable = false)
    private LocalDateTime createdAt;

    // 연관관계 설정용
    public void assignRecord(GourmetRecord gourmetRecord) {
        this.gourmetRecord = gourmetRecord;
    }

    public void updateOrder(int order) {
        this.imageOrder = order;
    }

    public void updatePath(String storedFileName, String imageUrl) {
        this.storedFileName = storedFileName;
        this.imageUrl = imageUrl;
    }
}
