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
