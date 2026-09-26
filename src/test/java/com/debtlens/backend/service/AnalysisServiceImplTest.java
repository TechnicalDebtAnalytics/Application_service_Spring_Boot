package com.debtlens.backend.service;

import com.debtlens.backend.dto.messaging.AnalysisJobMessage;
import com.debtlens.backend.dto.messaging.AnalysisResultDTO;
import com.debtlens.backend.dto.messaging.ClassMetricsDTO;
import com.debtlens.backend.dto.messaging.MLJobMessage;
import com.debtlens.backend.dto.messaging.RepositoryMetricsDTO;
import com.debtlens.backend.dto.response.AnalysisResponseDTO;
import com.debtlens.backend.entity.AnalysisJobStatus;
import com.debtlens.backend.entity.Analysis_Status_History;
import com.debtlens.backend.entity.Analysis_Job;
import com.debtlens.backend.entity.Class_Comment;
import com.debtlens.backend.entity.Class_Metrics;
import com.debtlens.backend.entity.Company;
import com.debtlens.backend.entity.Repository;
import com.debtlens.backend.entity.User;
import com.debtlens.backend.exception.BadRequestException;
import com.debtlens.backend.exception.ResourceNotFoundException;
import com.debtlens.backend.integration.rabbitmq.AnalysisJobProducer;
import com.debtlens.backend.integration.rabbitmq.MLJobProducer;
import com.debtlens.backend.repository.Analysis_JobRepository;
import com.debtlens.backend.repository.Analysis_Status_HistoryRepository;
import com.debtlens.backend.repository.Class_CommentRepository;
import com.debtlens.backend.repository.Class_MetricsRepository;
import com.debtlens.backend.repository.RepositoryRepository;
import com.debtlens.backend.security.Auth0UserService;
import com.debtlens.backend.service.impl.AnalysisServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.ArgumentCaptor;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class AnalysisServiceImplTest {

    private Analysis_JobRepository analysisJobRepository;
    private Analysis_Status_HistoryRepository statusHistoryRepository;
    private Class_MetricsRepository classMetricsRepository;
    private Class_CommentRepository classCommentRepository;
    private RepositoryRepository repositoryRepository;
    private AnalysisJobProducer analysisJobProducer;
    private MLJobProducer mlJobProducer;
    private Auth0UserService auth0UserService;
    private AnalysisServiceImpl service;

    @BeforeEach
    void setUp() {
        analysisJobRepository = mock(Analysis_JobRepository.class);
        statusHistoryRepository = mock(Analysis_Status_HistoryRepository.class);
        classMetricsRepository = mock(Class_MetricsRepository.class);
        classCommentRepository = mock(Class_CommentRepository.class);
        repositoryRepository = mock(RepositoryRepository.class);
        analysisJobProducer = mock(AnalysisJobProducer.class);
        mlJobProducer = mock(MLJobProducer.class);
        auth0UserService = mock(Auth0UserService.class);
        service = new AnalysisServiceImpl(
                analysisJobRepository,
                statusHistoryRepository,
                classMetricsRepository,
                classCommentRepository,
                repositoryRepository,
                analysisJobProducer,
                mlJobProducer,
                auth0UserService
        );
    }

    @Test
    void startAnalysis_shouldPersistQueuedJobAndHistoryAndPublishTrimmedBranch() {
        Repository repository = repository(42L, "debt-lens", "https://example.test/debt-lens.git", "main");
        User user = user(7L, "Ada", "Lovelace", "ada");
        when(auth0UserService.getAuthenticatedUser()).thenReturn(user);
        when(repositoryRepository.findById(42L)).thenReturn(Optional.of(repository));
        when(analysisJobRepository.save(any(Analysis_Job.class))).thenAnswer(invocation -> {
            Analysis_Job job = invocation.getArgument(0);
            job.setAnalysisId(100L);
            return job;
        });

        AnalysisResponseDTO response = service.startAnalysis(42L, "  feature/unit-tests  ");

        ArgumentCaptor<Analysis_Job> jobCaptor = ArgumentCaptor.forClass(Analysis_Job.class);
        verify(analysisJobRepository).save(jobCaptor.capture());
        Analysis_Job savedJob = jobCaptor.getValue();
        assertEquals(repository, savedJob.getRepository());
        assertEquals(user, savedJob.getStartedBy());
        assertEquals(AnalysisJobStatus.QUEUED, savedJob.getStatus());
        assertNotNull(savedJob.getStartedAt());

        ArgumentCaptor<Analysis_Status_History> historyCaptor = ArgumentCaptor.forClass(Analysis_Status_History.class);
        verify(statusHistoryRepository).save(historyCaptor.capture());
        assertEquals(savedJob, historyCaptor.getValue().getAnalysisJob());
        assertEquals(AnalysisJobStatus.QUEUED, historyCaptor.getValue().getStatus());
        assertTrue(historyCaptor.getValue().getMessage().contains("feature/unit-tests"));

        ArgumentCaptor<AnalysisJobMessage> messageCaptor = ArgumentCaptor.forClass(AnalysisJobMessage.class);
        verify(analysisJobProducer).publishAnalysisJob(messageCaptor.capture());
        assertEquals("100", messageCaptor.getValue().getJobId());
        assertEquals("42", messageCaptor.getValue().getRepositoryId());
        assertEquals(repository.getRepositoryUrl(), messageCaptor.getValue().getRepositoryUrl());
        assertEquals("feature/unit-tests", messageCaptor.getValue().getBranch());

        assertEquals(100L, response.analysisId());
        assertEquals(AnalysisJobStatus.QUEUED, response.status());
        assertEquals("feature/unit-tests", response.branch());
        assertEquals("Ada Lovelace", response.startedByName());
        assertEquals(0, response.totalClassesAnalyzed());
    }

    @ParameterizedTest
    @CsvSource(value = {"<null>,develop,develop", "'   ',develop,develop", "<null>,<null>,main"}, nullValues = "<null>")
    void startAnalysis_shouldChooseDefaultOrMainBranch(String requestedBranch, String defaultBranch, String expectedBranch) {
        Repository repository = repository(42L, "debt-lens", "https://example.test/debt-lens.git", defaultBranch);
        when(auth0UserService.getAuthenticatedUser()).thenReturn(user(7L, "Ada", "Lovelace", "ada"));
        when(repositoryRepository.findById(42L)).thenReturn(Optional.of(repository));
        when(analysisJobRepository.save(any(Analysis_Job.class))).thenAnswer(invocation -> {
            Analysis_Job job = invocation.getArgument(0);
            job.setAnalysisId(100L);
            return job;
        });

        AnalysisResponseDTO response = service.startAnalysis(42L, requestedBranch);

        ArgumentCaptor<AnalysisJobMessage> messageCaptor = ArgumentCaptor.forClass(AnalysisJobMessage.class);
        verify(analysisJobProducer).publishAnalysisJob(messageCaptor.capture());
        assertEquals(expectedBranch, messageCaptor.getValue().getBranch());
        assertEquals(expectedBranch, response.branch());
    }

    @Test
    void startAnalysis_shouldRejectNullRepositoryIdWithoutCallingDependencies() {
        BadRequestException exception = assertThrows(BadRequestException.class, () -> service.startAnalysis(null, "main"));

        assertEquals("Repository ID must not be null", exception.getMessage());
        verifyNoInteractions(auth0UserService, repositoryRepository, analysisJobRepository,
                statusHistoryRepository, analysisJobProducer);
    }

    @Test
    void startAnalysis_shouldFailWhenRepositoryDoesNotExistWithoutPersistingOrPublishing() {
        when(auth0UserService.getAuthenticatedUser()).thenReturn(user(7L, "Ada", "Lovelace", "ada"));
        when(repositoryRepository.findById(404L)).thenReturn(Optional.empty());

        ResourceNotFoundException exception = assertThrows(
                ResourceNotFoundException.class,
                () -> service.startAnalysis(404L, "main")
        );

        assertEquals("Repository with ID 404 not found", exception.getMessage());
        verify(analysisJobRepository, never()).save(any());
        verifyNoInteractions(statusHistoryRepository, analysisJobProducer);
    }

    @Test
    void getAnalysisJob_shouldReturnMappedJobAndMetricCount() {
        Analysis_Job job = job(100L, AnalysisJobStatus.RUNNING, repository(42L, "debt-lens", "url", "develop"));
        job.setStartedBy(user(7L, null, null, "ada"));
        when(analysisJobRepository.findById(100L)).thenReturn(Optional.of(job));
        when(classMetricsRepository.findByAnalysisJobAnalysisIdOrderByFilePathAscStartLineAscClassNameAsc(100L))
                .thenReturn(List.of(new Class_Metrics(), new Class_Metrics()));

        AnalysisResponseDTO response = service.getAnalysisJob(100L);

        assertEquals(100L, response.analysisId());
        assertEquals(42L, response.repositoryId());
        assertEquals("develop", response.branch());
        assertEquals("ada", response.startedByName());
        assertEquals(AnalysisJobStatus.RUNNING, response.status());
        assertEquals(2, response.totalClassesAnalyzed());
    }

    @Test
    void getAnalysisJob_shouldFailWhenJobDoesNotExistWithoutLoadingMetrics() {
        when(analysisJobRepository.findById(404L)).thenReturn(Optional.empty());

        ResourceNotFoundException exception = assertThrows(
                ResourceNotFoundException.class,
                () -> service.getAnalysisJob(404L)
        );

        assertEquals("Analysis job with ID 404 not found", exception.getMessage());
        verifyNoInteractions(classMetricsRepository);
    }

    @Test
    void getRepositoryAnalysisHistory_shouldPreserveRepositoryOrderAndCountEachJob() {
        Repository repository = repository(42L, "debt-lens", "url", "main");
        Analysis_Job newest = job(102L, AnalysisJobStatus.COMPLETED, repository);
        Analysis_Job oldest = job(101L, AnalysisJobStatus.FAILED, repository);
        when(repositoryRepository.existsById(42L)).thenReturn(true);
        when(analysisJobRepository.findByRepositoryRepositoryIdOrderByStartedAtDesc(42L))
                .thenReturn(List.of(newest, oldest));
        when(classMetricsRepository.countByAnalysisJobAnalysisId(102L)).thenReturn(3);
        when(classMetricsRepository.countByAnalysisJobAnalysisId(101L)).thenReturn(0);

        List<AnalysisResponseDTO> history = service.getRepositoryAnalysisHistory(42L);

        assertEquals(List.of(102L, 101L), history.stream().map(AnalysisResponseDTO::analysisId).toList());
        assertEquals(List.of(3, 0), history.stream().map(AnalysisResponseDTO::totalClassesAnalyzed).toList());
        verify(classMetricsRepository).countByAnalysisJobAnalysisId(102L);
        verify(classMetricsRepository).countByAnalysisJobAnalysisId(101L);
    }

    @Test
    void getRepositoryAnalysisHistory_shouldFailWhenRepositoryDoesNotExist() {
        when(repositoryRepository.existsById(404L)).thenReturn(false);

        ResourceNotFoundException exception = assertThrows(
                ResourceNotFoundException.class,
                () -> service.getRepositoryAnalysisHistory(404L)
        );

        assertEquals("Repository with ID 404 not found", exception.getMessage());
        verify(analysisJobRepository, never()).findByRepositoryRepositoryIdOrderByStartedAtDesc(any());
        verifyNoInteractions(classMetricsRepository);
    }

    @Test
    void processAnalysisResult_shouldPersistMetricsAndCommentsPublishMlJobAndRemainRunning() {
        Repository repository = repository(42L, "debt-lens", "url", "main");
        Analysis_Job job = job(100L, AnalysisJobStatus.QUEUED, repository);
        when(analysisJobRepository.findById(100L)).thenReturn(Optional.of(job));

        ClassMetricsDTO incoming = ClassMetricsDTO.builder()
                .className("DebtCalculator")
                .filePath("src/DebtCalculator.java")
                .startLine(10)
                .endLine(80)
                .numberOfLinesOfCode(71)
                .cbo(4)
                .comments(Arrays.asList("TODO reduce coupling", "  ", null))
                .build();
        AnalysisResultDTO result = successfulResult("100", List.of(incoming));

        when(classMetricsRepository.saveAll(anyList())).thenAnswer(invocation -> {
            List<Class_Metrics> metrics = invocation.getArgument(0);
            metrics.get(0).setClassId(500L);
            return metrics;
        });
        when(classCommentRepository.saveAll(anyList())).thenAnswer(invocation -> {
            List<Class_Comment> comments = invocation.getArgument(0);
            comments.get(0).setCommentId(600L);
            return comments;
        });
        List<AnalysisJobStatus> savedStatuses = new ArrayList<>();
        doAnswer(invocation -> {
            savedStatuses.add(invocation.<Analysis_Job>getArgument(0).getStatus());
            return invocation.getArgument(0);
        }).when(analysisJobRepository).save(any(Analysis_Job.class));

        service.processAnalysisResult(result);

        ArgumentCaptor<List<Class_Metrics>> metricsCaptor = ArgumentCaptor.forClass(List.class);
        verify(classMetricsRepository).saveAll(metricsCaptor.capture());
        Class_Metrics savedMetric = metricsCaptor.getValue().get(0);
        assertEquals(job, savedMetric.getAnalysisJob());
        assertEquals("DebtCalculator", savedMetric.getClassName());
        assertEquals("src/DebtCalculator.java", savedMetric.getFilePath());
        assertEquals(71, savedMetric.getNumberOfLinesOfCode());
        assertEquals(4, savedMetric.getCbo());

        ArgumentCaptor<List<Class_Comment>> commentsCaptor = ArgumentCaptor.forClass(List.class);
        verify(classCommentRepository).saveAll(commentsCaptor.capture());
        assertEquals(1, commentsCaptor.getValue().size());
        assertEquals("TODO reduce coupling", commentsCaptor.getValue().get(0).getComment());
        assertEquals(500L, commentsCaptor.getValue().get(0).getClassMetrics().getClassId());

        ArgumentCaptor<MLJobMessage> mlMessageCaptor = ArgumentCaptor.forClass(MLJobMessage.class);
        verify(mlJobProducer).publishMLJob(mlMessageCaptor.capture());
        assertEquals("100", mlMessageCaptor.getValue().getJobId());
        assertEquals("42", mlMessageCaptor.getValue().getRepositoryId());
        assertEquals(1, mlMessageCaptor.getValue().getClasses().size());
        assertEquals(500L, mlMessageCaptor.getValue().getClasses().get(0).getClassId());
        assertEquals(1, mlMessageCaptor.getValue().getClasses().get(0).getComments().size());
        assertEquals(600L, mlMessageCaptor.getValue().getClasses().get(0).getComments().get(0).getCommentId());

        ArgumentCaptor<Analysis_Status_History> historyCaptor = ArgumentCaptor.forClass(Analysis_Status_History.class);
        verify(statusHistoryRepository).save(historyCaptor.capture());
        assertEquals(AnalysisJobStatus.RUNNING, historyCaptor.getValue().getStatus());
        assertEquals("Static analysis completed and ML processing queued. Processed 1 classes.", historyCaptor.getValue().getMessage());
        assertEquals(List.of(AnalysisJobStatus.RUNNING), savedStatuses);
        assertEquals(AnalysisJobStatus.RUNNING, job.getStatus());
        assertNull(job.getCompletedAt());
    }

    @Test
    void processAnalysisResult_shouldCompleteSuccessfulJobWithNoClassMetricsWithoutPublishingMlJob() {
        Analysis_Job job = job(100L, AnalysisJobStatus.QUEUED, repository(42L, "debt-lens", "url", "main"));
        when(analysisJobRepository.findById(100L)).thenReturn(Optional.of(job));

        service.processAnalysisResult(successfulResult("100", List.of()));

        verify(classMetricsRepository, never()).saveAll(anyList());
        verifyNoInteractions(classCommentRepository, mlJobProducer);
        ArgumentCaptor<Analysis_Status_History> historyCaptor = ArgumentCaptor.forClass(Analysis_Status_History.class);
        verify(statusHistoryRepository).save(historyCaptor.capture());
        assertEquals(AnalysisJobStatus.COMPLETED, historyCaptor.getValue().getStatus());
        assertEquals("Analysis completed successfully. Processed 0 classes.", historyCaptor.getValue().getMessage());
        assertEquals(AnalysisJobStatus.COMPLETED, job.getStatus());
        assertNotNull(job.getCompletedAt());
        verify(analysisJobRepository, times(2)).save(job);
    }

    @Test
    void processAnalysisResult_shouldMarkFailedAndRecordProvidedError() {
        Analysis_Job job = job(100L, AnalysisJobStatus.RUNNING, repository(42L, "debt-lens", "url", "main"));
        when(analysisJobRepository.findById(100L)).thenReturn(Optional.of(job));
        AnalysisResultDTO result = AnalysisResultDTO.builder()
                .jobId("100")
                .status("FAILED")
                .error("checkout failed")
                .build();

        service.processAnalysisResult(result);

        assertEquals(AnalysisJobStatus.FAILED, job.getStatus());
        assertNotNull(job.getCompletedAt());
        verify(analysisJobRepository).save(job);
        ArgumentCaptor<Analysis_Status_History> historyCaptor = ArgumentCaptor.forClass(Analysis_Status_History.class);
        verify(statusHistoryRepository).save(historyCaptor.capture());
        assertEquals(AnalysisJobStatus.FAILED, historyCaptor.getValue().getStatus());
        assertEquals("Analysis failed: checkout failed", historyCaptor.getValue().getMessage());
        verifyNoInteractions(classMetricsRepository, classCommentRepository, mlJobProducer);
    }

    @Test
    void processAnalysisResult_shouldTreatSuccessWithoutRepositoryMetricsAsFailure() {
        Analysis_Job job = job(100L, AnalysisJobStatus.RUNNING, repository(42L, "debt-lens", "url", "main"));
        when(analysisJobRepository.findById(100L)).thenReturn(Optional.of(job));
        AnalysisResultDTO result = AnalysisResultDTO.builder().jobId("100").status("SUCCESS").build();

        service.processAnalysisResult(result);

        assertEquals(AnalysisJobStatus.FAILED, job.getStatus());
        assertNotNull(job.getCompletedAt());
        ArgumentCaptor<Analysis_Status_History> historyCaptor = ArgumentCaptor.forClass(Analysis_Status_History.class);
        verify(statusHistoryRepository).save(historyCaptor.capture());
        assertEquals("Analysis failed: Analysis execution failed.", historyCaptor.getValue().getMessage());
        verifyNoInteractions(classMetricsRepository, classCommentRepository, mlJobProducer);
    }

    @Test
    void processAnalysisResult_shouldIgnoreNullInvalidAndUnknownJobIds() {
        service.processAnalysisResult(AnalysisResultDTO.builder().status("SUCCESS").build());
        service.processAnalysisResult(AnalysisResultDTO.builder().jobId("not-a-number").status("SUCCESS").build());
        when(analysisJobRepository.findById(404L)).thenReturn(Optional.empty());
        service.processAnalysisResult(AnalysisResultDTO.builder().jobId("404").status("SUCCESS").build());

        verify(analysisJobRepository).findById(404L);
        verify(analysisJobRepository, never()).save(any());
        verifyNoInteractions(statusHistoryRepository, classMetricsRepository, classCommentRepository, mlJobProducer);
    }

    private static AnalysisResultDTO successfulResult(String jobId, List<ClassMetricsDTO> metrics) {
        return AnalysisResultDTO.builder()
                .jobId(jobId)
                .status("SUCCESS")
                .repositoryMetrics(RepositoryMetricsDTO.builder().classMetrics(metrics).build())
                .build();
    }

    private static Repository repository(Long id, String name, String url, String defaultBranch) {
        Company company = new Company();
        company.setCompanyId(9L);
        company.setCompanyName("DebtLens");
        Repository repository = new Repository();
        repository.setRepositoryId(id);
        repository.setRepositoryName(name);
        repository.setRepositoryUrl(url);
        repository.setDefaultBranch(defaultBranch);
        repository.setCompany(company);
        return repository;
    }

    private static User user(Long id, String firstName, String lastName, String githubUsername) {
        User user = new User();
        user.setUserId(id);
        user.setFirstName(firstName);
        user.setLastName(lastName);
        user.setGithubUsername(githubUsername);
        return user;
    }

    private static Analysis_Job job(Long id, AnalysisJobStatus status, Repository repository) {
        Analysis_Job job = new Analysis_Job();
        job.setAnalysisId(id);
        job.setStatus(status);
        job.setRepository(repository);
        job.setStartedAt(LocalDateTime.now().minusMinutes(5));
        return job;
    }
}
