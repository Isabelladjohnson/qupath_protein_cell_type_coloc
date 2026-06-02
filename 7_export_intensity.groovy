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

    // Filter to only colocalized cells (63: iba1)
    def colocCells = detections.findAll { it.getPathClass()?.toString() == '63: iba1' }

    if (colocCells.isEmpty()) {
        print('  WARNING: No colocalized cells found.')
        continue
    }

    print('  Found ' + colocCells.size() + ' colocalized cells.')

    // Collect Cell and Nucleus 63 max values from coloc cells
    def cellMax63Values = colocCells.collect { it.getMeasurementList().get('Cell: 63 max') }
        .findAll { it != null && !Double.isNaN(it) }
    def nucMax63Values = colocCells.collect { it.getMeasurementList().get('Nucleus: 63 max') }
        .findAll { it != null && !Double.isNaN(it) }

    // Calculate averages
    double avgCellMax63 = cellMax63Values ? (cellMax63Values.sum() as double) / cellMax63Values.size() : 0.0
    double avgNucMax63 = nucMax63Values ? (nucMax63Values.sum() as double) / nucMax63Values.size() : 0.0

    // Write CSV
    def imageName = entry.getImageName().replaceAll('[^a-zA-Z0-9._-]', '_')
    def csvFile = new File(outputDir, imageName + '_coloc_intensity.csv')

    csvFile.withWriter { writer ->
        // Header
        writer.writeLine('Cell ID,Cell: 63 max,Nucleus: 63 max')

        // One row per coloc cell
        def cellIndex = 1
        for (def cell : colocCells) {
            def cellMax = cell.getMeasurementList().get('Cell: 63 max')
            def nucMax = cell.getMeasurementList().get('Nucleus: 63 max')
            def cellMaxStr = (cellMax != null && !Double.isNaN(cellMax)) ? String.format('%.4f', cellMax as double) : 'N/A'
            def nucMaxStr = (nucMax != null && !Double.isNaN(nucMax)) ? String.format('%.4f', nucMax as double) : 'N/A'
            writer.writeLine('Coloc cell ' + cellIndex + ',' + cellMaxStr + ',' + nucMaxStr)
            cellIndex++
        }

        // Summary rows at the bottom
        writer.writeLine('')
        writer.writeLine('Summary,Cell: 63 max,Nucleus: 63 max')
        writer.writeLine('Num coloc cells,' + colocCells.size() + ',')
        writer.writeLine('Average,' + String.format('%.4f', avgCellMax63) + ',' + String.format('%.4f', avgNucMax63))
    }

    print('  Saved: ' + csvFile.getName())
}

print('Done! Results saved to: ' + outputDir)
