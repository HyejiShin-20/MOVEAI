package com.moveai.dataset;

import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.stereotype.Component;

import com.moveai.dataset.service.DatasetImportService;
import com.moveai.dataset.service.SchemaInitializer;

/**
 * {@code --import-datasets} 로 실행하면 DDL 적용 + 데이터셋 임포트만 하고 종료한다.
 *
 * <pre>
 * ./gradlew bootRun --args="--import-datasets"
 * </pre>
 *
 * <p>Phase 2a 는 API 없이 SQL 로 건수를 확인하는 단계다 (05C §7). 서버를 계속 띄워 두면
 * 임포트 실패와 기동 실패가 섞여 원인이 흐려지므로, 임포트만 하고 프로세스를 닫는다.
 */
@Component
public class DatasetImportRunner implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(DatasetImportRunner.class);
    private static final String IMPORT_OPTION = "import-datasets";

    private final SchemaInitializer schemaInitializer;
    private final DatasetImportService datasetImportService;
    private final ConfigurableApplicationContext applicationContext;

    public DatasetImportRunner(
            SchemaInitializer schemaInitializer,
            DatasetImportService datasetImportService,
            ConfigurableApplicationContext applicationContext) {
        this.schemaInitializer = schemaInitializer;
        this.datasetImportService = datasetImportService;
        this.applicationContext = applicationContext;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (!args.containsOption(IMPORT_OPTION)) {
            return;
        }
        int exitCode = 0;
        try {
            schemaInitializer.apply();
            Map<String, Integer> counts = datasetImportService.importAll();
            counts.forEach((table, count) -> log.info("  {} {}", pad(table), count));
        } catch (RuntimeException exception) {
            log.error("dataset import failed: {}", exception.getMessage(), exception);
            exitCode = 1;
        }
        System.exit(SpringApplication.exit(applicationContext, () -> 0) + exitCode);
    }

    private static String pad(String table) {
        return String.format("%-22s", table);
    }
}
