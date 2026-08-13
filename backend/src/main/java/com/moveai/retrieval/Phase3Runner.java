package com.moveai.retrieval;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import com.moveai.dataset.service.SchemaInitializer;
import com.moveai.knowledge.embedding.KnowledgeEmbeddingImportService;

/** Phase 3 운영 명령: --import-embeddings 또는 --evaluate-rag. */
@Component
@Order(Ordered.LOWEST_PRECEDENCE)
public class Phase3Runner implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(Phase3Runner.class);
    private final SchemaInitializer schemaInitializer;
    private final KnowledgeEmbeddingImportService importService;
    private final RagEvaluationService evaluationService;
    private final ConfigurableApplicationContext applicationContext;

    public Phase3Runner(
            SchemaInitializer schemaInitializer,
            KnowledgeEmbeddingImportService importService,
            RagEvaluationService evaluationService,
            ConfigurableApplicationContext applicationContext) {
        this.schemaInitializer = schemaInitializer;
        this.importService = importService;
        this.evaluationService = evaluationService;
        this.applicationContext = applicationContext;
    }

    @Override
    public void run(ApplicationArguments args) {
        boolean importEmbeddings = args.containsOption("import-embeddings");
        boolean evaluateRag = args.containsOption("evaluate-rag");
        if (!importEmbeddings && !evaluateRag) {
            return;
        }
        int exitCode = 0;
        try {
            schemaInitializer.apply();
            if (importEmbeddings) {
                log.info("knowledge_embeddings imported: {}", importService.importAll());
            }
            if (evaluateRag) {
                RagEvaluationService.EvaluationReport report = evaluationService.evaluate();
                report.results().forEach(result -> log.info(
                        "{} hit@3={} top5={} expected={} violations={}",
                        result.queryCode(), result.hitAt3(), result.top5(),
                        result.expectedCodes(), result.mustNotViolations()));
                log.info("RAG EVAL queries={} Hit@3={}/{} ({}) Hit@5={}/{} ({}) mustNot={}",
                        report.queryCount(), report.hitAt3Count(), report.queryCount(), report.hitAt3(),
                        report.hitAt5Count(), report.queryCount(), report.hitAt5(),
                        report.mustNotViolationCount());
            }
        } catch (RuntimeException exception) {
            log.error("Phase 3 command failed: {}", exception.getMessage(), exception);
            exitCode = 1;
        }
        System.exit(SpringApplication.exit(applicationContext, () -> 0) + exitCode);
    }
}
