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
public class SimpleDynamicFileWriterConfig {

    @Bean
    @StepScope
    public FlatFileItemWriter<Film> fileWriter() {
        FlatFileItemWriter<Film> writer = new FlatFileItemWriter<>();
        writer.setResource(new FileSystemResource("data/output.csv"));

        // Create a sample instance to extract field names dynamically
        Film sample = new Film();
        BeanWrapper wrapper = PropertyAccessorFactory.forBeanPropertyAccess(sample);
        String[] fieldNames = wrapper.getPropertyDescriptors()
                                     .stream()
                                     .map(descriptor -> descriptor.getName())
                                     .filter(name -> !"class".equals(name)) // exclude 'class' property
                                     .toArray(String[]::new);

        // Set header using the extracted field names
        writer.setHeaderCallback(new FlatFileHeaderCallback() {
            @Override
            public void writeHeader(Writer writer) throws IOException {
                String header = String.join(", ", fieldNames);
                writer.write(header);
            }
        });

        // Set fields for line aggregator
        DelimitedLineAggregator<Film> lineAggregator = new DelimitedLineAggregator<>();
        lineAggregator.setDelimiter(",");
        
        BeanWrapperFieldExtractor<Film> fieldExtractor = new BeanWrapperFieldExtractor<>();
        fieldExtractor.setNames(fieldNames);
        
        lineAggregator.setFieldExtractor(fieldExtractor);
        writer.setLineAggregator(lineAggregator);

        return writer;
    }
}
