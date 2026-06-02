def project = getProject()
if (project == null) {
    print('ERROR: No project is open.')
    return
}

def outputDir = new File(project.getPath().getParent().toFile(), 'results')
if (!outputDir.exists()) {
    outputDir.mkdirs()
    print('Created results directory: ' + outputDir)
}

def projectEntries = project.getImageList()
print('Found ' + projectEntries.size() + ' image(s) in project.')

for (def entry : projectEntries) {
    print('Processing: ' + entry.getImageName())

    def imageData = entry.readImageData()
    if (imageData == null) {
        print('  SKIPPED: Could not read image data.')
        continue
    }

    def hierarchy = imageData.getHierarchy()
    def detections = hierarchy.getDetectionObjects()

    if (detections.isEmpty()) {
        print('  SKIPPED: No detections found.')
        continue
    }

    // Count cells by class
    def totalCells = detections.size()
    def num63 = detections.count { it.getPathClass()?.toString() == '63' }
    def numIba1 = detections.count { it.getPathClass()?.toString() == 'iba1' }
    def numColoc = detections.count { it.getPathClass()?.toString() == 'coloc' }
    def numUnclassified = detections.count { it.getPathClass() == null || it.getPathClass().toString() == 'Unclassified' }

    // Collect ROI min and max values for 63 channel across all cells
    def roiMin63Values = detections.collect { it.getMeasurementList().get('ROI: 2.00 \u00b5m per pixel: 63: Min') }
        .findAll { it != null && !Double.isNaN(it) }
    def roiMax63Values = detections.collect { it.getMeasurementList().get('ROI: 2.00 \u00b5m per pixel: 63: Max') }
        .findAll { it != null && !Double.isNaN(it) }

    // Calculate averages across all cells
    double avgROIMin63 = roiMin63Values ? (roiMin63Values.sum() as double) / roiMin63Values.size() : 0.0
    double avgROIMax63 = roiMax63Values ? (roiMax63Values.sum() as double) / roiMax63Values.size() : 0.0

    // Print summary
    print('  ROI min/max values found: ' + roiMin63Values.size() + ' cells')
    print('  Average ROI 63 min: ' + avgROIMin63)
    print('  Average ROI 63 max: ' + avgROIMax63)

    // Write CSV
    def imageName = entry.getImageName().replaceAll('[^a-zA-Z0-9._-]', '_')
    def csvFile = new File(outputDir, imageName + '_results.csv')

    csvFile.withWriter { writer ->
        writer.writeLine('Measurement,Value')

        // Cell counts
        writer.writeLine('Total cells,' + totalCells)
        writer.writeLine('Num 63,' + num63)
        writer.writeLine('Num iba1,' + numIba1)
        writer.writeLine('Num coloc,' + numColoc)
        writer.writeLine('Num unclassified,' + numUnclassified)

        // Percentages
        writer.writeLine('Percent 63,' + String.format('%.2f', num63 / totalCells * 100.0))
        writer.writeLine('Percent iba1,' + String.format('%.2f', numIba1 / totalCells * 100.0))
        writer.writeLine('Percent coloc,' + String.format('%.2f', numColoc / totalCells * 100.0))

        // ROI min/max intensities averaged across all cells
        writer.writeLine('Average ROI: 63 min,' + String.format('%.4f', avgROIMin63))
        writer.writeLine('Average ROI: 63 max,' + String.format('%.4f', avgROIMax63))
    }

    print('  Saved: ' + csvFile.getName())
}

print('Done! Results saved to: ' + outputDir)
