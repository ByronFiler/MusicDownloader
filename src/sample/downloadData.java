package sample;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.time.Instant;
import java.util.OptionalDouble;
import java.util.Timer;
import java.util.TimerTask;
import java.util.stream.IntStream;

// TODO: Initialize is the only function, doesn't require a unique controller, move back into the downloads controller
public class downloadData {
    @FXML BorderPane root;

    @FXML VBox textInfoContainer;
    @FXML Text processing;
    @FXML Text downloadSpeed;
    @FXML Text eta;

    @FXML
    private void initialize() {

        long start = Instant.now().toEpochMilli();

        final JSONObject[] workingData = {new JSONObject()};
        new Timer().schedule(new TimerTask() {
            @Override
            public void run() {

                // New data received, redrawing
                if (!Model.getInstance().download.getDownloadInfo().toString().equals(workingData[0].toString())) {

                    // Begin drawing with loaded data
                    try {
                        workingData[0] = Model.getInstance().download.getDownloadInfo();

                        // UI Text
                        downloadSpeed.setText(workingData[0].getString("downloadSpeed"));

                        eta.setText(workingData[0].getString("eta"));

                        processing.setText(workingData[0].getString("song"));

                        // processing.setWrappingWidth(INTERNAL CONTAINER WIDTH - (INFO MESSAGE + SPACING));
                        textInfoContainer.prefWidthProperty().bind(root.prefHeightProperty().divide(2).subtract(20));

                        // Preparing the data
                        JSONArray chartData = workingData[0].getJSONArray("seriesData");

                        // Calculating max point
                        double minCalculator = 0;
                        OptionalDouble minCalculatorOpt = IntStream.range(0, chartData.length()).mapToDouble(i -> {
                            try {
                                return chartData.getJSONObject(i).getInt("speed");
                            } catch (JSONException e) {
                                Debug.error(null, "Missing data in working data.", e.getCause());
                            }
                            return 0;
                        }).min();

                        if (minCalculatorOpt.isPresent())
                            minCalculator = minCalculatorOpt.getAsDouble();
                        else
                            Debug.error(null, "Failed to get maximum value from given data.", null);

                        int conversion;
                        // Surely there is a better way to do this?
                        if (minCalculator > 1024 * 1024) {
                            // Using units MiB/s
                            conversion = 2;
                        } else if (minCalculator > 1024) {
                            // Using units KiB/s
                            conversion = 1;
                        } else {
                            // Using units Bytes/s
                            conversion = 0;
                        }

                        NumberAxis xAxis = new NumberAxis();
                        NumberAxis yAxis = new NumberAxis();

                        yAxis.setLabel(new String[]{"Bytes/s", "KiB/s", "MiB/s"}[conversion]);
                        xAxis.setLabel("Playtime Downloaded");

                        LineChart<Number, Number> chart = new LineChart<>(xAxis,yAxis);
                        XYChart.Series<Number, Number> series = new XYChart.Series<>();

                        // Due to size constraints we ideally just want to map a few data points if there are too many
                        if (chartData.length() > 10) {

                            // Group data into 10 clusters calculate average of each
                            for (int i = 0; i < 9; i++) {

                                OptionalDouble clusterAverageTimeOpt = IntStream.range(
                                        (int) Math.round((double) chartData.length() / 10) * i,
                                        (int) Math.round((double) chartData.length() / 10) * i+1
                                ).mapToDouble(j -> {
                                    try {
                                        return chartData.getJSONObject(j).getInt("time");
                                    } catch (JSONException e) {
                                        e.printStackTrace();
                                    }
                                    return 0;
                                }).average();

                                OptionalDouble clusterAverageSpeedOpt = IntStream.range(
                                        (int) Math.round((double) chartData.length() / 10) * i,
                                        (int) Math.round((double) chartData.length() / 10) * i+1
                                ).mapToDouble(j -> {
                                    try {
                                        return chartData.getJSONObject(j).getInt("speed");
                                    } catch (JSONException e) {
                                        e.printStackTrace();
                                    }
                                    return 0;
                                }).average();

                                if (clusterAverageTimeOpt.isPresent() && clusterAverageSpeedOpt.isPresent()) {

                                    series.getData().add(
                                            new XYChart.Data<>(
                                                    clusterAverageTimeOpt.getAsDouble(),
                                                    clusterAverageSpeedOpt.getAsDouble()
                                            )
                                    );

                                } else {
                                    Debug.error(null, "Failed to calculate average of given cluster.", null);
                                }

                            }

                        } else {

                            for (int i = 0; i < chartData.length(); i++) {
                                series.getData().add(
                                        new XYChart.Data<>(
                                                chartData.getJSONObject(i).getInt("time"),
                                                chartData.getJSONObject(i).getInt("speed") / Math.pow(1024, conversion)
                                        )
                                );

                            }
                        }

                        chart.getData().add(series);
                        chart.prefWidthProperty().bind(root.prefWidthProperty().subtract(textInfoContainer.widthProperty()));

                        Platform.runLater(() -> root.setRight(chart));
                    } catch (JSONException e) {
                        Debug.warn(null, "Unknown key");
                    }

                }

                if (Model.getInstance().download.getDownloadObject().toString().equals(new JSONObject().toString()))
                    this.cancel();

            }
        }, 0, 50);

        Debug.trace(null, "Initialized downloads data in : " + (Instant.now().toEpochMilli() - start) + "ms");

    }

}
