import org.springframework.batch.item.file.FlatFileHeaderCallback;
import org.springframework.batch.item.file.FlatFileItemWriter;
import org.springframework.batch.item.file.transform.BeanWrapperFieldExtractor;
import org.springframework.batch.item.file.transform.DelimitedLineAggregator;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.core.io.FileSystemResource;
import org.springframework.stereotype.Component;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.beans.BeanWrapper;
import org.springframework.beans.PropertyAccessorFactory;

import java.io.IOException;
import java.io.Writer;
import java.util.Arrays;
import java.util.stream.Collectors;

@Component
public class DynamicFileWriterConfig {

    @Bean
    @StepScope
    public <T> FlatFileItemWriter<T> fileWriter(@Value("#{jobParameters['entityClass']}") Class<T> entityClass) {
        FlatFileItemWriter<T> writer = new FlatFileItemWriter<>();
        writer.setResource(new FileSystemResource("data/output.csv"));

        // Extract field names dynamically for the given entity class
        BeanWrapper wrapper = PropertyAccessorFactory.forBeanPropertyAccess(entityClass);
        String[] fieldNames = Arrays.stream(wrapper.getPropertyDescriptors())
                                    .map(descriptor -> descriptor.getName())
                                    .filter(name -> !"class".equals(name))
                                    .toArray(String[]::new);

        // Set dynamic header
        writer.setHeaderCallback(new FlatFileHeaderCallback() {
            @Override
            public void writeHeader(Writer writer) throws IOException {
                String header = String.join(", ", fieldNames);
                writer.write(header);
            }
        });

        // Set field extractor dynamically
        DelimitedLineAggregator<T> lineAggregator = new DelimitedLineAggregator<>();
        lineAggregator.setDelimiter(",");

        BeanWrapperFieldExtractor<T> fieldExtractor = new BeanWrapperFieldExtractor<>();
        fieldExtractor.setNames(fieldNames);

        lineAggregator.setFieldExtractor(fieldExtractor);
        writer.setLineAggregator(lineAggregator);

        return writer;
    }
}
