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

// Helper closure to get average of a measurement from a list of cells
def getAvg = { cells, measurementName ->
    def values = cells.collect { it.getMeasurementList().get(measurementName) }
        .findAll { it != null && !Double.isNaN(it) }
    return values ? (values.sum() as double) / values.size() : 0.0
}

// Helper closure to get average of a column across a list of row results
def getColAvg = { rows, key ->
    def values = rows.collect { it[key] }.findAll { it != null }
    return values ? (values.sum() as double) / values.size() : 0.0
}

// Collect all results
def allResults = []

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

    // Filter cells by class
    def cells63 = detections.findAll { it.getPathClass()?.toString() == '63' }
    def cellsIba1 = detections.findAll { it.getPathClass()?.toString() == 'iba1' }
    def cellsColoc = detections.findAll { it.getPathClass()?.toString() == '63: iba1' }
    def cellsUnclassified = detections.findAll { it.getPathClass() == null || it.getPathClass().toString() == 'Unclassified' }

    print('  63: ' + cells63.size() + '  iba1: ' + cellsIba1.size() + '  coloc: ' + cellsColoc.size() + '  unclassified: ' + cellsUnclassified.size())

    def isControl = entry.getImageName().startsWith('NC')

    allResults.add([
        image: entry.getImageName(),
        isControl: isControl,

        // 63 cells
        num63: cells63.size(),
        c63_63mean: getAvg(cells63, 'Cell: 63 mean'),
        c63_63min: getAvg(cells63, 'Cell: 63 min'),
        c63_63max: getAvg(cells63, 'Cell: 63 max'),
        c63_iba1mean: getAvg(cells63, 'Cell: iba1 mean'),
        c63_iba1min: getAvg(cells63, 'Cell: iba1 min'),
        c63_iba1max: getAvg(cells63, 'Cell: iba1 max'),

        // iba1 cells
        numIba1: cellsIba1.size(),
        iba1_63mean: getAvg(cellsIba1, 'Cell: 63 mean'),
        iba1_63min: getAvg(cellsIba1, 'Cell: 63 min'),
        iba1_63max: getAvg(cellsIba1, 'Cell: 63 max'),
        iba1_iba1mean: getAvg(cellsIba1, 'Cell: iba1 mean'),
        iba1_iba1min: getAvg(cellsIba1, 'Cell: iba1 min'),
        iba1_iba1max: getAvg(cellsIba1, 'Cell: iba1 max'),

        // coloc cells
        numColoc: cellsColoc.size(),
        coloc_63mean: getAvg(cellsColoc, 'Cell: 63 mean'),
        coloc_63min: getAvg(cellsColoc, 'Cell: 63 min'),
        coloc_63max: getAvg(cellsColoc, 'Cell: 63 max'),
        coloc_iba1mean: getAvg(cellsColoc, 'Cell: iba1 mean'),
        coloc_iba1min: getAvg(cellsColoc, 'Cell: iba1 min'),
        coloc_iba1max: getAvg(cellsColoc, 'Cell: iba1 max'),

        // unclassified cells
        numUnclassified: cellsUnclassified.size(),
        unc_63mean: getAvg(cellsUnclassified, 'Cell: 63 mean'),
        unc_63min: getAvg(cellsUnclassified, 'Cell: 63 min'),
        unc_63max: getAvg(cellsUnclassified, 'Cell: 63 max'),
        unc_iba1mean: getAvg(cellsUnclassified, 'Cell: iba1 mean'),
        unc_iba1min: getAvg(cellsUnclassified, 'Cell: iba1 min'),
        unc_iba1max: getAvg(cellsUnclassified, 'Cell: iba1 max')
    ])
}

// Split into control and experimental
def controlResults = allResults.findAll { it.isControl }
def experimentalResults = allResults.findAll { !it.isControl }

print('\nControl images: ' + controlResults.size())
print('Experimental images: ' + experimentalResults.size())

// Helper to format a row of values
def formatRow = { r ->
    return r.image + ',' +
        r.num63 + ',' +
        String.format('%.4f', r.c63_63mean as double) + ',' +
        String.format('%.4f', r.c63_63min as double) + ',' +
        String.format('%.4f', r.c63_63max as double) + ',' +
        String.format('%.4f', r.c63_iba1mean as double) + ',' +
        String.format('%.4f', r.c63_iba1min as double) + ',' +
        String.format('%.4f', r.c63_iba1max as double) + ',' +
        r.numIba1 + ',' +
        String.format('%.4f', r.iba1_63mean as double) + ',' +
        String.format('%.4f', r.iba1_63min as double) + ',' +
        String.format('%.4f', r.iba1_63max as double) + ',' +
        String.format('%.4f', r.iba1_iba1mean as double) + ',' +
        String.format('%.4f', r.iba1_iba1min as double) + ',' +
        String.format('%.4f', r.iba1_iba1max as double) + ',' +
        r.numColoc + ',' +
        String.format('%.4f', r.coloc_63mean as double) + ',' +
        String.format('%.4f', r.coloc_63min as double) + ',' +
        String.format('%.4f', r.coloc_63max as double) + ',' +
        String.format('%.4f', r.coloc_iba1mean as double) + ',' +
        String.format('%.4f', r.coloc_iba1min as double) + ',' +
        String.format('%.4f', r.coloc_iba1max as double) + ',' +
        r.numUnclassified + ',' +
        String.format('%.4f', r.unc_63mean as double) + ',' +
        String.format('%.4f', r.unc_63min as double) + ',' +
        String.format('%.4f', r.unc_63max as double) + ',' +
        String.format('%.4f', r.unc_iba1mean as double) + ',' +
        String.format('%.4f', r.unc_iba1min as double) + ',' +
        String.format('%.4f', r.unc_iba1max as double)
}

// Helper to format a summary average row
def formatAvgRow = { label, rows ->
    return label + ',' +
        String.format('%.2f', getColAvg(rows, 'num63') as double) + ',' +
        String.format('%.4f', getColAvg(rows, 'c63_63mean') as double) + ',' +
        String.format('%.4f', getColAvg(rows, 'c63_63min') as double) + ',' +
        String.format('%.4f', getColAvg(rows, 'c63_63max') as double) + ',' +
        String.format('%.4f', getColAvg(rows, 'c63_iba1mean') as double) + ',' +
        String.format('%.4f', getColAvg(rows, 'c63_iba1min') as double) + ',' +
        String.format('%.4f', getColAvg(rows, 'c63_iba1max') as double) + ',' +
        String.format('%.2f', getColAvg(rows, 'numIba1') as double) + ',' +
        String.format('%.4f', getColAvg(rows, 'iba1_63mean') as double) + ',' +
        String.format('%.4f', getColAvg(rows, 'iba1_63min') as double) + ',' +
        String.format('%.4f', getColAvg(rows, 'iba1_63max') as double) + ',' +
        String.format('%.4f', getColAvg(rows, 'iba1_iba1mean') as double) + ',' +
        String.format('%.4f', getColAvg(rows, 'iba1_iba1min') as double) + ',' +
        String.format('%.4f', getColAvg(rows, 'iba1_iba1max') as double) + ',' +
        String.format('%.2f', getColAvg(rows, 'numColoc') as double) + ',' +
        String.format('%.4f', getColAvg(rows, 'coloc_63mean') as double) + ',' +
        String.format('%.4f', getColAvg(rows, 'coloc_63min') as double) + ',' +
        String.format('%.4f', getColAvg(rows, 'coloc_63max') as double) + ',' +
        String.format('%.4f', getColAvg(rows, 'coloc_iba1mean') as double) + ',' +
        String.format('%.4f', getColAvg(rows, 'coloc_iba1min') as double) + ',' +
        String.format('%.4f', getColAvg(rows, 'coloc_iba1max') as double) + ',' +
        String.format('%.2f', getColAvg(rows, 'numUnclassified') as double) + ',' +
        String.format('%.4f', getColAvg(rows, 'unc_63mean') as double) + ',' +
        String.format('%.4f', getColAvg(rows, 'unc_63min') as double) + ',' +
        String.format('%.4f', getColAvg(rows, 'unc_63max') as double) + ',' +
        String.format('%.4f', getColAvg(rows, 'unc_iba1mean') as double) + ',' +
        String.format('%.4f', getColAvg(rows, 'unc_iba1min') as double) + ',' +
        String.format('%.4f', getColAvg(rows, 'unc_iba1max') as double)
}

def header = 'Image,' +
    'Num 63 Cells,Avg Cell: 63 mean (63),Avg Cell: 63 min (63),Avg Cell: 63 max (63),Avg Cell: iba1 mean (63),Avg Cell: iba1 min (63),Avg Cell: iba1 max (63),' +
    'Num iba1 Cells,Avg Cell: 63 mean (iba1),Avg Cell: 63 min (iba1),Avg Cell: 63 max (iba1),Avg Cell: iba1 mean (iba1),Avg Cell: iba1 min (iba1),Avg Cell: iba1 max (iba1),' +
    'Num Coloc Cells,Avg Cell: 63 mean (coloc),Avg Cell: 63 min (coloc),Avg Cell: 63 max (coloc),Avg Cell: iba1 mean (coloc),Avg Cell: iba1 min (coloc),Avg Cell: iba1 max (coloc),' +
    'Num Unclassified Cells,Avg Cell: 63 mean (unclassified),Avg Cell: 63 min (unclassified),Avg Cell: 63 max (unclassified),Avg Cell: iba1 mean (unclassified),Avg Cell: iba1 min (unclassified),Avg Cell: iba1 max (unclassified)'

def csvFile = new File(outputDir, 'all_classes_intensity.csv')
csvFile.withWriter { writer ->

    writer.writeLine(header)

    // Control group rows
    writer.writeLine('--- Control Group (NC) ---')
    for (def r : controlResults) {
        writer.writeLine(formatRow(r))
    }

    // Experimental group rows
    writer.writeLine('--- Experimental Group ---')
    for (def r : experimentalResults) {
        writer.writeLine(formatRow(r))
    }

    // Summary averages
    writer.writeLine('')
    writer.writeLine('--- Group Averages ---')
    writer.writeLine(formatAvgRow('Control Average (NC)', controlResults))
    writer.writeLine(formatAvgRow('Experimental Average', experimentalResults))
}

print('Done! Results saved to: ' + csvFile)
