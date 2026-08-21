package com.telco.ventas.service;

import com.lowagie.text.Document;
import com.lowagie.text.DocumentException;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.FontFactory;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import com.telco.ventas.dto.ReporteDto;
import com.telco.ventas.dto.ResumenVentasResponse;
import com.telco.ventas.entity.Usuario;
import lombok.RequiredArgsConstructor;
import org.apache.poi.ss.usermodel.BorderStyle;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.HorizontalAlignment;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.VerticalAlignment;
import org.apache.poi.xssf.usermodel.XSSFCell;
import org.apache.poi.xssf.usermodel.XSSFCellStyle;
import org.apache.poi.xssf.usermodel.XSSFColor;
import org.apache.poi.xssf.usermodel.XSSFFont;
import org.apache.poi.xssf.usermodel.XSSFRow;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.awt.Color;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@Service
@RequiredArgsConstructor
public class ReporteExportService {

    private static final Color AZUL = new Color(37, 99, 235);
    private static final Color AZUL_OSCURO = new Color(30, 58, 138);
    private static final Color GRIS_CLARO = new Color(241, 245, 249);
    private static final byte[] AZUL_RGB = { (byte) 37, (byte) 99, (byte) 235 };
    private static final byte[] AZUL_OSCURO_RGB = { (byte) 30, (byte) 58, (byte) 138 };
    private static final byte[] GRIS_CLARO_RGB = { (byte) 241, (byte) 245, (byte) 249 };

    private final ReporteService reporteService;

    public byte[] exportar(Usuario usuario, String tipo, String formato,
            LocalDate dia, Integer anio, Integer mes,
            LocalDate desde, LocalDate hasta) {
        List<Tabla> tablas = construirTablas(usuario, tipo, dia, anio, mes, desde, hasta);
        String subtitulo = construirSubtitulo(dia, anio, mes, desde, hasta, usuario);
        return switch (formato.toLowerCase()) {
            case "csv" -> toCsv(tablas, subtitulo);
            case "xlsx", "excel" -> toXlsx(tablas, subtitulo);
            case "pdf" -> toPdf(tablas, subtitulo);
            case "html" -> toHtml(tablas, subtitulo);
            default -> throw new IllegalArgumentException("Formato no soportado: " + formato);
        };
    }

    public String nombreArchivo(String tipo, String formato) {
        String base = switch (tipo) {
            case "resumen" -> "reporte_resumen";
            case "por-plan" -> "reporte_por_plan";
            case "por-agente" -> "reporte_por_agente";
            case "comisiones" -> "reporte_comisiones";
            default -> "reporte";
        };
        String ext = switch (formato.toLowerCase()) {
            case "csv" -> "csv";
            case "xlsx", "excel" -> "xlsx";
            case "pdf" -> "pdf";
            case "html" -> "html";
            default -> "txt";
        };
        return base + "_" + LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE) + "." + ext;
    }

    public String contentType(String formato) {
        return switch (formato.toLowerCase()) {
            case "csv" -> "text/csv; charset=utf-8";
            case "xlsx", "excel" -> "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";
            case "pdf" -> "application/pdf";
            case "html" -> "text/html; charset=utf-8";
            default -> "application/octet-stream";
        };
    }

    // ---------------------------------------------------------------- modelo

    private List<Tabla> construirTablas(Usuario usuario, String tipo,
            LocalDate dia, Integer anio, Integer mes,
            LocalDate desde, LocalDate hasta) {
        switch (tipo) {
            case "resumen" -> {
                ResumenVentasResponse r = reporteService.resumenVentas(usuario, dia, anio, mes, desde, hasta);
                List<Tabla> tablas = new ArrayList<>();
                tablas.add(Tabla.kpis(r));
                tablas.add(Tabla.porDia(r));
                return tablas;
            }
            case "por-plan" -> {
                List<ReporteDto.PorPlan> data = reporteService.ventasPorPlan(usuario, dia, anio, mes, desde, hasta);
                return List.of(Tabla.porPlan(data));
            }
            case "por-agente" -> {
                List<ReporteDto.PorAgente> data = reporteService.ventasPorAgente(usuario, dia, anio, mes, desde, hasta);
                return List.of(Tabla.porAgente(data));
            }
            case "comisiones" -> {
                ReporteDto.ResumenComisiones data = reporteService.resumenComisiones(usuario);
                return List.of(Tabla.comisiones(data));
            }
            default -> throw new IllegalArgumentException("Tipo de reporte no soportado: " + tipo);
        }
    }

    private String construirSubtitulo(LocalDate dia, Integer anio, Integer mes,
            LocalDate desde, LocalDate hasta, Usuario usuario) {
        String periodo;
        if (desde != null && hasta != null) {
            periodo = "Período: " + desde.format(DateTimeFormatter.ofPattern("dd/MM/yyyy"))
                    + " al " + hasta.format(DateTimeFormatter.ofPattern("dd/MM/yyyy"));
        } else if (dia != null) {
            periodo = "Día: " + dia.format(DateTimeFormatter.ofPattern("dd/MM/yyyy"));
        } else if (anio != null && mes != null) {
            periodo = "Mes: " + String.format("%02d/%d", mes, anio);
        } else {
            periodo = "Año actual";
        }
        String generado = LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss"));
        return periodo + "  |  Generado: " + generado + "  |  Usuario: " + usuario.getUsername();
    }

    // ---------------------------------------------------------------- CSV

    private byte[] toCsv(List<Tabla> tablas, String subtitulo) {
        StringBuilder sb = new StringBuilder();
        sb.append('\uFEFF');
        for (Tabla t : tablas) {
            sb.append(t.titulo).append('\n');
            sb.append(subtitulo).append('\n');
            sb.append(t.headers.stream().collect(Collectors.joining(";"))).append('\n');
            for (Object[] fila : t.filas) {
                sb.append(IntStream.range(0, fila.length)
                        .mapToObj(i -> celda(fila[i], t.montoCols.contains(i)))
                        .collect(Collectors.joining(";"))).append('\n');
            }
            if (t.conTotales && !t.filas.isEmpty()) {
                Object[] tot = totales(t);
                sb.append(IntStream.range(0, tot.length)
                        .mapToObj(i -> celda(tot[i], t.montoCols.contains(i)))
                        .collect(Collectors.joining(";"))).append('\n');
            }
            sb.append('\n');
        }
        return sb.toString().getBytes(StandardCharsets.UTF_8);
    }

    private String celda(Object v, boolean moneda) {
        if (v == null)
            return "";
        if (v instanceof BigDecimal bd) {
            return moneda ? "S/ " + bd.setScale(2).toPlainString() : bd.toPlainString();
        }
        if (v instanceof Number)
            return String.valueOf(v);
        return String.valueOf(v);
    }

    // ---------------------------------------------------------------- XLSX

    private byte[] toXlsx(List<Tabla> tablas, String subtitulo) {
        try (XSSFWorkbook wb = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            for (int ti = 0; ti < tablas.size(); ti++) {
                Tabla t = tablas.get(ti);
                XSSFSheet sheet = wb.createSheet(t.titulo.length() > 28 ? "Tabla " + (ti + 1) : t.titulo);
                XSSFCellStyle styleTitulo = tituloStyle(wb);
                XSSFCellStyle styleSub = subtituloStyle(wb);
                XSSFCellStyle styleHeader = headerStyle(wb);
                XSSFCellStyle styleCelda = celdaStyle(wb, false);
                XSSFCellStyle styleCeldaAlt = celdaStyle(wb, true);
                XSSFCellStyle styleMonto = montoStyle(wb, false);
                XSSFCellStyle styleMontoAlt = montoStyle(wb, true);
                XSSFCellStyle styleTotal = totalStyle(wb);

                int r = 0;
                XSSFRow rowTitulo = sheet.createRow(r++);
                rowTitulo.setHeight((short) 560);
                XSSFCell c = rowTitulo.createCell(0);
                c.setCellValue(t.titulo);
                c.setCellStyle(styleTitulo);
                sheet.addMergedRegion(
                        new org.apache.poi.ss.util.CellRangeAddress(0, 0, 0, Math.max(0, t.headers.size() - 1)));

                XSSFRow rowSub = sheet.createRow(r++);
                rowSub.setHeight((short) 320);
                XSSFCell cs = rowSub.createCell(0);
                cs.setCellValue(subtitulo);
                cs.setCellStyle(styleSub);
                sheet.addMergedRegion(
                        new org.apache.poi.ss.util.CellRangeAddress(1, 1, 0, Math.max(0, t.headers.size() - 1)));

                XSSFRow rowHeader = sheet.createRow(r++);
                for (int h = 0; h < t.headers.size(); h++) {
                    XSSFCell hc = rowHeader.createCell(h);
                    hc.setCellValue(t.headers.get(h));
                    hc.setCellStyle(styleHeader);
                }

                for (int i = 0; i < t.filas.size(); i++) {
                    XSSFRow row = sheet.createRow(r++);
                    Object[] fila = t.filas.get(i);
                    for (int j = 0; j < fila.length; j++) {
                        XSSFCell cell = row.createCell(j);
                        boolean monto = t.montoCols.contains(j);
                        XSSFCellStyle st = monto ? (i % 2 == 0 ? styleMonto : styleMontoAlt)
                                : (i % 2 == 0 ? styleCelda : styleCeldaAlt);
                        if (fila[j] instanceof Number n) {
                            cell.setCellValue(n.doubleValue());
                        } else if (fila[j] != null) {
                            cell.setCellValue(String.valueOf(fila[j]));
                        }
                        cell.setCellStyle(st);
                    }
                }

                if (t.conTotales && !t.filas.isEmpty()) {
                    XSSFRow rowTotal = sheet.createRow(r++);
                    Object[] tot = totales(t);
                    for (int j = 0; j < tot.length; j++) {
                        XSSFCell cell = rowTotal.createCell(j);
                        if (tot[j] instanceof Number n) {
                            cell.setCellValue(n.doubleValue());
                        } else if (tot[j] != null) {
                            cell.setCellValue(String.valueOf(tot[j]));
                        }
                        cell.setCellStyle(styleTotal);
                    }
                }

                int[] anchos = { 14, 22, 12, 12, 12, 14, 14, 14, 14, 14, 14 };
                for (int j = 0; j < t.headers.size() && j < anchos.length; j++) {
                    sheet.setColumnWidth(j, anchos[j] * 256);
                }
                sheet.setAutoFilter(new org.apache.poi.ss.util.CellRangeAddress(2, Math.max(2, r - 1), 0,
                        Math.max(0, t.headers.size() - 1)));
            }
            wb.write(out);
            return out.toByteArray();
        } catch (Exception e) {
            throw new IllegalStateException("Error generando Excel", e);
        }
    }

    private XSSFCellStyle tituloStyle(XSSFWorkbook wb) {
        XSSFCellStyle st = wb.createCellStyle();
        st.setFillForegroundColor(new XSSFColor(AZUL_OSCURO_RGB));
        st.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        st.setAlignment(HorizontalAlignment.CENTER);
        st.setVerticalAlignment(VerticalAlignment.CENTER);
        XSSFFont f = wb.createFont();
        f.setBold(true);
        f.setColor(IndexedColors.WHITE.getIndex());
        f.setFontHeightInPoints((short) 16);
        st.setFont(f);
        return st;
    }

    private XSSFCellStyle subtituloStyle(XSSFWorkbook wb) {
        XSSFCellStyle st = wb.createCellStyle();
        st.setFillForegroundColor(new XSSFColor(GRIS_CLARO_RGB));
        st.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        st.setAlignment(HorizontalAlignment.LEFT);
        st.setVerticalAlignment(VerticalAlignment.CENTER);
        XSSFFont f = wb.createFont();
        f.setColor(IndexedColors.GREY_50_PERCENT.getIndex());
        f.setFontHeightInPoints((short) 10);
        st.setFont(f);
        return st;
    }

    private XSSFCellStyle headerStyle(XSSFWorkbook wb) {
        XSSFCellStyle st = wb.createCellStyle();
        st.setFillForegroundColor(new XSSFColor(AZUL_RGB));
        st.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        st.setAlignment(HorizontalAlignment.CENTER);
        st.setVerticalAlignment(VerticalAlignment.CENTER);
        st.setBorderBottom(BorderStyle.MEDIUM);
        st.setBorderTop(BorderStyle.MEDIUM);
        st.setBorderLeft(BorderStyle.THIN);
        st.setBorderRight(BorderStyle.THIN);
        XSSFFont f = wb.createFont();
        f.setBold(true);
        f.setColor(IndexedColors.WHITE.getIndex());
        st.setFont(f);
        return st;
    }

    private XSSFCellStyle celdaStyle(XSSFWorkbook wb, boolean alt) {
        XSSFCellStyle st = wb.createCellStyle();
        if (alt) {
            st.setFillForegroundColor(new XSSFColor(GRIS_CLARO_RGB));
            st.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        }
        st.setVerticalAlignment(VerticalAlignment.CENTER);
        st.setBorderBottom(BorderStyle.THIN);
        st.setBorderLeft(BorderStyle.THIN);
        st.setBorderRight(BorderStyle.THIN);
        st.setBorderTop(BorderStyle.THIN);
        return st;
    }

    private XSSFCellStyle montoStyle(XSSFWorkbook wb, boolean alt) {
        XSSFCellStyle st = celdaStyle(wb, alt);
        st.setAlignment(HorizontalAlignment.RIGHT);
        st.setDataFormat(wb.createDataFormat().getFormat("S/ #,##0.00"));
        return st;
    }

    private XSSFCellStyle totalStyle(XSSFWorkbook wb) {
        XSSFCellStyle st = celdaStyle(wb, false);
        st.setFillForegroundColor(new XSSFColor(AZUL_RGB));
        st.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        XSSFFont f = wb.createFont();
        f.setBold(true);
        f.setColor(IndexedColors.WHITE.getIndex());
        st.setFont(f);
        st.setBorderTop(BorderStyle.MEDIUM);
        return st;
    }

    // ---------------------------------------------------------------- PDF

    private byte[] toPdf(List<Tabla> tablas, String subtitulo) {
        try {
            Document doc = new Document(PageSize.A4.rotate());
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            PdfWriter.getInstance(doc, out);
            doc.open();

            for (int ti = 0; ti < tablas.size(); ti++) {
                Tabla t = tablas.get(ti);
                Paragraph titulo = new Paragraph(t.titulo,
                        FontFactory.getFont(FontFactory.HELVETICA_BOLD, 18, Font.BOLD, AZUL_OSCURO));
                titulo.setSpacingAfter(4);
                doc.add(titulo);

                Paragraph sub = new Paragraph(subtitulo,
                        FontFactory.getFont(FontFactory.HELVETICA, 9, Font.NORMAL, Color.GRAY));
                sub.setSpacingAfter(10);
                doc.add(sub);

                PdfPTable tabla = new PdfPTable(t.headers.size());
                tabla.setWidthPercentage(100);
                tabla.setHeaderRows(1);

                for (String h : t.headers) {
                    PdfPCell hc = new PdfPCell(new Phrase(h,
                            FontFactory.getFont(FontFactory.HELVETICA_BOLD, 9, Font.BOLD, Color.WHITE)));
                    hc.setBackgroundColor(AZUL);
                    hc.setHorizontalAlignment(Element.ALIGN_CENTER);
                    hc.setPadding(6);
                    tabla.addCell(hc);
                }

                for (int i = 0; i < t.filas.size(); i++) {
                    Object[] fila = t.filas.get(i);
                    for (int j = 0; j < fila.length; j++) {
                        boolean moneda = t.montoCols.contains(j);
                        PdfPCell cell = new PdfPCell(new Phrase(celda(fila[j], moneda),
                                FontFactory.getFont(FontFactory.HELVETICA, 9, Font.NORMAL, Color.BLACK)));
                        if (moneda)
                            cell.setHorizontalAlignment(Element.ALIGN_RIGHT);
                        if (i % 2 == 0)
                            cell.setBackgroundColor(GRIS_CLARO);
                        cell.setPadding(5);
                        tabla.addCell(cell);
                    }
                }

                if (t.conTotales && !t.filas.isEmpty()) {
                    Object[] tot = totales(t);
                    for (int j = 0; j < tot.length; j++) {
                        boolean moneda = t.montoCols.contains(j);
                        PdfPCell cell = new PdfPCell(new Phrase(celda(tot[j], moneda),
                                FontFactory.getFont(FontFactory.HELVETICA_BOLD, 9, Font.BOLD, Color.WHITE)));
                        cell.setBackgroundColor(AZUL);
                        if (moneda)
                            cell.setHorizontalAlignment(Element.ALIGN_RIGHT);
                        cell.setPadding(5);
                        tabla.addCell(cell);
                    }
                }

                doc.add(tabla);
                if (ti < tablas.size() - 1) {
                    Paragraph gap = new Paragraph(" ");
                    gap.setSpacingAfter(16);
                    doc.add(gap);
                }
            }
            doc.close();
            return out.toByteArray();
        } catch (DocumentException e) {
            throw new IllegalStateException("Error generando PDF", e);
        }
    }

    // ---------------------------------------------------------------- HTML

    private byte[] toHtml(List<Tabla> tablas, String subtitulo) {
        StringBuilder sb = new StringBuilder();
        sb.append("<!DOCTYPE html><html lang='es'><head><meta charset='utf-8'>")
                .append("<title>Reporte Telco</title><style>")
                .append("body{font-family:Segoe UI,Arial,sans-serif;color:#0f172a;margin:24px}")
                .append("h1{background:#1e3a8a;color:#fff;padding:14px 18px;border-radius:8px;font-size:20px}")
                .append(".sub{color:#64748b;font-size:12px;margin:8px 0 16px}")
                .append("table{border-collapse:collapse;width:100%;margin-bottom:28px;font-size:13px}")
                .append("th{background:#2563eb;color:#fff;padding:8px 10px;text-align:center}")
                .append("td{border:1px solid #e2e8f0;padding:7px 10px}")
                .append("tr:nth-child(even){background:#f1f5f9}")
                .append("tfoot td{background:#2563eb;color:#fff;font-weight:bold}")
                .append("</style></head><body>");

        for (Tabla t : tablas) {
            sb.append("<h1>").append(t.titulo).append("</h1>")
                    .append("<p class='sub'>").append(subtitulo).append("</p>")
                    .append("<table><thead><tr>");
            for (String h : t.headers)
                sb.append("<th>").append(h).append("</th>");
            sb.append("</tr></thead><tbody>");
            for (Object[] fila : t.filas) {
                sb.append("<tr>");
                for (int j = 0; j < fila.length; j++) {
                    sb.append("<td>").append(celda(fila[j], t.montoCols.contains(j))).append("</td>");
                }
                sb.append("</tr>");
            }
            sb.append("</tbody>");
            if (t.conTotales && !t.filas.isEmpty()) {
                Object[] tot = totales(t);
                sb.append("<tfoot><tr>");
                for (int j = 0; j < tot.length; j++) {
                    sb.append("<td>").append(celda(tot[j], t.montoCols.contains(j))).append("</td>");
                }
                sb.append("</tr></tfoot>");
            }
            sb.append("</table>");
        }
        sb.append("</body></html>");
        return sb.toString().getBytes(StandardCharsets.UTF_8);
    }

    // ---------------------------------------------------------------- helpers

    private Object[] totales(Tabla t) {
        Object[] tot = new Object[t.headers.size()];
        for (int i = 0; i < t.headers.size(); i++)
            tot[i] = i == 0 ? "TOTAL" : BigDecimal.ZERO;
        for (Object[] fila : t.filas) {
            for (int j = 1; j < fila.length; j++) {
                if (fila[j] instanceof Number n) {
                    tot[j] = ((BigDecimal) tot[j]).add(new BigDecimal(n.toString()));
                }
            }
        }
        return tot;
    }

    private record Tabla(String titulo, List<String> headers, Set<Integer> montoCols, List<Object[]> filas,
            boolean conTotales) {

        static Tabla kpis(ResumenVentasResponse r) {
            return new Tabla("Resumen General de Ventas",
                    List.of("Métrica", "Valor"),
                    Set.of(4),
                    List.of(new Object[] { "Total de ventas", r.getTotalVentas() },
                            new Object[] { "Pendientes", r.getTotalPendientes() },
                            new Object[] { "Aprobadas", r.getTotalAprobadas() },
                            new Object[] { "Rechazadas", r.getTotalRechazadas() },
                            new Object[] { "Monto aprobado (S/)", r.getMontoTotalAprobadas() }),
                    false);
        }

        static Tabla porDia(ResumenVentasResponse r) {
            List<Object[]> filas = new ArrayList<>();
            for (ResumenVentasResponse.VentasPorDia d : r.getVentasPorDia()) {
                filas.add(new Object[] { d.getFecha(), d.getCantidad(), d.getMonto() });
            }
            return new Tabla("Ventas por Día",
                    List.of("Fecha", "Cantidad", "Monto (S/)"),
                    Set.of(2),
                    filas,
                    true);
        }

        static Tabla porPlan(List<ReporteDto.PorPlan> data) {
            List<Object[]> filas = new ArrayList<>();
            for (ReporteDto.PorPlan p : data) {
                filas.add(new Object[] { p.getPlanCodigo(), p.getPlanNombre(), p.getCantidad(), p.getAprobadas(),
                        p.getMontoAprobado() });
            }
            return new Tabla("Reporte de Ventas por Plan",
                    List.of("Plan", "Nombre", "Cantidad", "Aprobadas", "Monto aprobado (S/)"),
                    Set.of(4),
                    filas,
                    true);
        }

        static Tabla porAgente(List<ReporteDto.PorAgente> data) {
            List<Object[]> filas = new ArrayList<>();
            for (ReporteDto.PorAgente a : data) {
                filas.add(new Object[] { a.getAgenteUsername(), a.getTotal(), a.getPendientes(), a.getAprobadas(),
                        a.getRechazadas(), a.getMontoAprobado() });
            }
            return new Tabla("Reporte de Ventas por Agente",
                    List.of("Agente", "Total", "Pendientes", "Aprobadas", "Rechazadas", "Monto aprobado (S/)"),
                    Set.of(5),
                    filas,
                    true);
        }

        static Tabla comisiones(ReporteDto.ResumenComisiones data) {
            List<Object[]> filas = new ArrayList<>();
            for (ReporteDto.ComisionAgente c : data.getPorAgente()) {
                filas.add(new Object[] { c.getAgenteUsername(), c.getTotal(), c.getPendientes(), c.getPagadas(),
                        c.getMontoPendiente(), c.getMontoPagado() });
            }
            return new Tabla("Reporte de Comisiones por Agente",
                    List.of("Agente", "Total", "Pendientes", "Pagadas", "Monto pendiente (S/)", "Monto pagado (S/)"),
                    Set.of(4, 5),
                    filas,
                    true);
        }
    }
}
