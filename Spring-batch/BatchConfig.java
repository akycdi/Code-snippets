@Bean
public Step exportSalesStep(StepBuilderFactory stepBuilderFactory,
                            ItemReader<Sales> salesItemReader,
                            ItemWriter<Sales> salesItemWriter) {
    return stepBuilderFactory.get("exportSalesStep")
            .<Sales, Sales>chunk(10)  // Define the chunk size
            .reader(salesItemReader)
            .writer(salesItemWriter)
            .build();
}

@Bean
public FlatFileItemWriter<Sales> salesItemWriter() {
    FlatFileItemWriter<Sales> writer = new FlatFileItemWriter<>();
    writer.setResource(new FileSystemResource("sales.csv"));
    writer.setHeaderCallback(writer1 -> writer1.write("ID,Product,Quantity,Price"));
    
    writer.setLineAggregator(new DelimitedLineAggregator<Sales>() {{
        setDelimiter(",");
        setFieldExtractor(new BeanWrapperFieldExtractor<Sales>() {{
            setNames(new String[] {"id", "product", "quantity", "price"});
        }});
    }});
    
    return writer;
}


@Bean
public JpaPagingItemReader<Sales> salesItemReader(EntityManagerFactory entityManagerFactory) {
    JpaPagingItemReader<Sales> reader = new JpaPagingItemReader<>();
    reader.setEntityManagerFactory(entityManagerFactory);
    reader.setQueryString("SELECT s FROM Sales s");
    reader.setPageSize(10);  // Adjust page size as necessary
    return reader;
}
