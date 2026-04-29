import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.FileInputStream;
import java.io.IOException;

public class ExcelGraphLoader {

    public static Graph loadGraphFromExcel(String filePath) throws IOException {
        Graph graph = new Graph();

        try (FileInputStream fis = new FileInputStream(filePath);
             Workbook workbook = new XSSFWorkbook(fis)) {

            Sheet nodeSheet = workbook.getSheet("Nodes");
            Sheet edgeSheet = workbook.getSheet("Edges");

            if (nodeSheet == null) {
                throw new IOException("Sheet 'Nodes' not found.");
            }

            if (edgeSheet == null) {
                throw new IOException("Sheet 'Edges' not found.");
            }

            for (int i = 1; i <= nodeSheet.getLastRowNum(); i++) {
                Row row = nodeSheet.getRow(i);
                if (row == null) continue;

                String name = getString(row.getCell(0));
                double latitude = getDouble(row.getCell(1));
                double longitude = getDouble(row.getCell(2));
                double x = getDouble(row.getCell(3));
                double y = getDouble(row.getCell(4));

                if (name != null && !name.isBlank()) {
                    graph.addNode(name, latitude, longitude, x, y);
                }
            }

            for (int i = 1; i <= edgeSheet.getLastRowNum(); i++) {
                Row row = edgeSheet.getRow(i);
                if (row == null) continue;

                String from = getString(row.getCell(0));
                String to = getString(row.getCell(1));
                double distance = getDouble(row.getCell(2));

                if (from != null && to != null && !from.isBlank() && !to.isBlank()) {
                    graph.addEdge(from, to, distance);
                }
            }
        }

        return graph;
    }

    private static String getString(Cell cell) {
        if (cell == null) return null;

        return switch (cell.getCellType()) {
            case STRING -> cell.getStringCellValue().trim();
            case NUMERIC -> String.valueOf((int) cell.getNumericCellValue());
            default -> null;
        };
    }

    private static double getDouble(Cell cell) {
        if (cell == null) return 0;

        return switch (cell.getCellType()) {
            case NUMERIC -> cell.getNumericCellValue();
            case STRING -> {
                try {
                    yield Double.parseDouble(cell.getStringCellValue().trim());
                } catch (Exception e) {
                    yield 0;
                }
            }
            default -> 0;
        };
    }
}
