package com.debtlens.backend.repository;

import com.debtlens.backend.entity.Class_Metrics;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

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

    // Counts total classes analyzed for one analysis job.
    int countByAnalysisJobAnalysisId(Long analysisId);

    interface AnalysisClassCount {
        Long getAnalysisId();
        Long getClassCount();
    }

    @Query("""
            select cm.analysisJob.analysisId as analysisId, count(cm) as classCount
            from Class_Metrics cm
            where cm.analysisJob.analysisId in :analysisIds
            group by cm.analysisJob.analysisId
            """)
    List<AnalysisClassCount> countByAnalysisIds(List<Long> analysisIds);
}
