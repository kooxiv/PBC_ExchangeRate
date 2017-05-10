package sample;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.concurrent.Worker;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.image.*;
import javafx.scene.image.Image;
import javafx.scene.web.WebView;
import javafx.stage.Stage;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import sun.swing.ImageIconUIResource;

import java.awt.*;
import java.io.File;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;

public class Main extends Application {

    @Override
    public void start(Stage primaryStage) throws Exception {
        Parent root = FXMLLoader.load(getClass().getResource("index.fxml"));
        primaryStage.setTitle("PBC汇率");
        primaryStage.getIcons().add(new Image(getClass().getResourceAsStream("ico.png")));
        primaryStage.setScene(new Scene(root, 600, 58));
        primaryStage.setResizable(false);
        primaryStage.show();

        initViews(root);

    }

    WebView webView, subWeb;
    Label msgView;
    Button button;

    int page = 0;
    int maxPage = 1;

    void initParam() {
        page = 0;
        maxPage = 1;
        temp = false;
    }

    boolean temp;

    private void initViews(Parent root) {

        msgView = (Label) root.lookup("#tips");

        String lastpostion = FileUtil.getUtil().getLastPostion();
        debug("上次excel生成位置 ：" + lastpostion);
        try {
            if (lastpostion != null) {
                this.lastPostion = format.parse(lastpostion);
            }
            msgView.setText((this.lastPostion == null ? "" : "上次excel生成位置 ：" + lastpostion + "  ") + "更新中...");
        } catch (Exception e) {
            e.printStackTrace();
        }


        webView = (WebView) root.lookup("#webview");
        subWeb = (WebView) root.lookup("#subweb");

        button = (Button) root.lookup("#open");

        webView.getEngine().setJavaScriptEnabled(true);
        subWeb.getEngine().setJavaScriptEnabled(true);

        button.setOnAction(new EventHandler<ActionEvent>() {
            public void handle(ActionEvent event) {
                if (Desktop.isDesktopSupported()) {
                    try {
                        Desktop.getDesktop().open(new File(FileUtil.FILENAME));
                        System.exit(0);
                    } catch (Exception e) {
                        button.setText("打开失败");
                    }
                }
            }
        });

        webView.getEngine().getLoadWorker().stateProperty().addListener(new ChangeListener<Worker.State>() {
            @Override
            public void changed(ObservableValue<? extends Worker.State> observable, Worker.State oldValue, Worker.State newValue) {
                if (newValue == Worker.State.SUCCEEDED) {
                    Document document = webView.getEngine().getDocument();
                    if (document == null) {
                        return;
                    }

                    Element element = document.getElementById("r_con");
                    if (element != null) {
                        if (!temp) {
                            //                    总记录数:710,每页显示20条记录
                            String html = element.getTextContent();
                            totalDays = Integer.valueOf(html.substring(html.indexOf("总记录数:") + 5, html.indexOf(",每页显示")));
                            String per = html.substring(html.indexOf(",每页显示") + 5, html.indexOf("条记录"));
                            try {
                                perPage = Integer.valueOf(per);
                                if (perPage == 0) {
                                    perPage = 1;
                                }
                                maxPage = totalDays % perPage != 0 ? totalDays / perPage + 1 : totalDays / perPage;
                            } catch (Exception e) {
                                msgView.setText("抓取页码错误");
                            }
                            temp = true;
                        }

                        NodeList list = element.getElementsByTagName("a");
                        if (list != null && list.getLength() > 0) {
                            subList.clear();
                            for (int i = 0; i < list.getLength(); i++) {
                                if (list.item(i).getTextContent().contains("汇率")) {
                                    subList.add(list.item(i));
                                }
                            }
                            if (!subList.isEmpty()) {
                                getSubData();
                            }
                        }
                    }

                }
            }
        });


        subWeb.getEngine().getLoadWorker().stateProperty().addListener(new ChangeListener<Worker.State>() {
            @Override
            public void changed(ObservableValue<? extends Worker.State> observable, Worker.State oldValue, Worker.State newValue) {
                if (newValue == Worker.State.SUCCEEDED) {
                    Document document = subWeb.getEngine().getDocument();
                    if (document == null) {
                        return;
                    }
                    Element element = document.getElementById("zoom");
                    if (element != null) {
                        NodeList plist = element.getElementsByTagName("p");
                        if (plist != null && plist.getLength() > 0) {
                            String str = "";
                            k:
                            for (int p = 0; p < plist.getLength(); p++) {
                                str = plist.item(p).getTextContent();
                                if (str != null && str.contains("交易中心公布")) {
                                    break k;
                                }
                            }

                            //
                            if (str == null || !str.contains("中间价为")) {
                                NodeList spanList = element.getElementsByTagName("span");
                                if (spanList != null && spanList.getLength() > 0) {
                                    k:
                                    for (int p = 0; p < spanList.getLength(); p++) {
                                        str = spanList.item(p).getTextContent();
                                        if (str != null && str.contains("中间价为")) {
                                            break k;
                                        }
                                    }
                                }
                            }

                            if (str != null && str.length() > 0) {
                                str = str.replaceAll("，", ",");
                                str = str.replaceAll("。", ",");
                                String date = null;
                                try {
                                    if (str.contains("银行间外汇市场人民币汇率中间价为")) {
                                        date = str.substring(str.indexOf(",") + 1, str.indexOf("银行间外汇市场人民币汇率中间价为"));
                                    } else if (str.contains("银行间外汇市场美元等交易货币对人民币汇率的中间价为")) {
                                        if (str.contains("交易中心公布")) {
                                            date = str.substring(str.indexOf(",") + 1, str.indexOf("银行间外汇市场美元等交易货币对人民币汇率的中间价为"));
                                        } else {
                                            date = str.substring(0, str.indexOf("银行间外汇市场美元等交易货币对人民币汇率的中间价为"));
                                        }
                                    } else {
                                        debug("1 源数据格式不正确: " + str);
                                    }

                                } catch (Exception e) {
                                    e.printStackTrace();
                                }
                                if (date != null) {
                                    date = date.trim();
                                    if (!date.contains("日")) {
                                        date = date + "日";
                                    }
                                    String fex = "";
                                    if (str.contains("人民币汇率中间价为：")) {
                                        fex = str.substring(str.indexOf("人民币汇率中间价为：") + 10);
                                    } else if (str.contains("人民币汇率的中间价为：")) {
                                        fex = str.substring(str.indexOf("人民币汇率的中间价为：") + 11);
                                    } else {
                                        debug("2 源数据格式不正确: " + str);
                                    }

                                    fex = fex.replaceAll("\r|\n|\t", "").trim();
                                    fex = fex.replaceAll("，", "-1-");
                                    fex = fex.replaceAll(",", "-1-");
                                    fex = fex.replaceAll("。", "-1-");
                                    String[] fexlist = fex.split("-1-");

                                    x:
                                    for (String s : fexlist) {
                                        if (s == null) {
                                            continue x;
                                        }
                                        s = s.trim();
                                        if (s.length() < 1) {
                                            continue x;
                                        }

                                        String x = getNumbers(s);
                                        if (x != null && x.length() > 0 && !"null".equals(x)) {
                                            String header = s.replace(x, "[X]");
                                            if (header.startsWith("100日元对人民币")) {
                                                header = "100日元对人民币[X]元";//部分格式为 100日元对人民币为[XXX]    100日元对人民币为[XXX]元  统一
                                            }
                                            if (header.equals("[X]")) {
                                                continue x;
                                            }

                                            String[] temp = header.split("对");
                                            if (temp.length > 1 && temp[0].contains("人民币")) {
                                                header = temp[1] + "对" + temp[0];
                                            }

                                            save(date, header, x);
                                        } else {
                                            debug("出错了 汇率==null " + date);
                                        }
                                    }

                                    DbUtil.getDbUtil().insertHistory(subWeb.getEngine().getLocation());
                                    msgView.setText("本次抓取到: " + date);
                                    getSubData();
                                } else {
                                    debug("出错了 date==null");
                                }
                            } else {
                                debug("出错了 str==null");
                            }
                        }
                    } else {
                        debug("出错了 div id=zoom==null");
                        subWeb.getEngine().reload();
                    }
                } else {
//                    debug("出错了 status !=success newvalue=" + newValue + "  oldvalue=" + oldValue+ " observable="+observable.getValue());
                }
            }
        });

        getPageData();

    }

    static void debug(String s) {
        System.out.println(s);
    }

    DateFormat format = new SimpleDateFormat("yyyy年MM月dd日");

    Date lastPostion;


    boolean finish;

    /**
     * @param date
     * @param topic
     * @param rate
     */
    void save(String date, String topic, String rate) {
        date = date.trim();
        try {
            Date d = format.parse(date);
            date = format.format(d);
            DbUtil.getDbUtil().insertRate(date, topic, rate);
            if (lastPostion != null && (d.compareTo(lastPostion) <= 0)) {
                finish = true;
                generateXLS();
                return;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    String pageUrl;

    private void getPageData() {
        if (page < maxPage) {
            page++;

            pageUrl = (history ? "http://www.pbc.gov.cn/zhengcehuobisi/125207/125217/125925/2929357/b101a6a1/index" : "http://www.pbc.gov.cn/zhengcehuobisi/125207/125217/125925/17105/index") + page + ".html";

            webView.getEngine().load(pageUrl);

        } else {

            if (!history) {
                msgView.setText("抓取 历史公告 首页数据...");
                getHistoryData();
            } else {
                generateXLS();

            }
        }
    }

    private void generateXLS() {
        msgView.setText("全部抓取完毕 正在生成Excel  请勿关闭");
        new Thread(new Runnable() {
            @Override
            public void run() {
                FileUtil.getUtil().save();

                Platform.runLater(new Runnable() {
                    @Override
                    public void run() {
                        button.setVisible(true);
                        msgView.setVisible(false);
                        msgView.setText("");
                    }
                });
            }
        }).start();
    }

    boolean history;

    private void getHistoryData() {
        initParam();
        history = true;
        getPageData();
    }

    int currentIndex = -1;
    int totalDays = 0;
    int perPage = 1;

    ArrayList<Node> subList = new ArrayList<>(20);

    private void getSubData() {
        if (finish) {
            generateXLS();
            return;
        }

        currentIndex++;
        if (currentIndex >= subList.size()) {
            currentIndex = -1;
            getPageData();
            return;
        }
        Node item = subList.get(currentIndex);
        if (item == null) {
            return;
        }
        Node value = item.getAttributes().getNamedItem("href");
        if (lastPostion != null) {
            String title = item.getTextContent();
            if (title != null) {
                if (title.contains("中国外汇")) {
                    String date = title.substring(0, title.indexOf("中国外汇")).trim();
                    try {
                        Date current = format.parse(date);
                        if (lastPostion.compareTo(current) >= 0) {
                            finish = true;
                            generateXLS();
                            return;
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }

                }
            }
        }
        if (value != null) {
            String url = "http://www.pbc.gov.cn" + value.getTextContent();

            if (!DbUtil.getDbUtil().queryHistoryExsist(url)) {
                subWeb.getEngine().load(url);
            } else {
                msgView.setText("此页数据已存在记录中 " + url.toLowerCase().replaceAll("http://www.pbc.gov.cn/zhengcehuobisi/125207/125217/125925/", "").replaceAll("/index.html", ""));
                getSubData();
            }

        }
    }

    //截取数字
    public String getNumbers(String content) {
        content = content.replaceAll("[\\u4e00-\\u9fa5]", " ");
        String[] f = content.split(" ");
        String x = null;
        ArrayList<String> xlist = new ArrayList<>(2);
        for (String str : f) {

            if (str.trim().length() > 0) {
                if (str.contains(".")) {
                    x = str;
                }
                xlist.add(str.trim());
            }
        }

        if (x == null && !xlist.isEmpty()) {
            x = xlist.get(xlist.size() - 1);
        }

        return x;
    }


    public static void main(String[] args) {
        launch(args);
    }

}
