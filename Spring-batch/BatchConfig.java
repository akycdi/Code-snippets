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

----------------------------------------------


@Bean
public JdbcCursorItemReader<Car> carItemReader(DataSource dataSource) {
    JdbcCursorItemReader<Car> reader = new JdbcCursorItemReader<>();
    reader.setDataSource(dataSource);
    reader.setSql("{CALL GET_TOTAL_CARS_BY_MODEL(?)}");
    reader.setPreparedStatementSetter(preparedStatement -> {
        preparedStatement.setString(1, "your_model_value"); // Set the model parameter here
    });
    reader.setRowMapper(new BeanPropertyRowMapper<>(Car.class)); // Assuming Car is the mapped entity

    return reader;
}

@Bean
public FlatFileItemWriter<Car> carItemWriter() {
    FlatFileItemWriter<Car> writer = new FlatFileItemWriter<>();
    writer.setResource(new FileSystemResource("cars.csv"));
    writer.setHeaderCallback(writer1 -> writer1.write("ID,Model,Count"));
    
    writer.setLineAggregator(new DelimitedLineAggregator<Car>() {{
        setDelimiter(",");
        setFieldExtractor(new BeanWrapperFieldExtractor<Car>() {{
            setNames(new String[] {"id", "model", "count"});
        }});
    }});

    return writer;
}


@Bean
public Step exportCarStep(StepBuilderFactory stepBuilderFactory,
                          ItemReader<Car> carItemReader,
                          ItemWriter<Car> carItemWriter) {
    return stepBuilderFactory.get("exportCarStep")
            .<Car, Car>chunk(10) // Define chunk size
            .reader(carItemReader)
            .writer(carItemWriter)
            .build();
}

@Bean
public Job exportCarJob(JobBuilderFactory jobBuilderFactory, Step exportCarStep) {
    return jobBuilderFactory.get("exportCarJob")
            .incrementer(new RunIdIncrementer())
            .flow(exportCarStep)
            .end()
            .build();
}
