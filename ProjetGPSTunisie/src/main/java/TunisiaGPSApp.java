import javafx.animation.*;
import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.effect.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.scene.paint.*;
import javafx.scene.shape.*;
import javafx.scene.text.*;
import javafx.stage.Stage;
import javafx.util.Duration;

import java.util.List;

public class TunisiaGPSApp extends Application {

    // ── Palette ──────────────────────────────────────────────────────────────
    private static final Color BG_DARK      = Color.web("#0d1117");
    private static final Color PANEL_BG     = Color.web("#161b22");
    private static final Color BORDER_COLOR = Color.web("#30363d");
    private static final Color ACCENT_RED   = Color.web("#e63946");
    private static final Color ACCENT_GOLD  = Color.web("#f4a261");
    private static final Color TEXT_PRIMARY  = Color.web("#e6edf3");
    private static final Color TEXT_MUTED    = Color.web("#8b949e");
    private static final Color PATH_ASTAR   = Color.web("#2dd4bf");   // teal
    private static final Color PATH_BFS     = Color.web("#f97316");   // orange
    private static final Color NODE_FILL    = Color.web("#e63946");
    private static final Color NODE_HOVER   = Color.web("#f4a261");

    private Graph graph;
    private Pane mapPane;
    private ComboBox<String> startCombo;
    private ComboBox<String> goalCombo;

    // Result panels
    private Label astarDistance, astarTime, astarNodes, astarPath;
    private Label bfsDistance,   bfsTime,   bfsNodes,   bfsPath;
    private Label statusLabel;

    @Override
    public void start(Stage stage) {
        try {
            graph = ExcelGraphLoader.loadGraphFromExcel("tunisia_graph.xlsx");
        } catch (Exception e) {
            e.printStackTrace();
            //graph = createTunisiaGraph();

        }

        // ── Root layout ───────────────────────────────────────────────────────
        BorderPane root = new BorderPane();
        root.setBackground(new Background(new BackgroundFill(BG_DARK, CornerRadii.EMPTY, Insets.EMPTY)));

        // ── Header ─────────────────────────────────────────────────────────
        root.setTop(buildHeader());

        // ── Map area ───────────────────────────────────────────────────────
        mapPane = buildMapPane();

        // ── Right sidebar ─────────────────────────────────────────────────
        VBox sidebar = buildSidebar();
        sidebar.setPrefWidth(310);

        HBox centerArea = new HBox(0, mapPane, sidebar);
        HBox.setHgrow(mapPane, Priority.ALWAYS);
        root.setCenter(centerArea);

        // ── Status bar ────────────────────────────────────────────────────
        root.setBottom(buildStatusBar());

        Scene scene = new Scene(root, 1200, 1000);
        scene.getStylesheets().add(toInlineStylesheet());
        stage.setScene(scene);
        stage.setTitle("GPS Intelligent — Tunisie");
        stage.show();
    }


    private HBox buildHeader() {
        Label title = new Label("GPS Intelligent");
        title.setFont(Font.font("Georgia", FontWeight.BOLD, 22));
        title.setTextFill(TEXT_PRIMARY);

        Label subtitle = new Label("— Tunisie");
        subtitle.setFont(Font.font("Georgia", FontPosture.ITALIC, 16));
        subtitle.setTextFill(ACCENT_GOLD);

        HBox titleBox = new HBox(6, title, subtitle);
        titleBox.setAlignment(Pos.CENTER_LEFT);

        Label version = new Label("A* · BFS");
        version.setFont(Font.font("Courier New", 11));
        version.setTextFill(TEXT_MUTED);
        version.setPadding(new Insets(4, 8, 4, 8));
        version.setStyle("-fx-border-color: #30363d; -fx-border-radius: 20; -fx-background-color: #21262d; -fx-background-radius: 20;");



        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        HBox header = new HBox(16, titleBox, spacer, version);
        header.setAlignment(Pos.CENTER_LEFT);
        header.setPadding(new Insets(16, 24, 16, 24));
        header.setStyle("-fx-background-color: #161b22; -fx-border-color: transparent transparent #30363d transparent; -fx-border-width: 0 0 1 0;");
        return header;
    }

    private Pane buildMapPane() {
        mapPane = new Pane();
        mapPane.setStyle("-fx-background-color: #0d1117;");

        // Subtle grid lines
        for (int i = 0; i < 26; i++) {
            Line h = new Line(0, i * 36, 750, i * 36);
            h.setStroke(Color.web("#281c1c")); h.setStrokeWidth(0.5);
            mapPane.getChildren().add(h);
        }
        for (int i = 0; i < 22; i++) {
            Line v = new Line(i * 36, 0, i * 36, 870);
            v.setStroke(Color.web("#1c2128")); v.setStrokeWidth(0.5);
            mapPane.getChildren().add(v);
        }

        // Try to load map image
        try {
            Image mapImage = new Image(
                    getClass().getResource("/tunisia-map-map-tunisia-administrative-provinces-blue-color_1091279-2424.png").toExternalForm());
            ImageView mapView = new ImageView(mapImage);
            mapView.setFitWidth(870);
            mapView.setFitHeight(900);
            mapView.setPreserveRatio(false);
            mapView.setOpacity(0.60);
            mapPane.getChildren().add(mapView);
        } catch (Exception ignored) {
        }

        drawEdges();
        drawNodes();
        return mapPane;
    }

    private VBox buildSidebar() {
        VBox sidebar = new VBox(0);
        sidebar.setStyle("-fx-background-color: #161b22; -fx-border-color: transparent transparent transparent #30363d; -fx-border-width: 0 0 0 1;");

        // ── Route selector ────────────────────────────────────────────────
        VBox routeBox = new VBox(12);
        routeBox.setPadding(new Insets(20, 20, 20, 20));
        routeBox.setStyle("-fx-border-color: transparent transparent #30363d transparent; -fx-border-width: 0 0 1 0;");

        Label routeTitle = sectionLabel("ITINÉRAIRE");

        startCombo = styledCombo("Ville de départ");
        goalCombo  = styledCombo("Ville d'arrivée");
        graph.getNodes().keySet().stream().sorted().forEach(k -> {
            startCombo.getItems().add(k);
            goalCombo.getItems().add(k);
        });

        Button astarBtn = primaryButton("▶  Calculer avec A*",   ACCENT_RED);
        Button bfsBtn   = primaryButton("▶  Calculer avec BFS",  ACCENT_GOLD);
        Button clearBtn = ghostButton("✕  Effacer le tracé");

        astarBtn.setOnAction(e -> runAStar());
        bfsBtn.setOnAction(e -> runBFS());
        clearBtn.setOnAction(e -> clearAll());

        routeBox.getChildren().addAll(routeTitle,
                fieldLabel("Départ"), startCombo,
                fieldLabel("Arrivée"), goalCombo,
                new Separator(), astarBtn, bfsBtn, clearBtn);

        // ── Legend ────────────────────────────────────────────────────────
        VBox legendBox = new VBox(8);
        legendBox.setPadding(new Insets(16, 20, 16, 20));
        legendBox.setStyle("-fx-border-color: transparent transparent #30363d transparent; -fx-border-width: 0 0 1 0;");
        legendBox.getChildren().addAll(
                sectionLabel("LÉGENDE"),
                legendItem(PATH_ASTAR, "Chemin A*"),

                legendItem(PATH_BFS,   "Chemin BFS"),
                legendItem(NODE_FILL,  "Gouvernorat"),
                legendItem(Color.web("#2ea043"), "Véhicule")
        );
        VBox infoBox = new VBox(8);
        infoBox.setPadding(new Insets(16, 20, 16, 20));
        infoBox.setStyle("-fx-border-color: transparent transparent #30363d transparent; -fx-border-width: 0 0 1 0;");
        infoBox.getChildren().addAll(
                sectionLabel("INFO"),
                legendItem(PATH_ASTAR, "Speed : 80 km/h"),
                legendItem(NODE_FILL, "Heuristique : haversine")
        );
        // ── Results ───────────────────────────────────────────────────────
        VBox resultsBox = new VBox(12);
        resultsBox.setPadding(new Insets(16, 20, 16, 20));

        // A* card
        astarPath     = resultValueLabel("—");
        astarDistance = resultValueLabel("—");
        astarTime     = resultValueLabel("—");
        astarNodes    = resultValueLabel("—");
        VBox astarCard = resultCard("A*  (A-Star)", PATH_ASTAR,
                astarPath, astarDistance, astarTime, astarNodes);

        // BFS card
        bfsPath     = resultValueLabel("—");
        bfsDistance = resultValueLabel("—");
        bfsTime     = resultValueLabel("—");
        bfsNodes    = resultValueLabel("—");
        VBox bfsCard = resultCard("BFS  (Largeur)", PATH_BFS,
                bfsPath, bfsDistance, bfsTime, bfsNodes);

        resultsBox.getChildren().addAll(sectionLabel("RÉSULTATS"), astarCard, bfsCard);

        VBox.setVgrow(resultsBox, Priority.ALWAYS);
        sidebar.getChildren().addAll(routeBox, legendBox, infoBox , resultsBox);
        return sidebar;
    }

    private HBox buildStatusBar() {
        statusLabel = new Label("Prêt — sélectionnez deux villes et lancez une recherche.");
        statusLabel.setTextFill(TEXT_MUTED);
        statusLabel.setFont(Font.font("Courier New", 11));

        HBox bar = new HBox(statusLabel);
        bar.setPadding(new Insets(6, 16, 6, 16));
        bar.setStyle("-fx-background-color: #0d1117; -fx-border-color: #30363d transparent transparent transparent; -fx-border-width: 1 0 0 0;");
        return bar;
    }

    // ══════════════════════════════════════════════════════════════════════════
    //  DRAWING
    // ══════════════════════════════════════════════════════════════════════════

    private void drawEdges() {
        for (Node node : graph.getNodes().values()) {
            for (Edge edge : node.getNeighbors()) {
                Node t = edge.getTarget();
                // Draw each edge once
                if (node.getName().compareTo(t.getName()) < 0) {
                    Line line = new Line(node.getX(), node.getY(), t.getX(), t.getY());
                    line.setStroke(Color.web("#c410bb"));
                    line.setStrokeWidth(1.2);
                    line.getStrokeDashArray().addAll(4.0, 6.0);
                    line.setUserData("edge");
                    mapPane.getChildren().add(line);
                }
            }
        }
    }

    private void drawNodes() {
        for (Node node : graph.getNodes().values()) {
            Circle outer = new Circle(node.getX(), node.getY(), 8);
            outer.setFill(Color.web("#21262d"));
            outer.setStroke(NODE_FILL);
            outer.setStrokeWidth(2);

            Circle inner = new Circle(node.getX(), node.getY(), 3.5, NODE_FILL);

            // Glow effect
            DropShadow glow = new DropShadow(8, NODE_FILL);
            inner.setEffect(glow);

            Label lbl = new Label(node.getName());
            lbl.setFont(Font.font("Georgia", 10));
            lbl.setTextFill(TEXT_PRIMARY);
            lbl.setLayoutX(node.getX() + 10);
            lbl.setLayoutY(node.getY() - 9);
            lbl.setStyle("-fx-background-color: rgba(22,27,34,0.82); -fx-padding: 1 4 1 4; -fx-background-radius: 3;");

            // Hover
            outer.setOnMouseEntered(e -> {
                outer.setStroke(NODE_HOVER);
                inner.setFill(NODE_HOVER);
                lbl.setTextFill(ACCENT_GOLD);
                lbl.setStyle("-fx-background-color: rgba(22,27,34,0.95); -fx-padding: 1 4 1 4; -fx-background-radius: 3;");
                setStatus("📍  " + node.getName());
            });
            outer.setOnMouseExited(e -> {
                outer.setStroke(NODE_FILL);
                inner.setFill(NODE_FILL);
                lbl.setTextFill(TEXT_PRIMARY);
                lbl.setStyle("-fx-background-color: rgba(22,27,34,0.82); -fx-padding: 1 4 1 4; -fx-background-radius: 3;");
            });
            // Click to set as start/goal
            outer.setOnMouseClicked(e -> {
                if (startCombo.getValue() == null) startCombo.setValue(node.getName());
                else if (goalCombo.getValue() == null) goalCombo.setValue(node.getName());
            });

            mapPane.getChildren().addAll(outer, inner, lbl);
        }
    }

    // ══════════════════════════════════════════════════════════════════════════
    //  ALGORITHMS
    // ══════════════════════════════════════════════════════════════════════════

    private void runAStar() {
        String s = startCombo.getValue(), g = goalCombo.getValue();
        if (!validateSelection(s, g)) return;
        clearRouteDrawing();

        Node start = graph.getNode(s), goal = graph.getNode(g);
        PathResult result = AStar.findPath(start, goal);

        if (result.getPath().isEmpty()) {
            setStatus("❌  A* : aucun chemin trouvé."); return;
        }

        drawAnimatedPath(result.getPath(), PATH_ASTAR, "astar");
        animateVehicle(result.getPath(), Color.web("#2dd4bf"));
        populateCard(result, astarPath, astarDistance, astarTime, astarNodes);
        setStatus("✅  A* terminé — " + result.getExploredNodes() + " nœuds explorés.");
    }

    private void runBFS() {
        String s = startCombo.getValue(), g = goalCombo.getValue();
        if (!validateSelection(s, g)) return;

        Node start = graph.getNode(s), goal = graph.getNode(g);
        PathResult result = BFS.findPath(start, goal);

        if (result.getPath().isEmpty()) {
            setStatus("❌  BFS : aucun chemin trouvé."); return;
        }

        drawAnimatedPath(result.getPath(), PATH_BFS, "bfs");
        animateVehicle(result.getPath(), Color.web("#f97316"));
        populateCard(result, bfsPath, bfsDistance, bfsTime, bfsNodes);
        setStatus("✅  BFS terminé — " + result.getExploredNodes() + " nœuds explorés.");
    }

    private boolean validateSelection(String s, String g) {
        if (s == null || g == null) {
            setStatus("⚠  Veuillez sélectionner départ et arrivée."); return false;
        }
        if (s.equals(g)) {
            setStatus("⚠  Départ et arrivée sont identiques."); return false;
        }
        return true;
    }

    // ══════════════════════════════════════════════════════════════════════════
    //  DRAWING HELPERS
    // ══════════════════════════════════════════════════════════════════════════

    private void drawAnimatedPath(List<Node> path, Color color, String tag) {
        for (int i = 0; i < path.size() - 1; i++) {
            Node a = path.get(i), b = path.get(i + 1);

            Line bg = new Line(a.getX(), a.getY(), b.getX(), b.getY());
            bg.setStroke(color.deriveColor(0, 1, 1, 0.25));
            bg.setStrokeWidth(8);
            bg.setStrokeLineCap(StrokeLineCap.ROUND);
            bg.setUserData(tag);

            Line line = new Line(a.getX(), a.getY(), b.getX(), b.getY());
            line.setStroke(color);
            line.setStrokeWidth(3);
            line.setStrokeLineCap(StrokeLineCap.ROUND);
            line.setUserData(tag);

            DropShadow glow = new DropShadow(6, color);
            line.setEffect(glow);

            // Draw-on animation via stroke dash
            double len = Math.hypot(b.getX() - a.getX(), b.getY() - a.getY());
            line.getStrokeDashArray().addAll(len);
            line.setStrokeDashOffset(len);
            Timeline tl = new Timeline(
                    new KeyFrame(Duration.millis(300 + i * 180),
                            new KeyValue(line.strokeDashOffsetProperty(), 0, Interpolator.EASE_OUT))
            );
            tl.play();

            mapPane.getChildren().addAll(bg, line);
        }

        // Highlight path nodes
        for (Node n : path) {
            Circle highlight = new Circle(n.getX(), n.getY(), 10);
            highlight.setFill(Color.TRANSPARENT);
            highlight.setStroke(color);
            highlight.setStrokeWidth(2);
            highlight.setUserData(tag);
            highlight.setOpacity(0.7);
            mapPane.getChildren().add(highlight);
        }
    }

    private void animateVehicle(List<Node> path, Color color) {
        if (path.size() < 2) return;

        Polyline polyline = new Polyline();
        for (Node n : path) polyline.getPoints().addAll(n.getX(), n.getY());

        Circle vehicle = new Circle(10, Color.web("#2ea043"));
        vehicle.setStroke(color);
        vehicle.setStrokeWidth(2);
        vehicle.setEffect(new DropShadow(12, color));
        vehicle.setUserData("vehicle");
        mapPane.getChildren().add(vehicle);

        PathTransition pt = new PathTransition(Duration.seconds(4), polyline, vehicle);
        pt.setCycleCount(1);
        pt.setInterpolator(Interpolator.EASE_BOTH);
        pt.play();
    }

    private void clearRouteDrawing() {
        mapPane.getChildren().removeIf(n ->
                "astar".equals(n.getUserData()) ||
                        "bfs".equals(n.getUserData()) ||
                        "vehicle".equals(n.getUserData())
        );
    }

    private void clearAll() {
        clearRouteDrawing();
        startCombo.setValue(null);
        goalCombo.setValue(null);
        resetCard(astarPath, astarDistance, astarTime, astarNodes);
        resetCard(bfsPath,   bfsDistance,   bfsTime,   bfsNodes);
        setStatus("Tracé effacé.");
    }

    private void populateCard(PathResult r,
                              Label pathLbl, Label distLbl, Label timeLbl, Label nodesLbl) {
        StringBuilder sb = new StringBuilder();
        List<Node> p = r.getPath();
        for (int i = 0; i < p.size(); i++) {
            sb.append(p.get(i).getName());
            if (i < p.size() - 1) sb.append(" → ");
        }
        pathLbl.setText(sb.toString());
        distLbl.setText(String.format("%.0f km", r.getTotalCost()));
        double min = (r.getTotalCost() / 80.0) * 60;
        timeLbl.setText(String.format("%.0f min", min));
        nodesLbl.setText(r.getExploredNodes() + " nœuds");
    }

    private void resetCard(Label p, Label d, Label t, Label n) {
        p.setText("—"); d.setText("—"); t.setText("—"); n.setText("—");
    }

    // ══════════════════════════════════════════════════════════════════════════
    //  WIDGET FACTORIES
    // ══════════════════════════════════════════════════════════════════════════

    private Label sectionLabel(String text) {
        Label l = new Label(text);
        l.setFont(Font.font("Courier New", FontWeight.BOLD, 12));
        l.setTextFill(TEXT_PRIMARY);
        l.setPadding(new Insets(0, 0, 4, 0));
        return l;
    }

    private Label fieldLabel(String text) {
        Label l = new Label(text);
        l.setFont(Font.font("Georgia", 13));
        l.setTextFill(TEXT_PRIMARY);
        return l;
    }

    private Label resultValueLabel(String text) {
        Label l = new Label(text);
        l.setFont(Font.font("Courier New", 11));
        l.setTextFill(TEXT_PRIMARY);
        l.setWrapText(true);
        return l;
    }

    private ComboBox<String> styledCombo(String prompt) {
        ComboBox<String> cb = new ComboBox<>();
        cb.setPromptText(prompt);
        cb.setMaxWidth(Double.MAX_VALUE);
        cb.setStyle("""
            -fx-background-color: #8b949e;
            -fx-border-color: #30363d;
            -fx-border-radius: 6;
            -fx-background-radius: 6;
            -fx-text-fill: #e6edf3;
            -fx-prompt-text-fill: #8b949e;
            """);
        return cb;
    }

    private Button primaryButton(String text, Color accent) {
        String hex = toHex(accent);
        Button b = new Button(text);
        b.setMaxWidth(Double.MAX_VALUE);
        b.setFont(Font.font("Georgia", FontWeight.BOLD, 13));
        b.setStyle(String.format("""
            -fx-background-color: %s;
            -fx-text-fill: white;
            -fx-background-radius: 6;
            -fx-cursor: hand;
            -fx-padding: 9 16 9 16;
            """, hex));
        b.setOnMouseEntered(e -> b.setStyle(String.format("""
            -fx-background-color: derive(%s, 20%%);
            -fx-text-fill: white;
            -fx-background-radius: 6;
            -fx-cursor: hand;
            -fx-padding: 9 16 9 16;
            """, hex)));
        b.setOnMouseExited(e -> b.setStyle(String.format("""
            -fx-background-color: %s;
            -fx-text-fill: white;
            -fx-background-radius: 6;
            -fx-cursor: hand;
            -fx-padding: 9 16 9 16;
            """, hex)));
        return b;
    }

    private Button ghostButton(String text) {
        Button b = new Button(text);
        b.setMaxWidth(Double.MAX_VALUE);
        b.setFont(Font.font("Georgia", 12));
        b.setStyle("""
            -fx-background-color: transparent;
            -fx-text-fill: #e6edf3;
            -fx-border-color: #30363d;
            -fx-border-radius: 6;
            -fx-background-radius: 6;
            -fx-cursor: hand;
            -fx-padding: 7 16 7 16;
            """);
        return b;
    }

    private HBox legendItem(Color color, String text) {
        Circle dot = new Circle(5, color);
        dot.setEffect(new DropShadow(4, color));
        Label lbl = new Label(text);
        lbl.setFont(Font.font("Georgia", 12));
        lbl.setTextFill(TEXT_PRIMARY);
        HBox row = new HBox(8, dot, lbl);
        row.setAlignment(Pos.CENTER_LEFT);
        return row;
    }

    private VBox resultCard(String title, Color accent,
                            Label pathLbl, Label distLbl, Label timeLbl, Label nodesLbl) {
        String hex = toHex(accent);
        Label header = new Label(title);
        header.setFont(Font.font("Georgia", FontWeight.BOLD, 13));
        header.setTextFill(accent);

        VBox card = new VBox(6,
                header,
                metaRow("Trajet :", pathLbl),
                metaRow("Distance :", distLbl),
                metaRow("Durée est. :", timeLbl),
                metaRow("Exploré :", nodesLbl)
        );
        card.setPadding(new Insets(12));
        card.setStyle(String.format("""
            -fx-background-color: #0d1117;
            -fx-border-color: %s;
            -fx-border-width: 0 0 0 3;
            -fx-border-radius: 0 6 6 0;
            -fx-background-radius: 0 6 6 0;
            """, hex));
        return card;
    }

    private HBox metaRow(String key, Label valueLabel) {
        Label k = new Label(key);
        k.setFont(Font.font("Courier New", 10));
        k.setTextFill(TEXT_MUTED);
        k.setMinWidth(80);
        HBox row = new HBox(6, k, valueLabel);
        row.setAlignment(Pos.TOP_LEFT);
        return row;
    }

    private void setStatus(String msg) {
        if (statusLabel != null) statusLabel.setText(msg);
    }

    private String toHex(Color c) {
        return String.format("#%02X%02X%02X",
                (int)(c.getRed()*255), (int)(c.getGreen()*255), (int)(c.getBlue()*255));
    }

    private String toInlineStylesheet() {
        // Inline CSS data URI for ComboBox dropdown styling
        return "data:text/css," + java.net.URLEncoder.encode(
                ".combo-box-popup .list-cell { -fx-background-color: #21262d; -fx-text-fill: #e6edf3; }" +
                        ".combo-box-popup .list-cell:hover { -fx-background-color: #30363d; }" +
                        ".combo-box .list-cell { -fx-background-color: #21262d; -fx-text-fill: #e6edf3; }",
                java.nio.charset.StandardCharsets.UTF_8);
    }

    // ══════════════════════════════════════════════════════════════════════════
    //  GRAPH — ALL 24 GOVERNORATES
    // ══════════════════════════════════════════════════════════════════════════

    /*private Graph createTunisiaGraph() {
        Graph g = new Graph();

        g.addNode("Tunis",       36.8065, 10.1815, 435, 100);
        g.addNode("Ariana",      36.8663, 10.1647, 430,  65);
        g.addNode("Ben Arous",   36.7533, 10.2282, 450, 110);
        g.addNode("Manouba",     36.8100, 10.0967, 400, 90);

        // North
        g.addNode("Bizerte",     37.2744,  9.8739, 406,  40);
        g.addNode("Nabeul",      36.4510, 10.7350, 495, 90);
        g.addNode("Zaghouan",    36.4029, 10.1429, 432, 152);

        // North-West
        g.addNode("Béja",        36.7256,  9.1817, 339, 115);
        g.addNode("Jendouba",    36.5011,  8.7803, 300, 100);
        g.addNode("Kef",         36.1826,  8.7149, 294, 176);
        g.addNode("Siliana",     36.0843,  9.3700, 357, 188);

        // Centre-East
        g.addNode("Sousse",      35.8256, 10.6084, 460, 200);
        g.addNode("Monastir",    35.7643, 10.8114, 496, 224);
        g.addNode("Mahdia",      35.5047, 11.0622, 521, 253);

        // Centre
        g.addNode("Kairouan",    35.6781, 10.0963, 427, 233);
        g.addNode("Kasserine",   35.1676,  8.8306, 305, 291);
        g.addNode("Sidi Bouzid", 35.0382,  9.4849, 368, 306);

        // South-Centre / East
        g.addNode("Sfax",        34.7406, 10.7603, 485, 320);
        g.addNode("Gabes",       33.8815, 10.0982, 427, 436);
        g.addNode("Médenine",    33.3500, 10.5029, 467, 480);
        g.addNode("Tataouine",   32.9211, 10.4518, 462, 544);

        // South-West
        g.addNode("Gafsa",       34.4250,  8.7842, 300, 375);
        g.addNode("Tozeur",      33.9197,  8.1335, 237, 432);
        g.addNode("Kebili",      33.7050,  8.9718, 318, 456);

        g.addEdge("Tunis",       "Ariana",      7);
        g.addEdge("Tunis",       "Ben Arous",  12);
        g.addEdge("Tunis",       "Manouba",    15);
        g.addEdge("Ariana",      "Manouba",    10);
        g.addEdge("Tunis",       "Bizerte",    65);
        g.addEdge("Tunis",       "Nabeul",     75);
        g.addEdge("Tunis",       "Zaghouan",   57);
        g.addEdge("Tunis",       "Béja",      106);
        g.addEdge("Tunis",       "Sousse",    140);
        g.addEdge("Nabeul",      "Sousse",    110);
        g.addEdge("Nabeul",      "Zaghouan",   75);
        g.addEdge("Zaghouan",    "Kairouan",  100);
        g.addEdge("Zaghouan",    "Sousse",    120);

        // North-West
        g.addEdge("Béja",        "Jendouba",   57);
        g.addEdge("Béja",        "Siliana",    85);
        g.addEdge("Béja",        "Kef",       110);
        g.addEdge("Jendouba",    "Kef",        72);
        g.addEdge("Kef",         "Siliana",    78);
        g.addEdge("Kef",         "Kasserine", 105);
        g.addEdge("Siliana",     "Kairouan",   95);

        // Centre
        g.addEdge("Sousse",      "Monastir",   22);
        g.addEdge("Monastir",    "Mahdia",     45);
        g.addEdge("Sousse",      "Kairouan",   60);
        g.addEdge("Sousse",      "Sfax",      130);
        g.addEdge("Mahdia",      "Sfax",       95);
        g.addEdge("Kairouan",    "Sidi Bouzid",117);
        g.addEdge("Kairouan",    "Kasserine", 108);

        // South-Centre
        g.addEdge("Sfax",        "Gabes",     145);
        g.addEdge("Sfax",        "Sidi Bouzid",120);
        g.addEdge("Kasserine",   "Sidi Bouzid", 90);
        g.addEdge("Kasserine",   "Gafsa",      98);
        g.addEdge("Sidi Bouzid", "Gafsa",     100);

        // South
        g.addEdge("Gabes",       "Médenine",   95);
        g.addEdge("Gabes",       "Kebili",    120);
        g.addEdge("Gabes",       "Gafsa",     150);
        g.addEdge("Gafsa",       "Tozeur",     95);
        g.addEdge("Gafsa",       "Kebili",    160);
        g.addEdge("Tozeur",      "Kebili",     95);
        g.addEdge("Kebili",      "Médenine",  190);
        g.addEdge("Médenine",    "Tataouine",  80);

        return g;
    }*/
}