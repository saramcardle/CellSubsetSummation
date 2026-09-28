package qupath.ext.cellsubsets;

import java.awt.image.BufferedImage;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;

import javafx.event.ActionEvent;
import javafx.geometry.Insets;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Dialog;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import qupath.fx.dialogs.Dialogs;
import qupath.lib.gui.QuPathGUI;
import qupath.lib.images.ImageData;
import qupath.lib.objects.PathObject;
import qupath.lib.objects.hierarchy.PathObjectHierarchy;
import qupath.lib.plugins.workflow.DefaultScriptableWorkflowStep;

/**
 * Shows the phenotype-selection dialog. The actual measurement logic lives in
 * {@link CellSubsetsScripting}, which this class calls into directly - the same methods
 * are also what gets called (as a single self-contained statement) from the workflow
 * steps this logs, so re-running/exporting the workflow doesn't need this dialog at all.
 */
class CellSubsetsCommand implements Runnable {

    private final QuPathGUI qupath;

    CellSubsetsCommand(QuPathGUI qupath) {
        this.qupath = qupath;
    }

    @Override
    public void run() {
        ImageData<BufferedImage> imageData = qupath.getImageData();
        if (imageData == null) {
            Dialogs.showErrorMessage("Add Cell Subsets", "No image is open!");
            return;
        }

        PathObjectHierarchy hierarchy = imageData.getHierarchy();

        Set<String> classOptions = new TreeSet<>();
        for (PathObject detection : hierarchy.getDetectionObjects())
            classOptions.addAll(detection.getClassifications());

        // This command is only ever invoked from a menu click, which already
        // runs on the JavaFX Application Thread - so no Platform.runLater()/
        // CountDownLatch is needed here (unlike when this ran as a script).
        List<CheckBox> checkBoxes = classOptions.stream()
                .map(CheckBox::new)
                .collect(Collectors.toList());

        VBox pane = new VBox(10);
        pane.setPadding(new Insets(10));
        pane.getChildren().addAll(checkBoxes);

        ButtonType sumButton = new ButtonType("Calculate Sum", ButtonBar.ButtonData.OTHER);
        ButtonType allSingleButton = new ButtonType("Calculate All Combos", ButtonBar.ButtonData.OTHER);

        Dialog<ButtonType> dialog = Dialogs.builder()
                .title("Select Phenotypes")
                .content(pane)
                .buttons(sumButton, allSingleButton)
                .nonModal()
                .build();

        // No cancel-type button is present, so the native title-bar close (X)
        // is disabled by default - wire the underlying Stage directly instead.
        Stage stage = (Stage) dialog.getDialogPane().getScene().getWindow();
        stage.setOnCloseRequest(event -> dialog.close());

        dialog.getDialogPane().lookupButton(sumButton).addEventFilter(ActionEvent.ACTION, event -> {
            List<String> selected = getSelected(checkBoxes);
            CellSubsetsScripting.addCellSubsetMeasurement(imageData, selected);
            logWorkflowStep(imageData, "Add cell subset measurement", "addCellSubsetMeasurement", selected);
            event.consume();
        });

        dialog.getDialogPane().lookupButton(allSingleButton).addEventFilter(ActionEvent.ACTION, event -> {
            List<String> selected = getSelected(checkBoxes);
            CellSubsetsScripting.addAllSingleCellSubsetMeasurements(imageData, selected);
            logWorkflowStep(imageData, "Add all-combination cell subset measurements",
                    "addAllSingleCellSubsetMeasurements", selected);
            event.consume();
        });

        dialog.show();
    }

    private static List<String> getSelected(List<CheckBox> checkBoxes) {
        return checkBoxes.stream()
                .filter(CheckBox::isSelected)
                .map(CheckBox::getText)
                .collect(Collectors.toList());
    }

    /**
     * Logs a workflow step whose script is a single call into {@link CellSubsetsScripting},
     * so concatenating multiple logged steps (e.g. via Automate &gt; Create workflow) never
     * redeclares a local variable twice.
     */
    private static void logWorkflowStep(ImageData<BufferedImage> imageData, String stepName, String methodName,
                                         List<String> selected) {
        imageData.getHistoryWorkflow().addStep(new DefaultScriptableWorkflowStep(
                stepName + " (" + String.join(", ", selected) + ")",
                "qupath.ext.cellsubsets.CellSubsetsScripting." + methodName + "(" + toGroovyStringList(selected) + ")"
        ));
    }

    private static String toGroovyStringList(List<String> values) {
        return values.stream()
                .map(CellSubsetsCommand::escapeGroovyString)
                .collect(Collectors.joining(", ", "[", "]"));
    }

    private static String escapeGroovyString(String value) {
        return "\"" + value.replace("\\", "\\\\").replace("\"", "\\\"") + "\"";
    }

}
