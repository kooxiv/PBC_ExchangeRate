package sample;

import jxl.Cell;
import jxl.Workbook;
import jxl.write.Label;
import jxl.write.WritableSheet;
import jxl.write.WritableWorkbook;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.sql.ResultSet;

/**
 * Created by kooxiv on 2017/5/6.
 */
public class FileUtil {

    static FileUtil util = new FileUtil();

    public static FileUtil getUtil() {
        return util;
    }

    private FileUtil() {
    }

    String lastDate;
    public static String FILENAME = "exchangeRate_";

    File getWritableFile(int index) {
        FILENAME = "exchangeRate_" + index + ".xls";
        File file = new File(FILENAME);
        if (file.exists()) {
            boolean x = file.delete();
            if (!x) {
                return getWritableFile(++index);
            }
        }
        return file;
    }

    public void save() {

        debug("生成excel");

        try {

            BufferedOutputStream bufferedOutputStream = new BufferedOutputStream(new FileOutputStream(getWritableFile(0)));
            //创建工作薄
            WritableWorkbook workbook = Workbook.createWorkbook(bufferedOutputStream);
            //创建新的一页
            WritableSheet sheet = workbook.createSheet("汇率", 0);
            //创建要显示的内容,创建一个单元格，第一个参数为列坐标，第二个参数为行坐标，第三个参数为内容
            Label dateHeader = new Label(0, 0, "日期");
            sheet.addCell(dateHeader);

            String topicSql = "SELECT DISTINCT [TOPICS] FROM [rate]";
            ResultSet resultSet = DbUtil.getDbUtil().query(topicSql);

            int position = 0;

            //标题行
            topic:
            while (resultSet.next()) {
                String head = resultSet.getString("TOPICS");
                if (head == null || head.length() < 1) {
                    continue topic;
                }
                Label headRow = new Label(++position, 0, head);
                sheet.addCell(headRow);
            }

            //日期列
            String dateSql = "SELECT DISTINCT [DATES] FROM [rate] ORDER BY DATES DESC";
            ResultSet dateResult = DbUtil.getDbUtil().query(dateSql);

            position = 0;
            date:
            while (dateResult.next()) {
                String dateItem = dateResult.getString("DATES");
                if (dateItem == null || dateItem.length() < 1) {
                    continue date;
                }
                Label dateRow = new Label(0, ++position, dateItem);
                sheet.addCell(dateRow);

                //降序排列的 第一个日期
                if (lastDate == null) {
                    lastDate = dateItem;
                }
            }

            //汇率
            int columns = sheet.getColumns();
            int tempR = sheet.getRows();

            for (; columns > 0; columns--) {
                Cell columnCell = sheet.getCell(columns, 0);
                String topic = columnCell.getContents();

                if (topic != null && topic.length() > 1) {
                    int rows = tempR;
                    for (; rows > 0; rows--) {
                        Cell rowCell = sheet.getCell(0, rows);
                        String date = rowCell.getContents();
                        if (date != null && date.length() > 1) {
                            String ratesql = "SELECT RATES from rate where TOPICS = '" + topic + "' AND DATES = '" + date + "'";
                            ResultSet rateResult = DbUtil.getDbUtil().query(ratesql);
                            rate:
                            while (rateResult.next()) {
                                String rate = rateResult.getString("RATES");
                                Label ratePlace = new Label(columns, rows, rate);
                                sheet.addCell(ratePlace);
                                debug("t:" + topic + " " + columns + "   d:" + date + " " + rows + "  " + rate);
                            }
                        }
                    }
                }
            }


            //
            workbook.write();
            workbook.close();
            bufferedOutputStream.close();

            FileOutputStream lastpostion = new FileOutputStream("lastPosition.txt", false);
            lastpostion.write(lastDate.getBytes());
            lastpostion.close();

        } catch (Exception e) {
            e.printStackTrace();
        }
        debug("生成excel成功");

    }

    public String getLastPostion() {
        try {
            FileReader reader = new FileReader("lastPosition.txt");
            char[] chars = new char[64];
            reader.read(chars);
            reader.close();

            return String.valueOf(chars).replaceAll("\r|\n|\t", "").trim();

        } catch (Exception e) {
        }
        return null;
    }

    void debug(String s) {
        System.out.println(s);
    }
}
