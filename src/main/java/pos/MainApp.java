package pos;

/**
 * ============================================================
 *  APLIKASI POS UMKM TAHU BULAT — Mang Udin
 * ============================================================
 *
 *  ALGORITMA PROGRAM:
 *  1. Aplikasi dimulai → inisialisasi data menu & tampilkan UI
 *  2. User memilih menu dari ListView (dengan live search)
 *  3. User mengisi jumlah menggunakan Spinner
 *  4. User klik "Tambah ke Nota"
 *     → validasi input (menu dipilih? qty > 0?)
 *     → hitung subtotal = harga × jumlah
 *     → tambah baris ke TableView nota transaksi
 *     → update total tagihan otomatis
 *     → Java Sound API: putar beep konfirmasi 880Hz
 *  5. User mengisi nominal bayar di field "Bayar"
 *     → kembalian = bayar - total dihitung otomatis
 *  6. User klik "Simpan Pembayaran"
 *     → validasi uang mencukupi
 *     → generate struk (tanggal dari LocalDateTime)
 *     → tampilkan dialog struk transaksi
 *     → Java Sound API: putar melodi sukses Do-Mi-Sol-Do
 *     → reset form untuk transaksi berikutnya
 *
 *  FITUR LENGKAP:
 *  ✔ Java Sound API  – beep klik & melodi transaksi berhasil
 *  ✔ Gambar Aplikasi – ilustrasi tahu bulat (JavaFX Canvas)
 *  ✔ Calendar & Date – jam real-time + tanggal transaksi ID
 *  ✔ Pencarian Barang – live filter ListView menu
 *  ✔ Perhitungan      – subtotal = qty x harga, total otomatis
 * ============================================================
 */

import javafx.application.Application;
import javafx.application.Platform;
import javafx.beans.property.*;
import javafx.collections.*;
import javafx.geometry.*;
import javafx.scene.*;
import javafx.scene.canvas.*;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.*;
import javafx.scene.paint.*;
import javafx.scene.text.*;
import javafx.stage.*;
import javafx.animation.*;
import javafx.util.Duration;

import java.time.*;
import java.time.format.*;
import java.util.*;

import javax.sound.sampled.*;

public class MainApp extends Application {

    // =============================================
    //  MODEL DATA
    // =============================================

    public static class MenuItem {
        private final String nama;
        private final int    harga;
        private final String kategori;

        public MenuItem(String nama, int harga, String kategori) {
            this.nama     = nama;
            this.harga    = harga;
            this.kategori = kategori;
        }
        public String getNama()     { return nama; }
        public int    getHarga()    { return harga; }
        public String getKategori() { return kategori; }

        @Override
        public String toString() { return nama + "  —  " + rpStr(harga); }
    }

    public static class NotaItem {
        private final SimpleIntegerProperty no;
        private final SimpleStringProperty  nama;
        private final SimpleIntegerProperty harga;
        private final SimpleIntegerProperty qty;
        private final SimpleLongProperty    subTotal;

        public NotaItem(int no, MenuItem m, int qty) {
            this.no       = new SimpleIntegerProperty(no);
            this.nama     = new SimpleStringProperty(m.getNama());
            this.harga    = new SimpleIntegerProperty(m.getHarga());
            this.qty      = new SimpleIntegerProperty(qty);
            this.subTotal = new SimpleLongProperty((long) m.getHarga() * qty);
        }
        public int    getNo()          { return no.get(); }
        public String getNama()        { return nama.get(); }
        public int    getHarga()       { return harga.get(); }
        public int    getQty()         { return qty.get(); }
        public long   getSubTotal()    { return subTotal.get(); }
        public String getHargaStr()    { return rpStr(harga.get()); }
        public String getSubTotalStr() { return rpStr(subTotal.get()); }

        public SimpleIntegerProperty noProperty()   { return no; }
        public SimpleStringProperty  namaProperty() { return nama; }
        public SimpleIntegerProperty qtyProperty()  { return qty; }
    }

    // =============================================
    //  STATE
    // =============================================
    private final List<MenuItem>           semuaMenu    = new ArrayList<>();
    private final ObservableList<MenuItem> filteredMenu = FXCollections.observableArrayList();
    private final ObservableList<NotaItem> nota         = FXCollections.observableArrayList();

    private MenuItem menuDipilih = null;
    private int      invCounter  = 1;
    private int      totalTrx    = 0;
    private long     totalOmzet  = 0;

    // UI refs
    private Label   lblClock, lblInvNo, lblStatus;
    private Label   lblSubTotal, lblTotal, lblKembali;
    private Label   lblSelMenu, lblSelHarga;
    private TextField tfSearch, tfBayar, tfDiskon;
    private Spinner<Integer> spQty;
    private ListView<MenuItem>   listMenu;
    private TableView<NotaItem>  tabelNota;

    // =============================================
    //  ENTRY
    // =============================================
    public static void main(String[] args) { launch(args); }

    @Override
    public void start(Stage stage) {
        inisialisasiMenu();
        stage.setTitle("POS Tahu Bulat — Mang Udin | Bandung");
        stage.setMinWidth(1000);
        stage.setMinHeight(660);
        BorderPane root = buildRoot();
        Scene scene = new Scene(root, 1100, 700);
        scene.getRoot().setStyle("-fx-font-family:'Segoe UI',Arial,sans-serif;");
        stage.setScene(scene);
        stage.show();
        FadeTransition ft = new FadeTransition(Duration.millis(400), root);
        ft.setFromValue(0); ft.setToValue(1); ft.play();
        startClock();
    }

    private void inisialisasiMenu() {
        semuaMenu.add(new MenuItem("Tahu Bulat Original",     2000, "Original"));
        semuaMenu.add(new MenuItem("Tahu Bulat Pedas",        2500, "Pedas"));
        semuaMenu.add(new MenuItem("Tahu Bulat Keju",         3000, "Spesial"));
        semuaMenu.add(new MenuItem("Tahu Bulat Balado",       2500, "Pedas"));
        semuaMenu.add(new MenuItem("Tahu Bulat BBQ",          3000, "Spesial"));
        semuaMenu.add(new MenuItem("Tahu Bulat Jagung Manis", 2500, "Original"));
        semuaMenu.add(new MenuItem("Tahu Bulat Isi Ayam",     3500, "Spesial"));
        semuaMenu.add(new MenuItem("Tahu Bulat Rumput Laut",  3000, "Spesial"));
        semuaMenu.add(new MenuItem("Paket 5 Pcs Original",    9000, "Paket"));
        semuaMenu.add(new MenuItem("Paket 10 Pcs Mix",       17000, "Paket"));
        filteredMenu.addAll(semuaMenu);
    }

    // =============================================
    //  ROOT
    // =============================================
    private BorderPane buildRoot() {
        BorderPane root = new BorderPane();
        root.setLeft(buildSidebar());
        VBox mainArea = new VBox(0);
        mainArea.setStyle("-fx-background-color:#F4F6F9;");
        mainArea.getChildren().addAll(buildTopBar(), buildContent());
        VBox.setVgrow(buildContent(), Priority.ALWAYS);
        root.setCenter(mainArea);
        root.setBottom(buildStatusBar());
        // rebuild center properly
        VBox center = new VBox(0);
        center.setStyle("-fx-background-color:#F4F6F9;");
        ScrollPane sp = buildContent();
        center.getChildren().addAll(buildTopBar(), sp);
        VBox.setVgrow(sp, Priority.ALWAYS);
        root.setCenter(center);
        return root;
    }

    // =============================================
    //  SIDEBAR
    // =============================================
    private VBox buildSidebar() {
        VBox sb = new VBox(0);
        sb.setPrefWidth(198);
        sb.setStyle("-fx-background-color:#2C3E6B;");

        // Brand
        HBox brandBox = new HBox(8);
        brandBox.setPadding(new Insets(14, 12, 14, 12));
        brandBox.setAlignment(Pos.CENTER_LEFT);
        brandBox.setStyle("-fx-border-color:#3D5080; -fx-border-width:0 0 1 0;");
        Canvas ic = makeTahuIcon(38, 36);
        Label lb = new Label("Tahu Bulat POS");
        lb.setStyle("-fx-font-size:14px; -fx-font-weight:bold; -fx-text-fill:white;");
        Label ls = new Label("UMKM Mang Udin");
        ls.setStyle("-fx-font-size:10px; -fx-text-fill:#8FA3CC;");
        brandBox.getChildren().addAll(ic, new VBox(1, lb, ls));

        // User
        HBox ub = new HBox(8);
        ub.setPadding(new Insets(10, 12, 10, 12));
        ub.setAlignment(Pos.CENTER_LEFT);
        ub.setStyle("-fx-border-color:#3D5080; -fx-border-width:0 0 1 0;");
        Label av = new Label("MU");
        av.setStyle("-fx-background-color:#E67E22; -fx-text-fill:white; -fx-font-weight:bold;" +
                    "-fx-font-size:12px; -fx-min-width:30px; -fx-min-height:30px;" +
                    "-fx-alignment:CENTER; -fx-background-radius:15;");
        ub.getChildren().addAll(av, new VBox(1,
            styledLabel("Mang Udin", "11px", "#CDD8EE", true),
            styledLabel("Kasir",     "10px", "#7A93BE", false)));

        // Nav
        VBox nav = new VBox(0);
        nav.setPadding(new Insets(6, 0, 0, 0));
        VBox.setVgrow(nav, Priority.ALWAYS);
        addNavSection(nav, "MENU UTAMA");
        addNavItem(nav, "Dashboard",          false);
        addNavItem(nav, "Kasir / Transaksi",  true);
        addNavSection(nav, "DATA");
        addNavItem(nav, "Penjualan",          false);
        addNavItem(nav, "Produk / Menu",      false);
        addNavItem(nav, "Pelanggan",          false);
        addNavSection(nav, "LAPORAN");
        addNavItem(nav, "Laporan Penjualan",  false);
        addNavSection(nav, "PENGATURAN");
        addNavItem(nav, "Pengaturan",         false);

        // Stat
        VBox stat = new VBox(3);
        stat.setPadding(new Insets(10, 12, 10, 12));
        stat.setStyle("-fx-border-color:#3D5080; -fx-border-width:1 0 0 0;");
        stat.getChildren().addAll(
            styledLabel("Sesi Ini", "9px", "#5A7AB5", true),
            styledLabel("Transaksi : 0",  "11px", "#A8BCD8", false),
            styledLabel("Omzet     : Rp 0","11px","#A8BCD8", false));
        stat.getChildren().get(1).setId("lblStatTrx");
        stat.getChildren().get(2).setId("lblStatOmzet");

        sb.getChildren().addAll(brandBox, ub, nav, stat);
        return sb;
    }

    private void addNavSection(VBox nav, String t) {
        Label l = new Label(t);
        l.setStyle("-fx-font-size:9px; -fx-text-fill:#5A7AB5; -fx-padding:8 12 3 12;" +
                   "-fx-font-weight:bold;");
        nav.getChildren().add(l);
    }

    private void addNavItem(VBox nav, String title, boolean active) {
        HBox item = new HBox();
        item.setPadding(new Insets(8, 12, 8, 12));
        item.setAlignment(Pos.CENTER_LEFT);
        item.setCursor(javafx.scene.Cursor.HAND);
        Label l = new Label(title);
        l.setStyle("-fx-font-size:12px; -fx-text-fill:" + (active ? "white" : "#A8BCD8") + ";");
        item.getChildren().add(l);
        if (active) {
            item.setStyle("-fx-background-color:#1A6FB5;");
        } else {
            item.setOnMouseEntered(e -> item.setStyle("-fx-background-color:#3D5080;"));
            item.setOnMouseExited(e  -> item.setStyle(""));
        }
        nav.getChildren().add(item);
    }

    // =============================================
    //  TOPBAR
    // =============================================
    private HBox buildTopBar() {
        HBox bar = new HBox();
        bar.setPadding(new Insets(0, 16, 0, 16));
        bar.setAlignment(Pos.CENTER_LEFT);
        bar.setMinHeight(50);
        bar.setMaxHeight(50);
        bar.setStyle("-fx-background-color:white; -fx-border-color:#E2E6EA;" +
                     "-fx-border-width:0 0 1 0;");
        VBox tt = new VBox(1);
        Label t1 = new Label("Transaksi Kasir");
        t1.setStyle("-fx-font-size:15px; -fx-font-weight:bold; -fx-text-fill:#2C3E6B;");
        Label t2 = new Label("Home  /  Kasir  /  Transaksi Baru");
        t2.setStyle("-fx-font-size:10px; -fx-text-fill:#95A5B8;");
        tt.getChildren().addAll(t1, t2);
        Region sp = new Region(); HBox.setHgrow(sp, Priority.ALWAYS);
        lblClock = new Label();
        lblClock.setStyle("-fx-font-size:11px; -fx-font-family:Consolas,monospace;" +
                          "-fx-text-fill:#7A8FA8; -fx-background-color:#F4F6F9;" +
                          "-fx-padding:4 10; -fx-background-radius:5;");
        bar.getChildren().addAll(tt, sp, lblClock);
        return bar;
    }

    // =============================================
    //  CONTENT
    // =============================================
    private ScrollPane buildContent() {
        HBox body = new HBox(12);
        body.setPadding(new Insets(14));
        body.setAlignment(Pos.TOP_LEFT);

        VBox left  = buildLeft();
        VBox right = buildRight();
        HBox.setHgrow(left, Priority.ALWAYS);
        body.getChildren().addAll(left, right);

        ScrollPane sp = new ScrollPane(body);
        sp.setFitToWidth(true);
        sp.setStyle("-fx-background:transparent; -fx-background-color:transparent;");
        return sp;
    }

    // =============================================
    //  PANEL KIRI
    // =============================================
    private VBox buildLeft() {
        VBox v = new VBox(12);

        HBox top = new HBox(12);
        top.setAlignment(Pos.TOP_LEFT);
        VBox cGambar = buildCardGambar();
        VBox cPilih  = buildCardPilihMenu();
        HBox.setHgrow(cPilih, Priority.ALWAYS);
        top.getChildren().addAll(cGambar, cPilih);

        v.getChildren().addAll(top, buildCardNota());
        return v;
    }

    // -- Card Gambar Tahu --------------------------
    private VBox buildCardGambar() {
        VBox c = makeCard();
        c.setPrefWidth(205);
        c.setMinWidth(205);
        c.getChildren().add(makeCardHeader("Produk Kami"));

        VBox body = new VBox(8);
        body.setPadding(new Insets(12));
        body.setAlignment(Pos.CENTER);

        // GAMBAR TAHU BULAT — JavaFX Canvas
        Canvas canvas = buildTahuCanvas(181, 148);

        Label hm = new Label("Mulai dari Rp 2.000");
        hm.setStyle("-fx-font-size:11px; -fx-font-weight:bold; -fx-text-fill:#1A6FB5;");

        Separator sep = new Separator();

        Label infoT = new Label("Info Toko");
        infoT.setStyle("-fx-font-size:11px; -fx-font-weight:bold; -fx-text-fill:#5A6A80;");

        VBox info = new VBox(3);
        for (String s : new String[]{
                "Jl. Cilimus No. 17, Bandung",
                "Telp: 0812-3456-7890",
                "Jam: 09.00 - 21.00 WIB",
                "Bayar: Tunai / QRIS"}) {
            Label l = new Label(s);
            l.setStyle("-fx-font-size:10px; -fx-text-fill:#5A6A80;");
            l.setWrapText(true);
            info.getChildren().add(l);
        }
        body.getChildren().addAll(canvas, hm, sep, infoT, info);
        c.getChildren().add(body);
        return c;
    }

    /**
     * FITUR GAMBAR: Ilustrasi tahu bulat programatik dengan JavaFX Canvas
     * Menggunakan RadialGradient untuk efek crispy golden-brown
     */
    private Canvas buildTahuCanvas(double w, double h) {
        Canvas cv = new Canvas(w, h);
        GraphicsContext gc = cv.getGraphicsContext2D();
        gc.setFill(Color.web("#F8F9FB"));
        gc.fillRoundRect(0, 0, w, h, 8, 8);

        double cx = w / 2, cy = h * 0.48;

        // Piring
        gc.setFill(Color.web("#F5F5F5"));
        gc.setStroke(Color.web("#DEDEDE"));
        gc.setLineWidth(1);
        gc.fillOval(cx - 72, cy + 26, 144, 42);
        gc.strokeOval(cx - 72, cy + 26, 144, 42);

        // Bayangan
        gc.setFill(Color.web("#BF8600", 0.1));
        gc.fillOval(cx - 46, cy + 34, 56, 14);
        gc.fillOval(cx - 8, cy + 34, 56, 14);

        drawTahu(gc, cx - 50, cy + 4,  48);
        drawTahu(gc, cx + 4,  cy + 4,  48);
        drawTahu(gc, cx - 30, cy - 10, 58);

        // Uap
        gc.setStroke(Color.web("#E65100", 0.35));
        gc.setLineWidth(1.3);
        for (int i = -1; i <= 1; i++) {
            double bx = cx + i * 13;
            gc.beginPath();
            gc.moveTo(bx, cy - 14);
            gc.bezierCurveTo(bx - 3, cy - 22, bx + 3, cy - 30, bx, cy - 38);
            gc.stroke();
        }

        gc.setFont(Font.font("Segoe UI", FontWeight.BOLD, 10));
        gc.setFill(Color.web("#1A6FB5"));
        gc.fillText("Crispy  Gurih  Hangat", cx - 52, h - 5);
        return cv;
    }

    private void drawTahu(GraphicsContext gc, double x, double y, double sz) {
        double r = sz / 2;
        RadialGradient g = new RadialGradient(
                0, 0, x + r * 0.35, y + r * 0.3, r, false, CycleMethod.NO_CYCLE,
                new Stop(0.0,  Color.web("#FFFDE7")),
                new Stop(0.35, Color.web("#E8B84B")),
                new Stop(1.0,  Color.web("#9A6A00")));
        gc.setFill(g);
        gc.fillOval(x, y, sz, sz);
        gc.setStroke(Color.web("#7A4E00"));
        gc.setLineWidth(0.8);
        gc.strokeOval(x, y, sz, sz);
        gc.setFill(Color.web("#7A4E00", 0.55));
        gc.fillOval(x + r * 0.5,  y + r * 0.42, 3.2, 3.2);
        gc.fillOval(x + r * 1.12, y + r * 0.72, 2.5, 2.5);
        gc.fillOval(x + r * 0.72, y + r * 1.15, 2.5, 2.5);
    }

    private Canvas makeTahuIcon(double w, double h) {
        Canvas cv = new Canvas(w, h);
        GraphicsContext gc = cv.getGraphicsContext2D();
        drawTahu(gc, 2, 3, w * 0.55);
        drawTahu(gc, w * 0.4, 1, w * 0.55);
        return cv;
    }

    // -- Card Pilih Menu ---------------------------
    private VBox buildCardPilihMenu() {
        VBox c = makeCard();
        c.getChildren().add(makeCardHeader("Pilih Menu"));

        VBox body = new VBox(10);
        body.setPadding(new Insets(12));

        // FITUR PENCARIAN — live filter
        HBox sr = new HBox(6);
        sr.setAlignment(Pos.CENTER_LEFT);
        tfSearch = new TextField();
        tfSearch.setPromptText("Cari nama menu tahu bulat...");
        tfSearch.setPrefHeight(34);
        tfSearch.setStyle(iStyle());
        HBox.setHgrow(tfSearch, Priority.ALWAYS);
        tfSearch.textProperty().addListener((o, old, nw) -> filterMenu(nw));

        Button btnC = btn("Cari",  "#1A6FB5");
        Button btnX = btn("Bersih","#E74C3C");
        btnC.setOnAction(e -> filterMenu(tfSearch.getText()));
        btnX.setOnAction(e -> { tfSearch.clear(); filterMenu(""); });
        sr.getChildren().addAll(tfSearch, btnC, btnX);

        // ListView
        listMenu = new ListView<>(filteredMenu);
        listMenu.setPrefHeight(196);
        listMenu.setStyle("-fx-background-color:white; -fx-border-color:#DCE1EA; -fx-border-radius:5;");
        listMenu.setCellFactory(lv -> new ListCell<>() {
            @Override protected void updateItem(MenuItem item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) { setText(null); setGraphic(null); setStyle(""); return; }
                HBox row = new HBox(8);
                row.setPadding(new Insets(4, 10, 4, 10));
                row.setAlignment(Pos.CENTER_LEFT);
                Label badge = new Label(item.getKategori());
                badge.setStyle("-fx-background-color:" + badgeClr(item.getKategori()) +
                               "; -fx-text-fill:white; -fx-font-size:9px; -fx-padding:2 5;" +
                               "-fx-background-radius:4; -fx-font-weight:bold;");
                Label nm = new Label(item.getNama());
                nm.setStyle("-fx-font-size:12px; -fx-font-weight:bold; -fx-text-fill:#2C3E6B;");
                Region sp = new Region(); HBox.setHgrow(sp, Priority.ALWAYS);
                Label hr = new Label(rpStr(item.getHarga()));
                hr.setStyle("-fx-font-size:12px; -fx-font-weight:bold; -fx-text-fill:#1A6FB5;");
                row.getChildren().addAll(badge, nm, sp, hr);
                setGraphic(row); setText(null);
                setOnMouseEntered(e -> { if (!isSelected()) setStyle("-fx-background-color:#EBF3FB;"); });
                setOnMouseExited(e  -> { if (!isSelected()) setStyle(""); });
            }
        });
        listMenu.getSelectionModel().selectedItemProperty().addListener((o, old, nw) -> {
            menuDipilih = nw;
            if (nw != null) {
                lblSelMenu.setText(nw.getNama());
                lblSelMenu.setStyle("-fx-font-size:12px; -fx-font-weight:bold; -fx-text-fill:#2C3E6B;");
                lblSelHarga.setText(rpStr(nw.getHarga()));
            }
        });

        // Info dipilih
        HBox infoSel = new HBox(8);
        infoSel.setStyle("-fx-background-color:#EBF3FB; -fx-background-radius:5; -fx-padding:6 10;");
        infoSel.setAlignment(Pos.CENTER_LEFT);
        Label lbDipilih = new Label("Menu:");
        lbDipilih.setStyle("-fx-font-size:11px; -fx-text-fill:#5A6A80;");
        lblSelMenu  = new Label("— belum dipilih —");
        lblSelMenu.setStyle("-fx-font-size:12px; -fx-text-fill:#9E9E9E;");
        Region g = new Region(); HBox.setHgrow(g, Priority.ALWAYS);
        lblSelHarga = new Label("");
        lblSelHarga.setStyle("-fx-font-size:12px; -fx-font-weight:bold; -fx-text-fill:#1A6FB5;");
        infoSel.getChildren().addAll(lbDipilih, lblSelMenu, g, lblSelHarga);

        // Qty + tombol tambah
        HBox qtyRow = new HBox(8);
        qtyRow.setAlignment(Pos.CENTER_LEFT);
        Label qLbl = new Label("Jumlah :");
        qLbl.setStyle("-fx-font-size:12px; -fx-font-weight:bold; -fx-text-fill:#5A6A80;");
        spQty = new Spinner<>(new SpinnerValueFactory.IntegerSpinnerValueFactory(1, 999, 1));
        spQty.setEditable(true);
        spQty.setPrefWidth(80);
        Region sp2 = new Region(); HBox.setHgrow(sp2, Priority.ALWAYS);
        Button btnAdd = new Button("+ Tambah ke Nota");
        btnAdd.setStyle(btnStyle("#1A6FB5") + "-fx-font-size:12px; -fx-padding:8 14;");
        btnAdd.setOnAction(e -> tambahKeNota());
        qtyRow.getChildren().addAll(qLbl, spQty, sp2, btnAdd);

        body.getChildren().addAll(sr, listMenu, infoSel, new Separator(), qtyRow);
        c.getChildren().add(body);
        return c;
    }

    // -- Card Nota ---------------------------------
    private VBox buildCardNota() {
        VBox c = makeCard();

        HBox hdr = new HBox(8);
        hdr.setPadding(new Insets(10, 12, 10, 12));
        hdr.setAlignment(Pos.CENTER_LEFT);
        hdr.setStyle("-fx-background-color:#F8F9FB; -fx-border-color:#E2E6EA;" +
                     "-fx-border-width:0 0 1 0; -fx-background-radius:8 8 0 0;");
        Label ht = new Label("Nota Transaksi");
        ht.setStyle("-fx-font-size:13px; -fx-font-weight:bold; -fx-text-fill:#2C3E6B;");
        Region sp = new Region(); HBox.setHgrow(sp, Priority.ALWAYS);
        Label invLbl = new Label("No. Invoice :");
        invLbl.setStyle("-fx-font-size:11px; -fx-text-fill:#7A8FA8;");
        lblInvNo = new Label("TBU-2025-0001");
        lblInvNo.setStyle("-fx-font-size:13px; -fx-font-weight:bold; -fx-text-fill:#2C3E6B;");
        Button btnHapus = btn("Hapus Item", "#E74C3C");
        btnHapus.setOnAction(e -> hapusItem());
        hdr.getChildren().addAll(ht, sp, invLbl, lblInvNo, btnHapus);
        c.getChildren().add(hdr);

        // TableView Nota
        tabelNota = new TableView<>(nota);
        tabelNota.setPrefHeight(185);
        tabelNota.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        tabelNota.setStyle("-fx-background-color:white;");
        tabelNota.setPlaceholder(
            new Label("Nota kosong. Pilih menu lalu klik \"+ Tambah ke Nota\"."));

        TableColumn<NotaItem, Integer> cNo = col("No.", "no", 38);
        cNo.setStyle("-fx-alignment:CENTER;");
        TableColumn<NotaItem, String> cNama = new TableColumn<>("Nama Menu");
        cNama.setCellValueFactory(d -> d.getValue().namaProperty());
        cNama.setPrefWidth(190);
        TableColumn<NotaItem, String> cHrg = new TableColumn<>("Harga");
        cHrg.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getHargaStr()));
        cHrg.setPrefWidth(100); cHrg.setStyle("-fx-alignment:CENTER_RIGHT;");
        TableColumn<NotaItem, Integer> cQty = col("Qty", "qty", 46);
        cQty.setStyle("-fx-alignment:CENTER;");
        // PERHITUNGAN: Sub Total = Harga x Qty ditampilkan di kolom ini
        TableColumn<NotaItem, String> cSub = new TableColumn<>("Sub Total");
        cSub.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getSubTotalStr()));
        cSub.setPrefWidth(115); cSub.setStyle("-fx-alignment:CENTER_RIGHT;");

        tabelNota.getColumns().addAll(cNo, cNama, cHrg, cQty, cSub);
        c.getChildren().add(tabelNota);
        return c;
    }

    // =============================================
    //  PANEL KANAN
    // =============================================
    private VBox buildRight() {
        VBox v = new VBox(12);
        v.setPrefWidth(305);
        v.setMinWidth(305);
        v.getChildren().addAll(buildCardPelanggan(), buildCardBayar());
        return v;
    }

    // -- Card Pelanggan & Tanggal ------------------
    private VBox buildCardPelanggan() {
        VBox c = makeCard();
        c.getChildren().add(makeCardHeader("Pelanggan & Tanggal Transaksi"));
        VBox body = new VBox(8);
        body.setPadding(new Insets(12));

        // FITUR CALENDAR & DATE
        Label lTgl = new Label("Tanggal Transaksi");
        lTgl.setStyle("-fx-font-size:11px; -fx-font-weight:bold; -fx-text-fill:#5A6A80;");
        Label valTgl = new Label();
        valTgl.setId("lblTanggal");
        valTgl.setStyle("-fx-font-size:11px; -fx-text-fill:#2C3E6B; -fx-font-weight:bold;" +
                        "-fx-background-color:#EBF3FB; -fx-padding:5 10; -fx-background-radius:5;");
        setTanggal(valTgl);

        Label lPel = new Label("Pelanggan");
        lPel.setStyle("-fx-font-size:11px; -fx-font-weight:bold; -fx-text-fill:#5A6A80;");
        ComboBox<String> cb = new ComboBox<>(FXCollections.observableArrayList(
                "Umum / Walk-in","Langganan A","Kantin Sekolah","Dari Marketplace"));
        cb.setValue("Umum / Walk-in");
        cb.setMaxWidth(Double.MAX_VALUE);
        cb.setStyle("-fx-border-color:#DCE1EA; -fx-border-radius:5; -fx-font-size:12px;");

        body.getChildren().addAll(lTgl, valTgl, lPel, cb);
        c.getChildren().add(body);
        return c;
    }

    // -- Card Bayar --------------------------------
    private VBox buildCardBayar() {
        VBox c = makeCard();
        c.getChildren().add(makeCardHeader("Rincian Pembayaran"));
        VBox body = new VBox(0);
        body.setPadding(new Insets(12));

        lblSubTotal = addPayRow(body, "Sub Total", false);

        // Diskon
        HBox dRow = new HBox(8);
        dRow.setPadding(new Insets(6, 0, 6, 0));
        dRow.setAlignment(Pos.CENTER_LEFT);
        dRow.setStyle("-fx-border-color:#F0F2F5; -fx-border-width:0 0 0.5 0;");
        Label dl = new Label("Diskon (Rp)");
        dl.setStyle("-fx-font-size:12px; -fx-font-weight:bold; -fx-text-fill:#5A6A80;");
        Region dsp = new Region(); HBox.setHgrow(dsp, Priority.ALWAYS);
        tfDiskon = new TextField("0");
        tfDiskon.setPrefWidth(110);
        tfDiskon.setStyle(iStyle() + "-fx-alignment:CENTER_RIGHT; -fx-font-size:12px;");
        tfDiskon.textProperty().addListener((o, old, nw) -> {
            if (!nw.matches("\\d*")) tfDiskon.setText(nw.replaceAll("\\D", ""));
            hitung();
        });
        dRow.getChildren().addAll(dl, dsp, tfDiskon);
        body.getChildren().add(dRow);

        // Total Tagihan box
        HBox tBox = new HBox(8);
        tBox.setAlignment(Pos.CENTER_LEFT);
        tBox.setStyle("-fx-background-color:#EBF3FB; -fx-background-radius:6; -fx-padding:8 10;");
        Label tl = new Label("Total Tagihan");
        tl.setStyle("-fx-font-size:12px; -fx-font-weight:bold; -fx-text-fill:#1560A0;");
        Region tsp = new Region(); HBox.setHgrow(tsp, Priority.ALWAYS);
        lblTotal = new Label("Rp 0");
        lblTotal.setStyle("-fx-font-size:17px; -fx-font-weight:bold; -fx-text-fill:#1A6FB5;");
        tBox.getChildren().addAll(tl, tsp, lblTotal);
        VBox.setMargin(tBox, new Insets(6, 0, 6, 0));
        body.getChildren().add(tBox);

        // Bayar
        HBox bRow = new HBox(8);
        bRow.setPadding(new Insets(5, 0, 5, 0));
        bRow.setAlignment(Pos.CENTER_LEFT);
        Label bl = new Label("Bayar (Rp)");
        bl.setStyle("-fx-font-size:12px; -fx-font-weight:bold; -fx-text-fill:#5A6A80;");
        Region bsp = new Region(); HBox.setHgrow(bsp, Priority.ALWAYS);
        tfBayar = new TextField("0");
        tfBayar.setPrefWidth(130);
        tfBayar.setStyle(iStyle() + "-fx-alignment:CENTER_RIGHT; -fx-font-size:13px;" +
                         "-fx-font-weight:bold; -fx-text-fill:#1A6FB5; -fx-border-color:#1A6FB5;");
        tfBayar.textProperty().addListener((o, old, nw) -> {
            if (!nw.matches("\\d*")) tfBayar.setText(nw.replaceAll("\\D", ""));
            hitung();
        });
        bRow.getChildren().addAll(bl, bsp, tfBayar);
        body.getChildren().add(bRow);

        // Kembalian
        HBox kBox = new HBox(8);
        kBox.setAlignment(Pos.CENTER_LEFT);
        kBox.setStyle("-fx-background-color:#EAF9F1; -fx-background-radius:6; -fx-padding:8 10;");
        Label kl = new Label("Kembalian");
        kl.setStyle("-fx-font-size:12px; -fx-font-weight:bold; -fx-text-fill:#1E8449;");
        Region ksp = new Region(); HBox.setHgrow(ksp, Priority.ALWAYS);
        lblKembali = new Label("Rp 0");
        lblKembali.setStyle("-fx-font-size:15px; -fx-font-weight:bold; -fx-text-fill:#27AE60;");
        kBox.getChildren().addAll(kl, ksp, lblKembali);
        VBox.setMargin(kBox, new Insets(6, 0, 10, 0));
        body.getChildren().add(kBox);

        // Tombol
        Button btnBayar = new Button("Simpan Pembayaran");
        btnBayar.setMaxWidth(Double.MAX_VALUE);
        btnBayar.setStyle(btnStyle("#27AE60") + "-fx-font-size:14px; -fx-padding:11 0;");
        btnBayar.setOnAction(e -> prosesBayar());

        Button btnReset = new Button("Transaksi Baru");
        btnReset.setMaxWidth(Double.MAX_VALUE);
        btnReset.setStyle(btnStyle("#95A5B6") + "-fx-font-size:12px; -fx-padding:8 0;");
        btnReset.setOnAction(e -> resetAll());
        VBox.setMargin(btnReset, new Insets(6, 0, 0, 0));

        body.getChildren().addAll(btnBayar, btnReset);
        c.getChildren().add(body);
        return c;
    }

    // =============================================
    //  STATUS BAR
    // =============================================
    private HBox buildStatusBar() {
        HBox bar = new HBox(8);
        bar.setPadding(new Insets(5, 14, 5, 14));
        bar.setAlignment(Pos.CENTER_LEFT);
        bar.setStyle("-fx-background-color:#2C3E6B;");
        lblStatus = new Label("Sistem siap — Selamat berjualan!");
        lblStatus.setStyle("-fx-font-size:10px; -fx-text-fill:#A8BCD8; -fx-font-family:Consolas;");
        Region sp = new Region(); HBox.setHgrow(sp, Priority.ALWAYS);
        Label ver = new Label("POS Tahu Bulat v1.0  |  Mang Udin, Bandung");
        ver.setStyle("-fx-font-size:10px; -fx-text-fill:#5A7AB5;");
        bar.getChildren().addAll(lblStatus, sp, ver);
        return bar;
    }

    // =============================================
    //  LOGIKA BISNIS
    // =============================================

    /** PENCARIAN: filter live saat user mengetik */
    private void filterMenu(String q) {
        filteredMenu.clear();
        String kw = (q == null ? "" : q.toLowerCase().trim());
        for (MenuItem m : semuaMenu) {
            if (kw.isEmpty() || m.getNama().toLowerCase().contains(kw)
                             || m.getKategori().toLowerCase().contains(kw))
                filteredMenu.add(m);
        }
        setStatus(filteredMenu.size() + " menu ditemukan" +
                  (kw.isEmpty() ? "" : "  untuk \"" + q + "\""));
    }

    /** Tambah item ke nota — PERHITUNGAN: subtotal = harga × qty */
    private void tambahKeNota() {
        if (menuDipilih == null) {
            showAlert("Perhatian", "Pilih menu terlebih dahulu!", Alert.AlertType.WARNING);
            return;
        }
        int qty = spQty.getValue();

        // Jika duplikat → update qty
        for (NotaItem ni : nota) {
            if (ni.getNama().equals(menuDipilih.getNama())) {
                int newQty = ni.getQty() + qty;
                int no     = ni.getNo();
                nota.remove(ni);
                nota.add(new NotaItem(no, menuDipilih, newQty));
                renomor();
                hitung();
                setStatus("Diperbarui: " + menuDipilih.getNama() + " qty=" + newQty);
                bunyiKlik(); animTotal(); return;
            }
        }

        nota.add(new NotaItem(nota.size() + 1, menuDipilih, qty));
        hitung();
        setStatus("Ditambahkan: " + menuDipilih.getNama() + " x" + qty +
                  "  =  " + rpStr((long) menuDipilih.getHarga() * qty));
        bunyiKlik(); animTotal();
    }

    private void hapusItem() {
        NotaItem sel = tabelNota.getSelectionModel().getSelectedItem();
        if (sel != null) {
            String n = sel.getNama();
            nota.remove(sel);
            renomor();
            hitung();
            setStatus("Dihapus: " + n);
        } else setStatus("Pilih baris di tabel nota terlebih dahulu.");
    }

    private void renomor() {
        for (int i = 0; i < nota.size(); i++) nota.get(i).noProperty().set(i + 1);
    }

    /**
     * PERHITUNGAN:
     *   Sub Total  = Σ (harga × qty) tiap item
     *   Total      = Sub Total − Diskon
     *   Kembalian  = Bayar − Total
     */
    private void hitung() {
        long sub  = nota.stream().mapToLong(NotaItem::getSubTotal).sum();
        long dis  = parseLong(tfDiskon.getText());
        long tot  = Math.max(0, sub - dis);
        long byr  = parseLong(tfBayar.getText());
        long kem  = byr - tot;

        lblSubTotal.setText(rpStr(sub));
        lblTotal.setText(rpStr(tot));
        lblKembali.setText(rpStr(Math.abs(kem)));
        lblKembali.setStyle("-fx-font-size:15px; -fx-font-weight:bold; -fx-text-fill:" +
                            (kem < 0 ? "#E74C3C" : "#27AE60") + ";");
    }

    private void prosesBayar() {
        if (nota.isEmpty()) {
            showAlert("Perhatian", "Nota masih kosong!", Alert.AlertType.WARNING);
            return;
        }
        long tot = parseLong(lblTotal.getText().replaceAll("\\D", ""));
        long byr = parseLong(tfBayar.getText());
        if (byr < tot) {
            showAlert("Uang Kurang",
                "Uang bayar kurang  Rp " + rpStr(tot - byr).replace("Rp ",""),
                Alert.AlertType.ERROR);
            return;
        }
        totalTrx++;
        totalOmzet += tot;
        updateStat();
        tampilStruk(tot, byr);

        // SUARA: melodi transaksi berhasil
        new Thread(() -> {
            int[] f = {523, 659, 784, 1047};
            int[] d = {110, 110, 110, 220};
            for (int i = 0; i < f.length; i++) {
                playBeep(f[i], d[i], 0.4f);
                try { Thread.sleep(d[i] + 20); } catch (InterruptedException ex) { Thread.currentThread().interrupt(); }
            }
        }).start();

        invCounter++;
        resetAll();
    }

    private void tampilStruk(long tot, long byr) {
        LocalDateTime now = LocalDateTime.now();
        String tgl = now.format(DateTimeFormatter.ofPattern("EEEE, dd MMMM yyyy", new Locale("id")));
        String jam = now.format(DateTimeFormatter.ofPattern("HH:mm:ss"));

        StringBuilder sb = new StringBuilder();
        sb.append("==========================================\n");
        sb.append("          TAHU BULAT MANG UDIN\n");
        sb.append("        Jl. Cilimus No.17, Bandung\n");
        sb.append("==========================================\n");
        sb.append("No. Invoice : ").append(lblInvNo.getText()).append("\n");
        sb.append("Tanggal     : ").append(tgl).append("\n");
        sb.append("Jam         : ").append(jam).append("\n");
        sb.append("==========================================\n");
        sb.append(String.format("%-3s %-22s %6s %4s %10s\n","No","Nama Menu","Harga","Qty","Sub Total"));
        sb.append("------------------------------------------\n");
        for (NotaItem ni : nota)
            sb.append(String.format("%-3d %-22s %6s %3dx %10s\n",
                ni.getNo(), ni.getNama(), rpStr(ni.getHarga()), ni.getQty(), ni.getSubTotalStr()));
        sb.append("==========================================\n");
        sb.append(String.format("%-28s %s\n",  "Sub Total  :",  lblSubTotal.getText()));
        sb.append(String.format("%-28s %s\n",  "Diskon     :", rpStr(parseLong(tfDiskon.getText()))));
        sb.append(String.format("%-28s %s\n",  "Total      :", rpStr(tot)));
        sb.append(String.format("%-28s %s\n",  "Bayar      :", rpStr(byr)));
        sb.append(String.format("%-28s %s\n",  "Kembalian  :", rpStr(byr - tot)));
        sb.append("==========================================\n");
        sb.append("   Terima kasih! Datang lagi ya~\n");
        sb.append("   Goreng mendadak, enak tiada duanya!\n");
        sb.append("==========================================\n");

        Dialog<ButtonType> dlg = new Dialog<>();
        dlg.setTitle("Struk Transaksi");
        dlg.setHeaderText("Pembayaran Berhasil! Kembalian: " + rpStr(byr - tot));
        TextArea ta = new TextArea(sb.toString());
        ta.setEditable(false);
        ta.setWrapText(false);
        ta.setStyle("-fx-font-family:'Courier New',monospace; -fx-font-size:11px;" +
                    "-fx-background-color:#FFFFF0;");
        ta.setPrefSize(460, 340);
        dlg.getDialogPane().setContent(ta);
        dlg.getDialogPane().setStyle("-fx-background-color:#F4F6F9;");
        dlg.getDialogPane().getButtonTypes().add(ButtonType.OK);
        dlg.showAndWait();
    }

    private void resetAll() {
        nota.clear();
        tfSearch.clear();
        tfBayar.setText("0");
        tfDiskon.setText("0");
        menuDipilih = null;
        spQty.getValueFactory().setValue(1);
        filterMenu("");
        lblSelMenu.setText("— belum dipilih —");
        lblSelMenu.setStyle("-fx-font-size:12px; -fx-text-fill:#9E9E9E;");
        lblSelHarga.setText("");
        listMenu.getSelectionModel().clearSelection();
        lblInvNo.setText("TBU-2025-" + String.format("%04d", invCounter));
        hitung();
        // refresh tanggal
        if (lblInvNo.getScene() != null) {
            Node nd = lblInvNo.getScene().lookup("#lblTanggal");
            if (nd instanceof Label) setTanggal((Label) nd);
        }
        setStatus("Transaksi baru — Invoice: " + lblInvNo.getText());
    }

    // =============================================
    //  JAM REAL-TIME
    // =============================================
    private void startClock() {
        Timeline tl = new Timeline(new KeyFrame(Duration.seconds(1), e -> {
            LocalDateTime now = LocalDateTime.now();
            if (lblClock != null)
                lblClock.setText("  " +
                    now.format(DateTimeFormatter.ofPattern("EEE, dd MMM yyyy  HH:mm:ss",
                               new Locale("id"))) + "  ");
            if (lblInvNo != null && lblInvNo.getScene() != null) {
                Node nd = lblInvNo.getScene().lookup("#lblTanggal");
                if (nd instanceof Label) setTanggal((Label) nd);
            }
        }));
        tl.setCycleCount(Animation.INDEFINITE);
        tl.play();
    }

    private void setTanggal(Label lbl) {
        lbl.setText(LocalDateTime.now().format(DateTimeFormatter.ofPattern(
                "EEEE, dd MMMM yyyy   HH:mm:ss", new Locale("id"))));
    }

    // =============================================
    //  JAVA SOUND API — Notifikasi Suara
    // =============================================
    /**
     * Membuat dan memutar nada menggunakan javax.sound.sampled
     * Gelombang sinus 16-bit PCM dengan envelope fade-in/out
     */
    private static void playBeep(int freq, int ms, float vol) {
        new Thread(() -> {
            try {
                int rate  = 44100;
                int n     = (int)(rate * ms / 1000.0);
                byte[] d  = new byte[n * 2];
                for (int i = 0; i < n; i++) {
                    double t   = (double) i / rate;
                    double env = Math.min(1.0, Math.min(t / 0.005, (ms/1000.0 - t) / 0.02));
                    short v    = (short)(Math.sin(2*Math.PI*freq*t) * env * Short.MAX_VALUE * vol);
                    d[i*2]   = (byte)(v & 0xFF);
                    d[i*2+1] = (byte)((v >> 8) & 0xFF);
                }
                AudioFormat fmt = new AudioFormat(
                    AudioFormat.Encoding.PCM_SIGNED, rate, 16, 1, 2, rate, false);
                DataLine.Info info = new DataLine.Info(SourceDataLine.class, fmt);
                if (!AudioSystem.isLineSupported(info)) return;
                SourceDataLine line = (SourceDataLine) AudioSystem.getLine(info);
                line.open(fmt); line.start();
                line.write(d, 0, d.length);
                line.drain(); line.close();
            } catch (Exception ex) {
                System.err.println("Sound: " + ex.getMessage());
            }
        }).start();
    }

    private void bunyiKlik()  { playBeep(880,  80, 0.35f); }

    private void updateStat() {
        if (lblInvNo == null || lblInvNo.getScene() == null) return;
        Node n1 = lblInvNo.getScene().lookup("#lblStatTrx");
        Node n2 = lblInvNo.getScene().lookup("#lblStatOmzet");
        if (n1 instanceof Label) ((Label)n1).setText("Transaksi : " + totalTrx);
        if (n2 instanceof Label) ((Label)n2).setText("Omzet     : " + rpStr(totalOmzet));
    }

    private void animTotal() {
        ScaleTransition st = new ScaleTransition(Duration.millis(120), lblTotal);
        st.setFromX(1); st.setToX(1.18);
        st.setFromY(1); st.setToY(1.18);
        st.setAutoReverse(true); st.setCycleCount(2); st.play();
    }

    // =============================================
    //  HELPERS
    // =============================================
    private VBox makeCard() {
        VBox v = new VBox(0);
        v.setStyle("-fx-background-color:white; -fx-border-color:#E2E6EA;" +
                   "-fx-border-radius:8; -fx-background-radius:8;");
        return v;
    }

    private Label makeCardHeader(String t) {
        Label l = new Label(t);
        l.setMaxWidth(Double.MAX_VALUE);
        l.setStyle("-fx-font-size:12px; -fx-font-weight:bold; -fx-text-fill:#2C3E6B;" +
                   "-fx-padding:9 12; -fx-background-color:#F8F9FB;" +
                   "-fx-border-color:#E2E6EA; -fx-border-width:0 0 1 0;" +
                   "-fx-background-radius:8 8 0 0;");
        return l;
    }

    private Label addPayRow(VBox parent, String label, boolean big) {
        HBox row = new HBox(8);
        row.setPadding(new Insets(6, 0, 6, 0));
        row.setAlignment(Pos.CENTER_LEFT);
        row.setStyle("-fx-border-color:#F0F2F5; -fx-border-width:0 0 0.5 0;");
        Label lbl = new Label(label);
        lbl.setStyle("-fx-font-size:12px; -fx-font-weight:bold; -fx-text-fill:#5A6A80;");
        Region sp = new Region(); HBox.setHgrow(sp, Priority.ALWAYS);
        Label val = new Label("Rp 0");
        val.setStyle("-fx-font-size:" + (big ? "15" : "13") + "px;" +
                     "-fx-font-weight:bold; -fx-text-fill:#2C3E6B;");
        row.getChildren().addAll(lbl, sp, val);
        parent.getChildren().add(row);
        return val;
    }

    @SuppressWarnings("unchecked")
    private <T> TableColumn<NotaItem, T> col(String title, String prop, double w) {
        TableColumn<NotaItem, T> c = new TableColumn<>(title);
        c.setCellValueFactory(new PropertyValueFactory<>(prop));
        c.setPrefWidth(w);
        return c;
    }

    private Button btn(String t, String clr) {
        Button b = new Button(t);
        b.setStyle(btnStyle(clr) + "-fx-padding:5 10;");
        return b;
    }

    private String btnStyle(String c) {
        return "-fx-background-color:" + c + "; -fx-text-fill:white; -fx-font-size:12px;" +
               "-fx-font-weight:bold; -fx-border-radius:5; -fx-background-radius:5; -fx-cursor:hand;";
    }

    private String iStyle() {
        return "-fx-background-color:white; -fx-border-color:#DCE1EA; " +
               "-fx-border-radius:5; -fx-background-radius:5; -fx-padding:5 8; -fx-font-size:12px;";
    }

    private Label styledLabel(String t, String sz, String clr, boolean bold) {
        Label l = new Label(t);
        l.setStyle("-fx-font-size:" + sz + "; -fx-text-fill:" + clr + ";" +
                   (bold ? "-fx-font-weight:bold;" : ""));
        return l;
    }

    private void setStatus(String msg) {
        Platform.runLater(() -> { if (lblStatus != null) lblStatus.setText(msg); });
    }

    private void showAlert(String title, String msg, Alert.AlertType type) {
        Alert a = new Alert(type);
        a.setTitle(title); a.setHeaderText(null); a.setContentText(msg);
        a.getDialogPane().setStyle("-fx-background-color:#F4F6F9;");
        a.showAndWait();
    }

    private static String rpStr(long n) {
        return "Rp " + String.format("%,d", n).replace(',', '.');
    }

    private static long parseLong(String s) {
        try { return Long.parseLong(s == null ? "0" : s.replaceAll("\\D", "")); }
        catch (Exception e) { return 0; }
    }

    private String badgeClr(String k) {
        return switch (k) {
            case "Pedas"   -> "#E74C3C";
            case "Spesial" -> "#9B59B6";
            case "Paket"   -> "#27AE60";
            default        -> "#E67E22";
        };
    }
}