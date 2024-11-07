import org.springframework.batch.item.file.FlatFileHeaderCallback;
import org.springframework.batch.item.file.FlatFileItemWriter;
import org.springframework.batch.item.file.transform.BeanWrapperFieldExtractor;
import org.springframework.batch.item.file.transform.DelimitedLineAggregator;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.core.io.FileSystemResource;
import org.springframework.stereotype.Component;
import org.springframework.batch.core.configuration.annotation.StepScope;

import java.io.IOException;
import java.io.Writer;
import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.stream.Collectors;

@Component
public class DynamicFileWriterConfig {

    @Bean
    @StepScope
    public FlatFileItemWriter<Film> fileWriter() {
        FlatFileItemWriter<Film> writer = new FlatFileItemWriter<>();
        writer.setResource(new FileSystemResource("data/output.csv"));
        
        // Set dynamic header
        writer.setHeaderCallback(new FlatFileHeaderCallback() {
            @Override
            public void writeHeader(Writer writer) throws IOException {
                String header = Arrays.stream(Film.class.getDeclaredFields())
                        .map(Field::getName)
                        .collect(Collectors.joining(", "));
                writer.write(header);
            }
        });
        
        // Set dynamic fields for line aggregator
        DelimitedLineAggregator<Film> lineAggregator = new DelimitedLineAggregator<>();
        lineAggregator.setDelimiter(",");
        
        BeanWrapperFieldExtractor<Film> fieldExtractor = new BeanWrapperFieldExtractor<>();
        String[] fieldNames = Arrays.stream(Film.class.getDeclaredFields())
                .map(Field::getName)
                .toArray(String[]::new);
        fieldExtractor.setNames(fieldNames);
        
        lineAggregator.setFieldExtractor(fieldExtractor);
        writer.setLineAggregator(lineAggregator);
        
        return writer;
    }
}
