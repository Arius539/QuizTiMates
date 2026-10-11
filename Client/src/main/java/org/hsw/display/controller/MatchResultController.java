package org.hsw.display.controller;

import javafx.beans.property.ReadOnlyIntegerWrapper;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.XYChart;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import org.hsw.contract.model.IPlayerMatchResult;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Locale;
import java.util.UUID;

@Component
public class MatchResultController {
    private Runnable onBack = () -> {
    };
    private UUID matchId;

    @FXML
    private TabPane resultTabs;
    @FXML
    private HBox podium;
    @FXML
    private Label emptyResultsLabel;
    @FXML
    private Label noRoundsLabel;
    @FXML
    private LineChart<String, Number> pointsChart;
    @FXML
    private TableView<IPlayerMatchResult> rankingTable;
    @FXML
    private TableColumn<IPlayerMatchResult, Number> rankColumn;
    @FXML
    private TableColumn<IPlayerMatchResult, String> playerColumn;
    @FXML
    private TableColumn<IPlayerMatchResult, Number> pointsColumn;
    @FXML
    private TableColumn<IPlayerMatchResult, String> timeColumn;

    @FXML
    private void initialize() {
        rankColumn.setCellValueFactory(cell -> new ReadOnlyIntegerWrapper(cell.getValue().getRank()));
        playerColumn.setCellValueFactory(cell -> new ReadOnlyStringWrapper(cell.getValue().getPlayer().getName()));
        pointsColumn.setCellValueFactory(cell -> new ReadOnlyIntegerWrapper(cell.getValue().getTotalPoints()));
        timeColumn.setCellValueFactory(cell -> new ReadOnlyStringWrapper(String.format(Locale.GERMANY, "%.3f s", cell.getValue().getTotalAnswerTimeMillis() / 1000.0)));
        rankingTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
    }

    public void showResult(UUID matchId, List<IPlayerMatchResult> results, Runnable onBack) {
        clearResult();
        this.matchId = matchId;
        this.onBack = onBack;
        resultTabs.getSelectionModel().selectFirst();
        show(resultTabs, !results.isEmpty());
        show(emptyResultsLabel, results.isEmpty());
        rankingTable.getItems().setAll(results);

        for (int rank : List.of(2, 1, 3)) {
            Region place = rank <= results.size() ? podiumPlace(results.get(rank - 1)) : new Region();
            place.setPrefWidth(100);
            place.setMinWidth(0);
            place.setMaxWidth(Double.MAX_VALUE);
            HBox.setHgrow(place, Priority.ALWAYS);
            podium.getChildren().add(place);
        }

        boolean hasRounds = !results.isEmpty() && !results.getFirst().getRoundPoints().isEmpty();
        show(pointsChart, hasRounds);
        show(noRoundsLabel, !hasRounds);
        if (hasRounds) {
            for (IPlayerMatchResult result : results) {
                XYChart.Series<String, Number> series = new XYChart.Series<>();
                series.setName(result.getPlayer().getName() + " (Platz " + result.getRank() + ")");
                int total = 0;
                for (int round = 0; round < result.getRoundPoints().size(); round++) {
                    total += result.getRoundPoints().get(round);
                    series.getData().add(new XYChart.Data<>(Integer.toString(round + 1), total));
                }
                pointsChart.getData().add(series);
            }
        }
    }

    public UUID getMatchId() {
        return matchId;
    }

    public void clearResult() {
        matchId = null;
        onBack = () -> {
        };
        podium.getChildren().clear();
        pointsChart.getData().clear();
        rankingTable.getItems().clear();
    }

    private static void show(Node node, boolean visible) {
        node.setVisible(visible);
        node.setManaged(visible);
    }

    private VBox podiumPlace(IPlayerMatchResult result) {
        Label name = new Label(result.getPlayer().getName());
        name.setWrapText(true);
        name.setAlignment(Pos.CENTER);
        name.setMaxWidth(Double.MAX_VALUE);
        name.setMaxHeight(54);
        name.setTooltip(new Tooltip(result.getPlayer().getName()));
        Label points = new Label(result.getTotalPoints() + " Punkte");
        points.setWrapText(true);
        Label rank = new Label(Integer.toString(result.getRank()));
        rank.setStyle("-fx-font-size: 26px; -fx-font-weight: bold;");
        VBox block = new VBox(rank);
        block.setAlignment(Pos.CENTER);
        block.setPrefHeight(150 - result.getRank() * 30);
        block.setMinHeight(Region.USE_PREF_SIZE);
        block.setMaxHeight(Region.USE_PREF_SIZE);
        String color = switch (result.getRank()) {
            case 1 -> "#ead078";
            case 2 -> "#ccd1d7";
            default -> "#c99c7b";
        };
        block.setStyle("-fx-background-color: " + color + "; -fx-background-radius: 4;");
        VBox place = new VBox(6, name, points, block);
        place.setAlignment(Pos.BOTTOM_CENTER);
        return place;
    }

    @FXML
    private void backToLobby() {
        onBack.run();
    }
}
