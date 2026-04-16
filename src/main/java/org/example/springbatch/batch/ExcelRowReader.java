package org.example.springbatch.batch;

import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.jspecify.annotations.Nullable;
import org.springframework.batch.infrastructure.item.ExecutionContext;
import org.springframework.batch.infrastructure.item.ItemStreamException;
import org.springframework.batch.infrastructure.item.ItemStreamReader;

import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;

import java.io.IOException;
import java.io.InputStream;
import java.util.Iterator;

public class ExcelRowReader implements ItemStreamReader<Row> {

    private final Resource resource;
    private InputStream fileInputStream;
    private Workbook workbook;
    private Iterator<Row> rowCursor;
    private int currentRowNumber;
    private final String CURRENT_ROW_KEY = "current.row.number";

    public ExcelRowReader(String filePath) throws IOException {
        if (filePath.startsWith("classpath:")) {
            this.resource = new ClassPathResource(filePath.substring("classpath:".length()));
        } else {
            this.resource = new FileSystemResource(filePath);
        }
        this.currentRowNumber = 0;
    }

    @Override
    public @Nullable Row read() throws Exception {
        if (rowCursor != null && rowCursor.hasNext()) {
            currentRowNumber++;
            return rowCursor.next();
        } else {
            return null;
        }
    }

    @Override
    public void open(ExecutionContext executionContext) throws ItemStreamException {
        try {
            fileInputStream = resource.getInputStream();
            workbook = WorkbookFactory.create(fileInputStream);
            Sheet sheet = workbook.getSheetAt(0);
            this.rowCursor = sheet.iterator();

            // "current.row.number" 존재시 초기화
            if (executionContext.containsKey(CURRENT_ROW_KEY)) {
                currentRowNumber = executionContext.getInt(CURRENT_ROW_KEY);
            }

            // 이미 실행한 부분은 건너 뜀
            for (int i = 0; i < currentRowNumber && rowCursor.hasNext(); i++) {
                rowCursor.next();
            }

        } catch (IOException e) {
            throw new ItemStreamException(e);
        }
    }

    @Override
    public void update(ExecutionContext executionContext) throws ItemStreamException {
        executionContext.putInt(CURRENT_ROW_KEY, currentRowNumber);
    }

    @Override
    public void close() throws ItemStreamException {
        try {
            if (workbook != null) {
                workbook.close();
            }
            if (fileInputStream != null) {
                fileInputStream.close();
            }
        } catch (IOException e) {
            throw new ItemStreamException(e);
        }
    }
}
