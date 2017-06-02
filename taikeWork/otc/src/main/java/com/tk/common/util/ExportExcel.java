package com.tk.common.util;

import com.google.common.collect.Lists;
import org.apache.commons.lang.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.streaming.SXSSFWorkbook;
import org.apache.poi.xssf.usermodel.XSSFClientAnchor;
import org.apache.poi.xssf.usermodel.XSSFRichTextString;

import javax.mail.internet.MimeUtility;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.URLEncoder;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 导出Excel文件（导出“XLSX”格式，支持大数据量导出 @see org.apache.poi.ss.SpreadsheetVersion）
 *
 * @author yishion
 */
public class ExportExcel {

	private static Logger log = LogManager.getLogger(ExportExcel.class.getName());

	/**
	 * 工作薄对象
	 */
	private SXSSFWorkbook wb;

	/**
	 * 工作表对象
	 */
	private Sheet sheet;

	/**
	 * 样式列表
	 */
	private Map<String, CellStyle> styles;

	/**
	 * 当前行号
	 */
	private int rownum;

	/**
	 * 构造函数
	 *
	 * @param title   表格标题，传“空值”，表示无标题
	 * @param headers 表头数组
	 */
	public ExportExcel(String title, String[] headers) {
		initialize(title, Lists.newArrayList(headers));
	}

	/**
	 * 构造函数
	 *
	 * @param title      表格标题，传“空值”，表示无标题
	 * @param headerList 表头列表
	 */
	public ExportExcel(String title, List<String> headerList) {
		initialize(title, headerList);
	}

	/**
	 * 初始化函数
	 *
	 * @param title      表格标题，传“空值”，表示无标题
	 * @param headerList 表头列表
	 */
	private void initialize(String title, List<String> headerList) {
		this.wb = new SXSSFWorkbook(500);
		this.sheet = wb.createSheet(StringUtils.isNotBlank(title) ? title : "Export");
		this.styles = createStyles(wb);
		// Create title
		if (StringUtils.isNotBlank(title)) {
			Row titleRow = sheet.createRow(rownum++);
			titleRow.setHeightInPoints(30);
			Cell titleCell = titleRow.createCell(0);
			titleCell.setCellStyle(styles.get("title"));
			titleCell.setCellValue(title);
			sheet.addMergedRegion(new CellRangeAddress(titleRow.getRowNum(),
							titleRow.getRowNum(), titleRow.getRowNum(), headerList
							.size() - 1));
		}
		// Create header
		if (headerList == null) {
			throw new RuntimeException("headerList not null!");
		}
		Row headerRow = sheet.createRow(rownum++);
		headerRow.setHeightInPoints(16);
		for (int i = 0; i < headerList.size(); i++) {
			Cell cell = headerRow.createCell(i);
			cell.setCellStyle(styles.get("header"));
			String[] ss = StringUtils.split(headerList.get(i), "**", 2);
			if (ss.length == 2) {
				cell.setCellValue(ss[0]);
				Comment comment = this.sheet.createDrawingPatriarch()
								.createCellComment(
												new XSSFClientAnchor(0, 0, 0, 0, (short) 3, 3,
																(short) 5, 6));
				comment.setString(new XSSFRichTextString(ss[1]));
				cell.setCellComment(comment);
			} else {
				cell.setCellValue(headerList.get(i));
			}
			sheet.autoSizeColumn(i);
		}
		for (int i = 0; i < headerList.size(); i++) {
			int colWidth = sheet.getColumnWidth(i) * 2;
			sheet.setColumnWidth(i, colWidth < 3000 ? 3000 : colWidth);
		}
		log.debug("Initialize success.");
	}

	/**
	 * 创建表格样式
	 *
	 * @param wb 工作薄对象
	 * @return 样式列表
	 */
	private Map<String, CellStyle> createStyles(Workbook wb) {
		Map<String, CellStyle> styles = new HashMap<String, CellStyle>();

		CellStyle style = wb.createCellStyle();
		style.setAlignment(CellStyle.ALIGN_CENTER);
		style.setVerticalAlignment(CellStyle.VERTICAL_CENTER);
		Font titleFont = wb.createFont();
		titleFont.setFontName("Arial");
		titleFont.setFontHeightInPoints((short) 16);
		titleFont.setBoldweight(Font.BOLDWEIGHT_BOLD);
		style.setFont(titleFont);
		styles.put("title", style);

		style = wb.createCellStyle();
		style.setVerticalAlignment(CellStyle.VERTICAL_CENTER);
		style.setBorderRight(CellStyle.BORDER_THIN);
		style.setRightBorderColor(IndexedColors.GREY_50_PERCENT.getIndex());
		style.setBorderLeft(CellStyle.BORDER_THIN);
		style.setLeftBorderColor(IndexedColors.GREY_50_PERCENT.getIndex());
		style.setBorderTop(CellStyle.BORDER_THIN);
		style.setTopBorderColor(IndexedColors.GREY_50_PERCENT.getIndex());
		style.setBorderBottom(CellStyle.BORDER_THIN);
		style.setBottomBorderColor(IndexedColors.GREY_50_PERCENT.getIndex());
		Font dataFont = wb.createFont();
		dataFont.setFontName("Arial");
		dataFont.setFontHeightInPoints((short) 10);
		style.setFont(dataFont);
		styles.put("data", style);

		style = wb.createCellStyle();
		style.cloneStyleFrom(styles.get("data"));
		style.setAlignment(CellStyle.ALIGN_LEFT);
		styles.put("data1", style);

		style = wb.createCellStyle();
		style.cloneStyleFrom(styles.get("data"));
		style.setAlignment(CellStyle.ALIGN_CENTER);
		styles.put("data2", style);

		style = wb.createCellStyle();
		style.cloneStyleFrom(styles.get("data"));
		style.setAlignment(CellStyle.ALIGN_RIGHT);
		styles.put("data3", style);

		style = wb.createCellStyle();
		style.cloneStyleFrom(styles.get("data"));
		// style.setWrapText(true);
		style.setAlignment(CellStyle.ALIGN_CENTER);
		style.setFillForegroundColor(IndexedColors.GREY_50_PERCENT.getIndex());
		style.setFillPattern(CellStyle.SOLID_FOREGROUND);
		Font headerFont = wb.createFont();
		headerFont.setFontName("Arial");
		headerFont.setFontHeightInPoints((short) 10);
		headerFont.setBoldweight(Font.BOLDWEIGHT_BOLD);
		headerFont.setColor(IndexedColors.WHITE.getIndex());
		style.setFont(headerFont);
		styles.put("header", style);

		return styles;
	}

	/**
	 * 添加一行
	 *
	 * @return 行对象
	 */
	public Row addRow() {
		return sheet.createRow(rownum++);
	}

	/**
	 * 添加一个单元格
	 *
	 * @param row    添加的行
	 * @param column 添加列号
	 * @param val    添加值
	 * @return 单元格对象
	 */
	public Cell addCell(Row row, int column, Object val) {
		return this.addCell(row, column, val, 0, Class.class);
	}

	/**
	 * 添加一个单元格
	 *
	 * @param row    添加的行
	 * @param column 添加列号
	 * @param val    添加值
	 * @param align  对齐方式（1：靠左；2：居中；3：靠右）
	 * @return 单元格对象
	 */
	public Cell addCell(Row row, int column, Object val, int align,
	                    Class<?> fieldType) {
		Cell cell = row.createCell(column);
		CellStyle style = styles.get("data"
						+ (align >= 1 && align <= 3 ? align : ""));
		try {
			if (val == null) {
				cell.setCellValue("");
			} else if (val instanceof String) {
				cell.setCellValue((String) val);
			} else if (val instanceof Integer) {
				cell.setCellValue((Integer) val);
			} else if (val instanceof Long) {
				cell.setCellValue((Long) val);
			} else if (val instanceof Double) {
				cell.setCellValue((Double) val);
			} else if (val instanceof Float) {
				cell.setCellValue((Float) val);
			} else if (val instanceof Date) {
				DataFormat format = wb.createDataFormat();
				style.setDataFormat(format.getFormat("yyyy-MM-dd"));
				cell.setCellValue((Date) val);
			} else {
				if (fieldType != Class.class) {
					cell.setCellValue((String) fieldType.getMethod("setValue",
									Object.class).invoke(null, val));
				} else {
					cell.setCellValue((String) Class
									.forName(
													this.getClass()
																	.getName()
																	.replaceAll(
																					this.getClass()
																									.getSimpleName(),
																					"fieldtype."
																									+ val.getClass()
																									.getSimpleName()
																									+ "Type"))
									.getMethod("setValue", Object.class)
									.invoke(null, val));
				}
			}
		} catch (Exception ex) {
			log.info("Set cell value [" + row.getRowNum() + "," + column
							+ "] error: " + ex.toString());
			cell.setCellValue(val.toString());
		}
		cell.setCellStyle(style);
		return cell;
	}

	/**
	 * 输出数据流
	 *
	 * @param os 输出数据流
	 */
	public ExportExcel write(OutputStream os) throws IOException {
		wb.write(os);
		return this;
	}

	/**
	 * 输出到客户端
	 *
	 * @param fileName 输出文件名
	 */
	public ExportExcel write(HttpServletResponse response, String fileName)
					throws IOException {
		response.reset();
		response.setContentType("application/octet-stream; charset=utf-8");
		response.setHeader("Content-Disposition", "attachment; filename="
						+ URLEncoder.encode(fileName, "UTF-8"));
		write(response.getOutputStream());
		return this;
	}

	public ExportExcel write(HttpServletRequest request,
	                         HttpServletResponse response, String fileName) throws IOException {
		response.reset();
		response.setContentType("application/octet-stream; charset=utf-8");
		response.setHeader("Content-Disposition", "attachment; filename="
						+ encodeFilename(fileName, request));
		write(response.getOutputStream());
		return this;
	}

	private static String encodeFilename(String filename, HttpServletRequest request) {
		/**
		 * 获取客户端浏览器和操作系统信息 在IE浏览器中得到的是：User-Agent=Mozilla/4.0 (compatible; MSIE 6.0; Windows NT 5.1; SV1; Maxthon; Alexa
		 * Toolbar) 在Firefox中得到的是：User-Agent=Mozilla/5.0 (Windows; U; Windows NT 5.1; zh-CN; rv:1.7.10) Gecko/20050717
		 * Firefox/1.0.6
		 */
		String agent = request.getHeader("USER-AGENT");
		try {
			if ((agent != null) && (-1 != agent.indexOf("MSIE"))) {
				String newFileName = URLEncoder.encode(filename, "UTF-8");
				newFileName = StringUtils.replace(newFileName, "+", "%20");
				if (newFileName.length() > 150) {
					newFileName = new String(filename.getBytes("GB2312"), "ISO8859-1");
					newFileName = StringUtils.replace(newFileName, " ", "%20");
				}
				return newFileName;
			}
			if ((agent != null) && (-1 != agent.indexOf("Mozilla")))
				return MimeUtility.encodeText(filename, "UTF-8", "B");
			return filename;
		} catch (Exception ex) {
			return filename;
		}
	}

	/**
	 * 输出到文件
	 *
	 * @param name 输出文件名
	 */
	public ExportExcel writeFile(String name) throws FileNotFoundException,
					IOException {
		FileOutputStream os = new FileOutputStream(name);
		this.write(os);
		return this;
	}

	public <E> ExportExcel setDataList(List<E> list) {
		for (E e : list) {
			int colunm = 0;
			Row row = this.addRow();
			StringBuilder sb = new StringBuilder();
			Map<String, Object> map = (Map<String, Object>) e;
			for (Map.Entry<String, Object> entry : map.entrySet()) {
				Object val = null;
				// Get entity value
				try {
					val = entry.getValue();
					String dictType = (String) map.get("dict_type");
					if (StringUtils.isNotBlank(dictType)) {
						//val = DictUtil.getDictLabel(val==null?"":val.toString(), dictType, "");
					}
				} catch (Exception ex) {
					// Failure to ignore
					log.info(ex.toString());
					val = "";
				}
				this.addCell(row, colunm++, val);
				sb.append(val + ", ");
			}
			log.debug("Write success: [" + row.getRowNum() + "] " + sb.toString());
		}
		return this;
	}

	/**
	 * 清理临时文件
	 */
	public ExportExcel dispose() {
		wb.dispose();
		return this;
	}

}