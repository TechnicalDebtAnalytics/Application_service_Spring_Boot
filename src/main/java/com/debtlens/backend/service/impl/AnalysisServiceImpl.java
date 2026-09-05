package com.debtlens.backend.service.impl;

import com.debtlens.backend.dto.messaging.*;
import com.debtlens.backend.dto.response.AnalysisResponseDTO;
import com.debtlens.backend.entity.*;
import com.debtlens.backend.exception.BadRequestException;
import com.debtlens.backend.exception.ResourceNotFoundException;
import com.debtlens.backend.integration.rabbitmq.AnalysisJobProducer;
import com.debtlens.backend.repository.Analysis_JobRepository;
import com.debtlens.backend.repository.Analysis_Status_HistoryRepository;
import com.debtlens.backend.repository.Class_MetricsRepository;
import com.debtlens.backend.repository.RepositoryRepository;
import com.debtlens.backend.security.Auth0UserService;
import com.debtlens.backend.service.AnalysisService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
public class AnalysisServiceImpl implements AnalysisService {

    private static final Logger log = LoggerFactory.getLogger(AnalysisServiceImpl.class);

    private final Analysis_JobRepository analysisJobRepository;
    private final Analysis_Status_HistoryRepository statusHistoryRepository;
    private final Class_MetricsRepository classMetricsRepository;
    private final com.debtlens.backend.repository.Class_CommentRepository classCommentRepository;
    private final RepositoryRepository repositoryRepository;
    private final AnalysisJobProducer analysisJobProducer;
    private final com.debtlens.backend.integration.rabbitmq.MLJobProducer mlJobProducer;
    private final Auth0UserService auth0UserService;

    public AnalysisServiceImpl(
            Analysis_JobRepository analysisJobRepository,
            Analysis_Status_HistoryRepository statusHistoryRepository,
            Class_MetricsRepository classMetricsRepository,
            com.debtlens.backend.repository.Class_CommentRepository classCommentRepository,
            RepositoryRepository repositoryRepository,
            AnalysisJobProducer analysisJobProducer,
            com.debtlens.backend.integration.rabbitmq.MLJobProducer mlJobProducer,
            Auth0UserService auth0UserService
    ) {
        this.analysisJobRepository = analysisJobRepository;
        this.statusHistoryRepository = statusHistoryRepository;
        this.classMetricsRepository = classMetricsRepository;
        this.classCommentRepository = classCommentRepository;
        this.repositoryRepository = repositoryRepository;
        this.analysisJobProducer = analysisJobProducer;
        this.mlJobProducer = mlJobProducer;
        this.auth0UserService = auth0UserService;
    }

    @Override
    @Transactional
    public AnalysisResponseDTO startAnalysis(Long repositoryId, String branch) {
        if (repositoryId == null) {
            throw new BadRequestException("Repository ID must not be null");
        }

        User currentUser = auth0UserService.getAuthenticatedUser();

        Repository repository = repositoryRepository.findById(repositoryId)
                .orElseThrow(() -> new ResourceNotFoundException("Repository with ID " + repositoryId + " not found"));

        String targetBranch = (branch != null && !branch.isBlank())
                ? branch.trim()
                : (repository.getDefaultBranch() != null ? repository.getDefaultBranch() : "main");

        // 1. Create and persist Analysis_Job
        Analysis_Job job = new Analysis_Job();
        job.setRepository(repository);
        job.setStartedBy(currentUser);
        job.setStatus(AnalysisJobStatus.QUEUED);
        job.setStartedAt(LocalDateTime.now());

        Analysis_Job savedJob = analysisJobRepository.save(job);

        // 2. Record initial status history
        Analysis_Status_History history = new Analysis_Status_History();
        history.setAnalysisJob(savedJob);
        history.setStatus(AnalysisJobStatus.QUEUED);
        history.setMessage("Analysis job queued for repository '" + repository.getRepositoryName() + "' on branch '" + targetBranch + "'.");
        history.setTimestamp(LocalDateTime.now());
        statusHistoryRepository.save(history);

        // 3. Publish to RabbitMQ
        AnalysisJobMessage jobMessage = AnalysisJobMessage.builder()
                .jobId(String.valueOf(savedJob.getAnalysisId()))
                .repositoryId(String.valueOf(repository.getRepositoryId()))
                .repositoryUrl(repository.getRepositoryUrl())
                .branch(targetBranch)
                .build();

        analysisJobProducer.publishAnalysisJob(jobMessage);

        log.info("Analysis job #{} created and dispatched to RabbitMQ for repo {}", savedJob.getAnalysisId(), repository.getRepositoryName());

        return mapToResponseDTO(savedJob, targetBranch, 0);
    }

    @Override
    @Transactional(readOnly = true)
    public AnalysisResponseDTO getAnalysisJob(Long analysisId) {
        Analysis_Job job = analysisJobRepository.findById(analysisId)
                .orElseThrow(() -> new ResourceNotFoundException("Analysis job with ID " + analysisId + " not found"));

        List<Class_Metrics> metrics = classMetricsRepository
                .findByAnalysisJobAnalysisIdOrderByFilePathAscStartLineAscClassNameAsc(analysisId);

        String branch = job.getRepository() != null ? job.getRepository().getDefaultBranch() : "main";
        return mapToResponseDTO(job, branch, metrics.size());
    }

    @Override
    @Transactional(readOnly = true)
    public List<AnalysisResponseDTO> getRepositoryAnalysisHistory(Long repositoryId) {
        if (!repositoryRepository.existsById(repositoryId)) {
            throw new ResourceNotFoundException("Repository with ID " + repositoryId + " not found");
        }

        List<Analysis_Job> jobs = analysisJobRepository.findByRepositoryRepositoryIdOrderByStartedAtDesc(repositoryId);
        return jobs.stream()
                .map(job -> {
                    String branch = job.getRepository() != null ? job.getRepository().getDefaultBranch() : "main";
                    int count = classMetricsRepository.countByAnalysisJobAnalysisId(job.getAnalysisId());
                    return mapToResponseDTO(job, branch, count);
                })
                .toList();
    }

    @Override
    @Transactional
    public void processAnalysisResult(AnalysisResultDTO result) {
        log.info("Processing analysis result for jobId: {}, status: {}", result.getJobId(), result.getStatus());

        if (result.getJobId() == null) {
            log.error("Received analysis result without jobId");
            return;
        }

        Long analysisId;
        try {
            analysisId = Long.parseLong(result.getJobId());
        } catch (NumberFormatException e) {
            log.error("Invalid jobId in analysis result: {}", result.getJobId());
            return;
        }

        Analysis_Job job = analysisJobRepository.findById(analysisId).orElse(null);
        if (job == null) {
            log.error("Analysis job #{} not found in database for result processing", analysisId);
            return;
        }

        boolean isSuccess = "SUCCESS".equalsIgnoreCase(result.getStatus());
        LocalDateTime now = LocalDateTime.now();

        if (isSuccess && result.getRepositoryMetrics() != null) {
            job.setStatus(AnalysisJobStatus.RUNNING);
            analysisJobRepository.save(job);

            // Persist class metrics
            List<ClassMetricsDTO> incomingMetrics = result.getRepositoryMetrics().getClassMetrics();
            int totalSaved = 0;
            if (incomingMetrics != null && !incomingMetrics.isEmpty()) {
                List<Class_Metrics> entitiesToSave = new ArrayList<>();
                for (ClassMetricsDTO dto : incomingMetrics) {
                    Class_Metrics entity = new Class_Metrics();
                    entity.setAnalysisJob(job);
                    entity.setClassName(dto.getClassName() != null ? dto.getClassName() : "Unknown");
                    entity.setFilePath(dto.getFilePath() != null ? dto.getFilePath() : "");
                    entity.setStartLine(dto.getStartLine());
                    entity.setEndLine(dto.getEndLine());
                    entity.setNumberOfLinesOfCode(dto.getNumberOfLinesOfCode());
                    entity.setDit(dto.getDit());
                    entity.setCbo(dto.getCbo());
                    entity.setFanIn(dto.getFanin());
                    entity.setFanOut(dto.getFanout());
                    entity.setLcom(dto.getLcom());
                    entity.setNoc(dto.getNoc());
                    entity.setRfc(dto.getRfc());
                    entity.setWmc(dto.getWmc());
                    entity.setNumberOfAttributes(dto.getNumberOfAttributes());
                    entity.setNumberOfMethods(dto.getNumberOfMethods());
                    entity.setNumberOfPrivateAttributes(dto.getNumberOfPrivateAttributes());
                    entity.setNumberOfPrivateMethods(dto.getNumberOfPrivateMethods());
                    entity.setNumberOfPublicAttributes(dto.getNumberOfPublicAttributes());
                    entity.setNumberOfPublicMethods(dto.getNumberOfPublicMethods());

                    // Git history metrics
                    entity.setNumberOfVersionsUntil(dto.getNumberOfVersionsUntil());
                    entity.setNumberOfAuthorsUntil(dto.getNumberOfAuthorsUntil());
                    entity.setLinesAddedUntil(dto.getLinesAddedUntil());
                    entity.setMaxLinesAddedUntil(dto.getMaxLinesAddedUntil());
                    entity.setAvgLinesAddedUntil(dto.getAvgLinesAddedUntil());
                    entity.setLinesRemovedUntil(dto.getLinesRemovedUntil());
                    entity.setMaxLinesRemovedUntil(dto.getMaxLinesRemovedUntil());
                    entity.setAvgLinesRemovedUntil(dto.getAvgLinesRemovedUntil());
                    entity.setCodeChurnUntil(dto.getCodeChurnUntil());
                    entity.setMaxCodeChurnUntil(dto.getMaxCodeChurnUntil());
                    entity.setAvgCodeChurnUntil(dto.getAvgCodeChurnUntil());
                    entity.setAgeWithRespectTo(dto.getAgeWithRespectTo());
                    entity.setWeightedAgeWithRespectTo(dto.getWeightedAgeWithRespectTo());

                    entitiesToSave.add(entity);
                }
                List<Class_Metrics> savedMetrics = classMetricsRepository.saveAll(entitiesToSave);
                totalSaved = savedMetrics.size();

                // Persist extracted comments for each class
                List<Class_Comment> commentsToSave = new ArrayList<>();
                for (int i = 0; i < incomingMetrics.size() && i < savedMetrics.size(); i++) {
                    ClassMetricsDTO dto = incomingMetrics.get(i);
                    Class_Metrics savedClassMetric = savedMetrics.get(i);
                    if (dto.getComments() != null && !dto.getComments().isEmpty()) {
                        for (String commentText : dto.getComments()) {
                            if (commentText != null && !commentText.isBlank()) {
                                Class_Comment commentEntity = new Class_Comment();
                                commentEntity.setClassMetrics(savedClassMetric);
                                commentEntity.setComment(commentText);
                                commentsToSave.add(commentEntity);
                            }
                        }
                    }
                }
                List<Class_Comment> savedComments = new ArrayList<>();
                if (!commentsToSave.isEmpty()) {
                    savedComments = classCommentRepository.saveAll(commentsToSave);
                    log.info("Saved {} comments for analysis job #{}", savedComments.size(), analysisId);
                }

                // Construct and publish ML prediction job to RabbitMQ
                List<MLClassMetricDTO> mlClasses = new ArrayList<>();
                for (int i = 0; i < incomingMetrics.size() && i < savedMetrics.size(); i++) {
                    ClassMetricsDTO dto = incomingMetrics.get(i);
                    Class_Metrics savedClassMetric = savedMetrics.get(i);

                    List<MLCommentDTO> classComments = new ArrayList<>();
                    for (Class_Comment savedComment : savedComments) {
                        if (savedComment.getClassMetrics() != null
                                && savedComment.getClassMetrics().getClassId() != null
                                && savedComment.getClassMetrics().getClassId().equals(savedClassMetric.getClassId())) {
                            classComments.add(new MLCommentDTO(savedComment.getCommentId(), savedComment.getComment()));
                        }
                    }

                    MLClassMetricDTO mlClass = MLClassMetricDTO.builder()
                            .classId(savedClassMetric.getClassId())
                            .className(savedClassMetric.getClassName())
                            .filePath(savedClassMetric.getFilePath())
                            .startLine(savedClassMetric.getStartLine())
                            .endLine(savedClassMetric.getEndLine())
                            .numberOfLinesOfCode(savedClassMetric.getNumberOfLinesOfCode())
                            .dit(savedClassMetric.getDit())
                            .cbo(savedClassMetric.getCbo())
                            .fanIn(savedClassMetric.getFanIn())
                            .fanOut(savedClassMetric.getFanOut())
                            .lcom(savedClassMetric.getLcom())
                            .noc(savedClassMetric.getNoc())
                            .rfc(savedClassMetric.getRfc())
                            .wmc(savedClassMetric.getWmc())
                            .numberOfAttributes(savedClassMetric.getNumberOfAttributes())
                            .numberOfMethods(savedClassMetric.getNumberOfMethods())
                            .numberOfPrivateAttributes(savedClassMetric.getNumberOfPrivateAttributes())
                            .numberOfPrivateMethods(savedClassMetric.getNumberOfPrivateMethods())
                            .numberOfPublicAttributes(savedClassMetric.getNumberOfPublicAttributes())
                            .numberOfPublicMethods(savedClassMetric.getNumberOfPublicMethods())
                            .numberOfVersionsUntil(savedClassMetric.getNumberOfVersionsUntil())
                            .numberOfAuthorsUntil(savedClassMetric.getNumberOfAuthorsUntil())
                            .linesAddedUntil(savedClassMetric.getLinesAddedUntil())
                            .maxLinesAddedUntil(savedClassMetric.getMaxLinesAddedUntil())
                            .avgLinesAddedUntil(savedClassMetric.getAvgLinesAddedUntil())
                            .linesRemovedUntil(savedClassMetric.getLinesRemovedUntil())
                            .maxLinesRemovedUntil(savedClassMetric.getMaxLinesRemovedUntil())
                            .avgLinesRemovedUntil(savedClassMetric.getAvgLinesRemovedUntil())
                            .codeChurnUntil(savedClassMetric.getCodeChurnUntil())
                            .maxCodeChurnUntil(savedClassMetric.getMaxCodeChurnUntil())
                            .avgCodeChurnUntil(savedClassMetric.getAvgCodeChurnUntil())
                            .ageWithRespectTo(savedClassMetric.getAgeWithRespectTo())
                            .weightedAgeWithRespectTo(savedClassMetric.getWeightedAgeWithRespectTo())
                            .comments(classComments)
                            .build();

                    mlClasses.add(mlClass);
                }

                MLJobMessage mlJobMessage = MLJobMessage.builder()
                        .jobId(String.valueOf(analysisId))
                        .repositoryId(job.getRepository() != null ? String.valueOf(job.getRepository().getRepositoryId()) : null)
                        .classes(mlClasses)
                        .build();

                mlJobProducer.publishMLJob(mlJobMessage);
            }

            Analysis_Status_History history = new Analysis_Status_History();
            history.setAnalysisJob(job);
            history.setStatus(AnalysisJobStatus.COMPLETED);
            history.setMessage("Analysis completed successfully. Processed " + totalSaved + " classes.");
            history.setTimestamp(now);
            statusHistoryRepository.save(history);

            log.info("Analysis job #{} completed successfully with {} classes saved", analysisId, totalSaved);
        } else {
            job.setStatus(AnalysisJobStatus.FAILED);
            job.setCompletedAt(now);
            analysisJobRepository.save(job);

            String errorMessage = result.getError() != null ? result.getError() : "Analysis execution failed.";
            Analysis_Status_History history = new Analysis_Status_History();
            history.setAnalysisJob(job);
            history.setStatus(AnalysisJobStatus.FAILED);
            history.setMessage("Analysis failed: " + errorMessage);
            history.setTimestamp(now);
            statusHistoryRepository.save(history);

            log.warn("Analysis job #{} failed: {}", analysisId, errorMessage);
        }
    }

    private AnalysisResponseDTO mapToResponseDTO(Analysis_Job job, String branch, Integer totalClasses) {
        String userName = null;
        Long userId = null;
        if (job.getStartedBy() != null) {
            userId = job.getStartedBy().getUserId();
            userName = (job.getStartedBy().getFirstName() != null ? job.getStartedBy().getFirstName() + " " : "")
                    + (job.getStartedBy().getLastName() != null ? job.getStartedBy().getLastName() : "");
            if (userName.isBlank()) {
                userName = job.getStartedBy().getGithubUsername();
            }
        }

        Repository repo = job.getRepository();
        Company company = repo != null ? repo.getCompany() : null;

        return new AnalysisResponseDTO(
                job.getAnalysisId(),
                repo != null ? repo.getRepositoryId() : null,
                repo != null ? repo.getRepositoryName() : null,
                repo != null ? repo.getRepositoryUrl() : null,
                company != null ? company.getCompanyId() : null,
                company != null ? company.getCompanyName() : null,
                branch,
                userId,
                userName != null ? userName.trim() : null,
                job.getStatus(),
                job.getStartedAt(),
                job.getCompletedAt(),
                totalClasses
        );
    }
}