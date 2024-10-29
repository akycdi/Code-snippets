import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.EnableBatchProcessing;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.item.database.JdbcCursorItemReader;
import org.springframework.batch.item.file.FlatFileItemWriter;
import org.springframework.batch.item.file.transform.BeanWrapperFieldExtractor;
import org.springframework.batch.item.file.transform.DelimitedLineAggregator;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.FileSystemResource;
import org.springframework.jdbc.core.BeanPropertyRowMapper;
import org.springframework.transaction.PlatformTransactionManager;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

@Configuration
@EnableBatchProcessing
public class BatchConfig {

    @Autowired
    private javax.sql.DataSource dataSource;

    @Bean
    public Job exportTablesToCsvJob(JobRepository jobRepository, PlatformTransactionManager transactionManager) {
        return new JobBuilder("exportTablesToCsvJob", jobRepository)
                .start(exportTableStep(jobRepository, transactionManager))
                .next(zipCsvFilesStep(jobRepository, transactionManager))
                .build();
    }

    @Bean
    public Step exportTableStep(JobRepository jobRepository, PlatformTransactionManager transactionManager) {
        return new StepBuilder("exportTableStep", jobRepository)
                .<Entity, Entity>chunk(100, transactionManager)
                .reader(databaseReader())
                .processor(item -> item) // No processing, pass-through
                .writer(csvFileWriter())
                .build();
    }

    @Bean
    public Step zipCsvFilesStep(JobRepository jobRepository, PlatformTransactionManager transactionManager) {
        return new StepBuilder("zipCsvFilesStep", jobRepository)
                .tasklet(zipCsvFilesTasklet(), transactionManager)
                .build();
    }

    @Bean
    public JdbcCursorItemReader<Entity> databaseReader() {
        JdbcCursorItemReader<Entity> reader = new JdbcCursorItemReader<>();
        reader.setDataSource(dataSource);
        reader.setSql("SELECT * FROM table_name"); // Replace with your table name
        reader.setRowMapper(new BeanPropertyRowMapper<>(Entity.class));
        return reader;
    }

    @Bean
    public FlatFileItemWriter<Entity> csvFileWriter() {
        FlatFileItemWriter<Entity> writer = new FlatFileItemWriter<>();
        writer.setResource(new FileSystemResource("output/entity.csv"));
        writer.setHeaderCallback(writer1 -> writer1.write("id,name,description")); // Adjust columns as needed
        writer.setLineAggregator(new DelimitedLineAggregator<>() {{
            setDelimiter(",");
            setFieldExtractor(new BeanWrapperFieldExtractor<>() {{
                setNames(new String[]{"id", "name", "description"}); // Adjust fields as needed
            }});
        }});
        return writer;
    }

    @Bean
    public Tasklet zipCsvFilesTasklet() {
        return (contribution, chunkContext) -> {
            String outputFilePath = "output/entity.zip";
            try (FileOutputStream fos = new FileOutputStream(outputFilePath);
                 ZipOutputStream zipOut = new ZipOutputStream(fos)) {

                File fileToZip = new File("output/entity.csv");
                try (FileInputStream fis = new FileInputStream(fileToZip)) {
                    ZipEntry zipEntry = new ZipEntry(fileToZip.getName());
                    zipOut.putNextEntry(zipEntry);

                    byte[] bytes = new byte[1024];
                    int length;
                    while ((length = fis.read(bytes)) >= 0) {
                        zipOut.write(bytes, 0, length);
                    }
                }
            } catch (IOException e) {
                throw new RuntimeException("Failed to zip files", e);
            }
            return RepeatStatus.FINISHED;
        };
    }
}
