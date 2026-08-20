package com.debtlens.backend.repository;

import com.debtlens.backend.entity.Class_Metrics;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface Class_MetricsRepository
        extends JpaRepository<Class_Metrics, Long> {

    // Returns one analysis job's class metrics in stable source-file order.
    List<Class_Metrics>
    findByAnalysisJobAnalysisIdOrderByFilePathAscStartLineAscClassNameAsc(
            Long analysisId
    );

    // Resolves the unique analyzed class region within one analysis job.
    Optional<Class_Metrics>
    findByAnalysisJobAnalysisIdAndFilePathAndStartLineAndClassName(
            Long analysisId,
            String filePath,
            Integer startLine,
            String className
    );
}
