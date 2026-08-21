package com.debtlens.backend.repository;

import com.debtlens.backend.entity.Class_Comment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface Class_CommentRepository
        extends JpaRepository<Class_Comment, Long> {

    // Returns all raw comments extracted from one analyzed class.
    List<Class_Comment>
    findByClassMetricsClassId(Long classId);

    // Returns all raw comments extracted during one analysis job.
    List<Class_Comment>
    findByClassMetricsAnalysisJobAnalysisId(Long analysisId);
}
