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
    def num153 = detections.count { it.getPathClass()?.toString() == '153' } // cells detected as 153 classification only
    def numIba1 = detections.count { it.getPathClass()?.toString() == 'iba1' } // cells detetced as iba1 only
    def numColoc = detections.count { it.getPathClass()?.toString() == 'coloc' } // cells detected as colocalized, expressing both 153 and iba1
    def numUnclassified = detections.count { it.getPathClass() == null || it.getPathClass().toString() == 'Unclassified' } // all other cells not classified above

    // Helper closure to average a named measurement across a list of cells
    def getAvg = { cells, measurementName ->
        def values = cells.collect { it.getMeasurementList().get(measurementName) }
            .findAll { it != null && !Double.isNaN(it) }
        return values ? (values.sum() as double) / values.size() : 0.0
    }

    // Average Cell: 153 and Cell: iba1 mean/min/max across all cells in the image
    double avg153Mean = getAvg(detections, 'Cell: 153 mean')
    double avg153Min = getAvg(detections, 'Cell: 153 min')
    double avg153Max = getAvg(detections, 'Cell: 153 max')
    double avgIba1Mean = getAvg(detections, 'Cell: iba1 mean')
    double avgIba1Min = getAvg(detections, 'Cell: iba1 min')
    double avgIba1Max = getAvg(detections, 'Cell: iba1 max')

    // Print summary
    print('  Average Cell: 153 mean/min/max: ' + avg153Mean + ' / ' + avg153Min + ' / ' + avg153Max)
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
        writer.writeLine('Num 153,' + num153)
        writer.writeLine('Num iba1,' + numIba1)
        writer.writeLine('Num coloc,' + numColoc)
        writer.writeLine('Num unclassified,' + numUnclassified)

        // Percentages
        writer.writeLine('Percent 153,' + String.format('%.2f', num153 / totalCells * 100.0))
        writer.writeLine('Percent iba1,' + String.format('%.2f', numIba1 / totalCells * 100.0))
        writer.writeLine('Percent coloc,' + String.format('%.2f', numColoc / totalCells * 100.0))

        // Per-cell intensity values, one row per detected cell in the image
        writer.writeLine('')
        writer.writeLine('Cell,Class,Cell: 153 mean,Cell: 153 min,Cell: 153 max,Cell: iba1 mean,Cell: iba1 min,Cell: iba1 max')
        detections.eachWithIndex { cell, idx ->
            def cellClass = cell.getPathClass()?.toString() ?: 'Unclassified'
            writer.writeLine((idx + 1) + ',' + cellClass + ',' +
                fmtVal(cell.getMeasurementList().get('Cell: 153 mean')) + ',' +
                fmtVal(cell.getMeasurementList().get('Cell: 153 min')) + ',' +
                fmtVal(cell.getMeasurementList().get('Cell: 153 max')) + ',' +
                fmtVal(cell.getMeasurementList().get('Cell: iba1 mean')) + ',' +
                fmtVal(cell.getMeasurementList().get('Cell: iba1 min')) + ',' +
                fmtVal(cell.getMeasurementList().get('Cell: iba1 max')))
        }

        // Overall average for the image, as the final row of the per-cell table
        writer.writeLine('Average,,' +
            String.format('%.4f', avg153Mean) + ',' +
            String.format('%.4f', avg153Min) + ',' +
            String.format('%.4f', avg153Max) + ',' +
            String.format('%.4f', avgIba1Mean) + ',' +
            String.format('%.4f', avgIba1Min) + ',' +
            String.format('%.4f', avgIba1Max))
    }

    print('  Saved: ' + csvFile.getName())
}

print('Done! Results saved to: ' + outputDir)
