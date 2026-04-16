package org.example.springbatch.batch;

import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.example.springbatch.entity.BeforeEntity;
import org.springframework.batch.infrastructure.item.Chunk;
import org.springframework.batch.infrastructure.item.ExecutionContext;
import org.springframework.batch.infrastructure.item.ItemStreamException;
import org.springframework.batch.infrastructure.item.ItemStreamWriter;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

public class ExcelRowWriter implements ItemStreamWriter<BeforeEntity> {

    private final Resource resource;
    private Workbook workbook;
    private Sheet sheet;
    private int currentRowNumber;
    private boolean isClosed;

    public ExcelRowWriter(String filePath) throws IOException {
        if (filePath.startsWith("classpath:")) {
            String relativePath = filePath.substring("classpath:".length());
            String dir = relativePath.contains("/") ? relativePath.substring(0, relativePath.lastIndexOf("/")) : "";
            String fileName = relativePath.contains("/") ? relativePath.substring(relativePath.lastIndexOf("/") + 1) : relativePath;
            File dirFile = new ClassPathResource(dir).getFile();
            this.resource = new FileSystemResource(new File(dirFile, fileName));
        } else {
            this.resource = new FileSystemResource(filePath);
        }
        this.isClosed = false;
        this.currentRowNumber = 0;
    }

    @Override
    public void write(Chunk<? extends BeforeEntity> chunk) throws Exception {
        for (BeforeEntity entity : chunk) {
            Row row = sheet.createRow(currentRowNumber++);
            row.createCell(0).setCellValue(entity.getUsername());
        }
    }

    @Override
    public void open(ExecutionContext executionContext) throws ItemStreamException {
        workbook = new XSSFWorkbook();
        sheet = workbook.createSheet("Sheet1");
    }

    @Override
    public void close() throws ItemStreamException {
        if (isClosed) {
            return;
        }

        try (FileOutputStream fileOut = new FileOutputStream(resource.getFile())) {
            workbook.write(fileOut);
        } catch (IOException e) {
            throw new ItemStreamException(e);
        } finally {
            try {
                workbook.close();
            } catch (IOException e) {
                throw new ItemStreamException(e);
            } finally {
                isClosed = true;
            }
        }
    }
}
