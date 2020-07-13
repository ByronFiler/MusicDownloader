package sample;

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

import java.util.OptionalDouble;
import java.util.stream.IntStream;

public class downloadData {

    @FXML BorderPane root;

    @FXML VBox textInfoContainer;
    @FXML Text processing;
    @FXML Text downloadSpeed;
    @FXML Text eta;

    @FXML
    private void initialize() {

        Debug.trace(null, "Initialized");

        // Load in data from the model or hide self and warn
        JSONObject workingData = Model.getInstance().download.getDownloadInfo();

        if (workingData.toString().equals(new JSONObject().toString())) {
            Debug.warn(null, "Download data view was accessed without valid data being accessible.");
        } else {

            // Begin drawing with loaded data
            try {

                // UI Text
                downloadSpeed.setText(workingData.getString("downloadSpeed"));
                eta.setText(workingData.getString("eta"));
                processing.setText(workingData.getString("processingMessage"));
                // processing.setWrappingWidth(INTERNAL CONTAINER WIDTH - (INFO MESSAGE + SPACING));
                textInfoContainer.prefWidthProperty().bind(root.prefHeightProperty().divide(2).subtract(20));

                // Preparing the data
                JSONArray chartData = workingData.getJSONArray("seriesData");

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

                for (int i = 0; i < chartData.length(); i++) {

                    series.getData().add(
                            new XYChart.Data<>(
                                    chartData.getJSONObject(i).getInt("time"),
                                    chartData.getJSONObject(i).getInt("speed") / Math.pow(1024, conversion)
                            )
                    );

                }


                chart.getData().add(series);
                chart.prefWidthProperty().bind(root.prefWidthProperty().subtract(textInfoContainer.widthProperty()));

                root.setRight(chart);

                /*
                // Download Speed View
                speedHistory = new LineChart<>(new NumberAxis(), new NumberAxis());
                speedHistory.getData().add(Model.getInstance().download.getHistoryChartData());
                 */


            } catch (JSONException e) {
                Debug.warn(null, "Unknown key");
            }


        }

        // TODO: TimerTask to update data every 50ms


    }

}
