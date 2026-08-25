def project = getProject() // open project
if (project == null) {
    print('ERROR: No project is open.')
    return
}

def outputDir = new File(project.getPath().getParent().toFile(), 'results') //confrim directory 'results' exists or make it
if (!outputDir.exists()) {
    outputDir.mkdirs()
    print('Created results directory: ' + outputDir)
}

def projectEntries = project.getImageList() //get the list of images
print('Found ' + projectEntries.size() + ' image(s) in project.')

for (def entry : projectEntries) { // for each image print its name, load it's data, get the imaeg heierarchy, pull detections
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
    def totalCells = detections.size() // every detected cell regardless of classification - raw object count and traces back to scripts 3-4 (i think) on how cell detection was run
    def numProtein = detections.count { it.getPathClass()?.toString() == 'protein' } // cells detected as protein classification only
    def numIba1 = detections.count { it.getPathClass()?.toString() == 'iba1' } // cells detetced as iba1 only
    def numColoc = detections.count { it.getPathClass()?.toString() == 'coloc' } // cells detected as colocalized, expressing both protein and iba1
    def numUnclassified = detections.count { it.getPathClass() == null || it.getPathClass().toString() == 'Unclassified' } // all other cells not classified above

    // Helper closure to average a named measurement across a list of cells
    def getAvg = { cells, measurementName ->
        def values = cells.collect { it.getMeasurementList().get(measurementName) }
            .findAll { it != null && !Double.isNaN(it) }
        return values ? (values.sum() as double) / values.size() : 0.0
    }

    // Average Cell: protein and Cell: iba1 mean/min/max across all cells in the image
    double avgProteinMean = getAvg(detections, 'Cell: protein mean')
    double avgProteinMin = getAvg(detections, 'Cell: protein min')
    double avgProteinMax = getAvg(detections, 'Cell: protein max')
    double avgIba1Mean = getAvg(detections, 'Cell: iba1 mean')
    double avgIba1Min = getAvg(detections, 'Cell: iba1 min')
    double avgIba1Max = getAvg(detections, 'Cell: iba1 max')

    // Print summary
    print('  Average Cell: protein mean/min/max: ' + avgProteinMean + ' / ' + avgProteinMin + ' / ' + avgProteinMax)
    print('  Average Cell: iba1 mean/min/max: ' + avgIba1Mean + ' / ' + avgIba1Min + ' / ' + avgIba1Max)

    // Helper to safely format a possibly-missing per-cell measurement
    def fmtVal = { value ->
        return (value != null && !Double.isNaN(value)) ? String.format('%.4f', value as double) : 'NA'
    }

    // Write CSV
    def imageName = entry.getImageName().replaceAll('[^a-zA-Z0-9._-]', '_')
    def csvFile = new File(outputDir, imageName + '_intensity_results.csv')

    csvFile.withWriter { writer ->
        writer.writeLine('Measurement,Value')

        // Cell counts
        writer.writeLine('Total cells,' + totalCells)
        writer.writeLine('Num protein,' + numProtein)
        writer.writeLine('Num iba1,' + numIba1)
        writer.writeLine('Num coloc,' + numColoc)
        writer.writeLine('Num unclassified,' + numUnclassified)

        // Per-cell intensity values, one row per detected cell in the image
        writer.writeLine('')
        writer.writeLine('Cell,Class,Cell: protein mean,Cell: protein min,Cell: protein max,Cell: iba1 mean,Cell: iba1 min,Cell: iba1 max')
        detections.eachWithIndex { cell, idx ->
            def cellClass = cell.getPathClass()?.toString() ?: 'Unclassified'
            writer.writeLine((idx + 1) + ',' + cellClass + ',' +
                fmtVal(cell.getMeasurementList().get('Cell: protein mean')) + ',' +
                fmtVal(cell.getMeasurementList().get('Cell: protein min')) + ',' +
                fmtVal(cell.getMeasurementList().get('Cell: protein max')) + ',' +
                fmtVal(cell.getMeasurementList().get('Cell: iba1 mean')) + ',' +
                fmtVal(cell.getMeasurementList().get('Cell: iba1 min')) + ',' +
                fmtVal(cell.getMeasurementList().get('Cell: iba1 max')))
        }

        // Overall average for the image, as the final row of the per-cell table
        writer.writeLine('Average,,' +
            String.format('%.4f', avgProteinMean) + ',' +
            String.format('%.4f', avgProteinMin) + ',' +
            String.format('%.4f', avgProteinMax) + ',' +
            String.format('%.4f', avgIba1Mean) + ',' +
            String.format('%.4f', avgIba1Min) + ',' +
            String.format('%.4f', avgIba1Max))
    }

    print('  Saved: ' + csvFile.getName())
}

print('Done! Results saved to: ' + outputDir)
