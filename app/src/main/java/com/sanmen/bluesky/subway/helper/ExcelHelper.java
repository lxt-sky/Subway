package com.sanmen.bluesky.subway.helper;

import android.os.Environment;
import com.sanmen.bluesky.subway.data.bean.AlarmInfo;
import com.sanmen.bluesky.subway.data.bean.DriveRecord;
import org.apache.poi.hssf.usermodel.*;
import org.apache.poi.ss.usermodel.HorizontalAlignment;
import org.apache.poi.ss.usermodel.VerticalAlignment;

import java.io.File;
import java.io.FileOutputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

/**
 * @author lxt_bluesky
 * @date 2019/4/9
 * @description
 */
public class ExcelHelper {

    private static final String ALARM_NAME = AlarmInfo.class.getName();
    private static final String DRIVE_NAME = DriveRecord.class.getName();

    /**
     * 表格数据和SQLite中的要对应
     */
    public static String createExcel(List<Object> data) throws Exception {
        ArrayList<Object> alarmList = new ArrayList<Object>();
        ArrayList<Object> driveList = new ArrayList<Object>();


        for (Object bean : data) {

            if (ALARM_NAME.equals(bean.getClass().getName())){
                alarmList.add(bean);
            }else if (DRIVE_NAME.equals(bean.getClass().getName())){
                driveList.add(bean);
            }
        }

        // 创建文档
        HSSFWorkbook workbook = new HSSFWorkbook();
        HSSFCellStyle cellStyle = workbook.createCellStyle();
        cellStyle.setAlignment(HSSFCellStyle.ALIGN_LEFT);
        cellStyle.setVerticalAlignment(HSSFCellStyle.VERTICAL_BOTTOM);
        HSSFSheet alarmSheet = workbook.createSheet("报警信息");
        int alarmSize = alarmList.size();
        if (alarmSize > 0) {
            insertRow(alarmSheet, (short) 0,new Object[]{"报警ID","记录ID","报警信息","报警时间"}, cellStyle);
            for (short i = 0; i < alarmSize; i++) {
                AlarmInfo alarmBean = (AlarmInfo) alarmList.get(i);
                Object[] values = {alarmBean.getId(),alarmBean.getRecordId(),alarmBean.getAlarmText(),alarmBean.getAlarmTime()};
                insertRow(alarmSheet, (short) (i+1), values, cellStyle);
            }
        }
        HSSFSheet driveSheet = workbook.createSheet("行车记录");
        int driveSize = driveList.size();
        if (driveSize > 0) {
            insertRow(driveSheet, (short) 0,new Object[]{"记录ID","记录名称","开始时间","结束时间"}, cellStyle);
            for (short i = 0; i < driveSize; i++) {
                DriveRecord driveBean = (DriveRecord) driveList.get(i);
                Object[] values = {driveBean.getId(),driveBean.getDriveName(),driveBean.getDriveBeginTime(),driveBean.getDriveEndTime()};
                insertRow(driveSheet, (short) (i+1), values, cellStyle);
            }
        }

        // 保存文档
        String dirPath = Environment.getExternalStorageDirectory().getAbsolutePath() +File.separator +"subway/tmp";
        new File(dirPath).mkdirs();
        File file = new File(dirPath, "subway_data.xls");
        FileOutputStream fos;

        if (!file.exists()) {
            file.createNewFile();
        }
        fos = new FileOutputStream(file);
        workbook.write(fos);
        fos.close();
        return file.getAbsolutePath();
    }

    public static void deleteExcel() {
        String dirPath = Environment.getExternalStorageDirectory().getAbsolutePath() + "subway/tmp";
        File file = new File(dirPath, "subway_data.xls");
        file.delete();
    }

    /**
     * 插入一行数据
     *
     * @param sheet        插入数据行的表单
     * @param rowIndex     插入的行的索引
     * @param columnValues 要插入一行中的数据，数组表示
     * @param cellStyle    该格中数据的显示样式
     */

    private static void insertRow(HSSFSheet sheet, short rowIndex,
                                  Object[] columnValues, HSSFCellStyle cellStyle) {
        HSSFRow row = sheet.createRow(rowIndex);
        int column = columnValues.length;
        for (short i = 0; i < column; i++) {
            createCell(row, i, columnValues[i], cellStyle);
        }
    }

    /**
     * 在一行中插入一个单元值
     *
     * @param row         要插入的数据的行
     * @param columnIndex 插入的列的索引
     * @param cellValue   该cell的值：如果是Calendar或者Date类型，就先对其格式化
     * @param cellStyle   该格中数据的显示样式
     */
    private static void createCell(HSSFRow row, short columnIndex, Object cellValue,
                                   HSSFCellStyle cellStyle) {
        HSSFCell cell = row.createCell(columnIndex);
        // 如果是Calender或者Date类型的数据，就格式化成字符串
        if (cellValue instanceof Date) {
            SimpleDateFormat format = new SimpleDateFormat(
                    "yyyy-MM-dd HH:mm:ss");
            String value = format.format(cellValue);
            HSSFRichTextString richTextString = new HSSFRichTextString(value);
            cell.setCellValue(richTextString);
        } else if (cellValue instanceof Calendar) {
            SimpleDateFormat format = new SimpleDateFormat(
                    "yyyy-MM-dd HH:mm:ss");
            String value = format.format(((Calendar) cellValue).getTime());
            HSSFRichTextString richTextString = new HSSFRichTextString(value);
            cell.setCellValue(richTextString);
        } else {
            HSSFRichTextString richTextString = new HSSFRichTextString(cellValue.toString());
            cell.setCellValue(richTextString);
        }
        cell.setCellStyle(cellStyle);
    }

}
