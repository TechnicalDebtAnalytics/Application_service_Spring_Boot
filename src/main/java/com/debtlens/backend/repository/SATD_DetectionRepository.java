package com.debtlens.backend.repository;

import com.debtlens.backend.entity.SATD_Detection;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface SATD_DetectionRepository
        extends JpaRepository<SATD_Detection, Long> {

    // Resolves the single current classification for one raw comment.
    Optional<SATD_Detection>
    findByClassCommentCommentId(Long commentId);

    // Supports idempotent persistence when an ML classification is received.
    boolean existsByClassCommentCommentId(Long commentId);

    // Retrieves all SATD classification results belonging to one analysis job.
    List<SATD_Detection>
    findByClassCommentClassMetricsAnalysisJobAnalysisId(Long analysisId);
}
