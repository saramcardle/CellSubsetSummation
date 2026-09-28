package qupath.ext.cellsubsets;

import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;

import qupath.lib.images.ImageData;
import qupath.lib.objects.PathDetectionObject;
import qupath.lib.objects.PathObject;
import qupath.lib.objects.hierarchy.PathObjectHierarchy;
import qupath.lib.scripting.QP;

/**
 * Core cell-subset measurement logic.
 * <p>
 * These methods are called both from the extension's dialog and directly from scripts -
 * including the scripts logged to a {@link qupath.lib.plugins.workflow.Workflow} by
 * {@link CellSubsetsCommand}, which call {@link #addCellSubsetMeasurement(List)} /
 * {@link #addAllSingleCellSubsetMeasurements(List)} as single statements rather than
 * inlining variable declarations (avoiding name clashes when multiple steps are combined).
 */
public final class CellSubsetsScripting {

    private CellSubsetsScripting() {}

    /**
     * Script-callable entry point: operates on the current image.
     * @param selected the phenotype classes required together
     */
    public static void addCellSubsetMeasurement(List<String> selected) {
        addCellSubsetMeasurement(QP.getCurrentImageData(), selected);
    }

    /**
     * Add, to every annotation, a measurement counting detections that carry every class in
     * {@code selected} (in addition to any others).
     */
    public static void addCellSubsetMeasurement(ImageData<BufferedImage> imageData, List<String> selected) {
        PathObjectHierarchy hierarchy = imageData.getHierarchy();
        Collection<PathObject> allDetections = hierarchy.getDetectionObjects();

        Set<String> required = new LinkedHashSet<>(selected);
        String name = buildMeasurementName(orderLikeMatchingCell(allDetections, required, selected));

        for (PathObject annotation : hierarchy.getAnnotationObjects()) {
            List<PathObject> children = new ArrayList<>(
                    hierarchy.getObjectsForROI(PathDetectionObject.class, annotation.getROI()));
            for (String cls : selected)
                children = children.stream()
                        .filter(c -> c.getClassifications().contains(cls))
                        .collect(Collectors.toList());
            annotation.getMeasurements().put(name, (double) children.size());
        }
    }

    /**
     * Script-callable entry point: operates on the current image.
     * @param selected the phenotype classes to break down against every other class in turn
     */
    public static void addAllSingleCellSubsetMeasurements(List<String> selected) {
        addAllSingleCellSubsetMeasurements(QP.getCurrentImageData(), selected);
    }

    /**
     * For every class {@code base} present on any detection, add an annotation measurement
     * counting detections carrying {@code base} plus every other entry in {@code selected}.
     */
    public static void addAllSingleCellSubsetMeasurements(ImageData<BufferedImage> imageData, List<String> selected) {
        PathObjectHierarchy hierarchy = imageData.getHierarchy();
        Collection<PathObject> allDetections = hierarchy.getDetectionObjects();

        Set<String> classOptions = new TreeSet<>();
        for (PathObject detection : allDetections)
            classOptions.addAll(detection.getClassifications());

        List<PathObject> annotations = new ArrayList<>(hierarchy.getAnnotationObjects());

        Map<PathObject, List<PathObject>> childrenMap = new LinkedHashMap<>();
        for (PathObject annotation : annotations)
            childrenMap.put(annotation, new ArrayList<>(
                    hierarchy.getObjectsForROI(PathDetectionObject.class, annotation.getROI())));

        for (String base : classOptions) {
            // Re-filter from each annotation's full detection set for every base,
            // rather than reusing the previous base's already-narrowed result.
            List<String> remaining = new ArrayList<>(selected);
            remaining.remove(base);

            Set<String> required = new LinkedHashSet<>();
            required.add(base);
            required.addAll(remaining);

            List<String> fallbackOrder = new ArrayList<>();
            fallbackOrder.add(base);
            fallbackOrder.addAll(remaining);

            String name = buildMeasurementName(orderLikeMatchingCell(allDetections, required, fallbackOrder));

            for (PathObject annotation : annotations) {
                List<PathObject> children = childrenMap.get(annotation).stream()
                        .filter(c -> c.getClassifications().contains(base))
                        .collect(Collectors.toList());
                for (String cls : selected) {
                    if (!cls.equals(base))
                        children = children.stream()
                                .filter(c -> c.getClassifications().contains(cls))
                                .collect(Collectors.toList());
                }
                annotation.getMeasurements().put(name, (double) children.size());
            }
        }
    }

    /**
     * Find any detection carrying every class in {@code required}, and return those classes
     * ordered the way that detection's own classification lists them (i.e. matching
     * {@link PathObject#getClassifications()}'s iteration order, which mirrors the order the
     * compound {@link qupath.lib.objects.classes.PathClass} was built in).
     * Falls back to {@code fallbackOrder} if no detection matches (the resulting count will be zero anyway).
     */
    private static List<String> orderLikeMatchingCell(Collection<PathObject> detections, Set<String> required,
                                                        List<String> fallbackOrder) {
        for (PathObject detection : detections) {
            Set<String> classifications = detection.getClassifications();
            if (classifications.containsAll(required))
                return classifications.stream().filter(required::contains).collect(Collectors.toList());
        }
        return fallbackOrder;
    }

    private static String buildMeasurementName(List<String> classes) {
        return "All " + String.join(": ", classes) + " cells";
    }

}
