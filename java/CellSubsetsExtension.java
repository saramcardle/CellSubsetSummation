package qupath.ext.cellsubsets;

import javafx.scene.control.Menu;
import javafx.scene.control.MenuItem;

import qupath.lib.common.Version;
import qupath.lib.gui.QuPathGUI;
import qupath.lib.gui.extensions.QuPathExtension;

/**
 * Extension entry point, discovered via the {@code META-INF/services} file.
 * Adds an "Add Cell Subsets..." command under Extensions &gt; Cell Subsets.
 */
public class CellSubsetsExtension implements QuPathExtension {

    @Override
    public void installExtension(QuPathGUI qupath) {
        Menu menu = qupath.getMenu("Extensions>Cell Subsets", true);
        MenuItem item = new MenuItem("Add Cell Subsets...");
        item.setOnAction(e -> new CellSubsetsCommand(qupath).run());
        menu.getItems().add(item);
    }

    @Override
    public String getName() {
        return "Cell Subsets";
    }

    @Override
    public String getDescription() {
        return "Add annotation measurements counting cell subsets by phenotype combination.";
    }

    @Override
    public Version getQuPathVersion() {
        return Version.parse("0.7.0");
    }

}
