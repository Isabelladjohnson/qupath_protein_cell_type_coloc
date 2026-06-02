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

// Separate images into control (NC) and experimental groups
def controlEntries = projectEntries.findAll { it.getImageName().startsWith('NC') }
def experimentalEntries = projectEntries.findAll { !it.getImageName().startsWith('NC') }

print('Control images (NC): ' + controlEntries.size())
print('Experimental images: ' + experimentalEntries.size())

// Function to get average Cell: 63 max from coloc cells for a list of entries
def getColocMax63 = { entries ->
    def results = []
    for (def entry : entries) {
        def imageData = entry.readImageData()
        if (imageData == null) {
            print('  SKIPPED: Could not read ' + entry.getImageName())
            continue
        }
        def detections = imageData.getHierarchy().getDetectionObjects()
        def colocCells = detections.findAll { it.getPathClass()?.toString() == '63: iba1' }

        if (colocCells.isEmpty()) {
            print('  WARNING: No coloc cells in ' + entry.getImageName())
            results.add([image: entry.getImageName(), numColoc: 0, avgCellMax63: 0.0])
            continue
        }

        def cellMax63Values = colocCells.collect { it.getMeasurementList().get('Cell: 63 max') }
            .findAll { it != null && !Double.isNaN(it) }

        double avgCellMax63 = cellMax63Values ? (cellMax63Values.sum() as double) / cellMax63Values.size() : 0.0

        results.add([image: entry.getImageName(), numColoc: colocCells.size(), avgCellMax63: avgCellMax63])
        print('  ' + entry.getImageName() + ': ' + colocCells.size() + ' coloc cells, avg Cell: 63 max = ' + String.format('%.4f', avgCellMax63))
    }
    return results
}

print('\nProcessing control group (NC)...')
def controlResults = getColocMax63(controlEntries)

print('\nProcessing experimental group...')
def experimentalResults = getColocMax63(experimentalEntries)

// Calculate group averages
def controlAvg = controlResults ? (controlResults.collect { it.avgCellMax63 }.sum() as double) / controlResults.size() : 0.0
def experimentalAvg = experimentalResults ? (experimentalResults.collect { it.avgCellMax63 }.sum() as double) / experimentalResults.size() : 0.0

// Write summary CSV
def csvFile = new File(outputDir, 'group_comparison.csv')
csvFile.withWriter { writer ->

    // Per image data side by side
    writer.writeLine('Control Group,,Experimental Group,')
    writer.writeLine('Image,Num Coloc Cells,Avg Cell: 63 max,Image,Num Coloc Cells,Avg Cell: 63 max')

    def maxRows = Math.max(controlResults.size(), experimentalResults.size())
    for (int i = 0; i < maxRows; i++) {
        def ctrlStr = i < controlResults.size() ?
            controlResults[i].image + ',' + controlResults[i].numColoc + ',' + String.format('%.4f', controlResults[i].avgCellMax63 as double) :
            ',,'
        def expStr = i < experimentalResults.size() ?
            experimentalResults[i].image + ',' + experimentalResults[i].numColoc + ',' + String.format('%.4f', experimentalResults[i].avgCellMax63 as double) :
            ',,'
        writer.writeLine(ctrlStr + ',' + expStr)
    }

    // Group averages at the bottom
    writer.writeLine('')
    writer.writeLine('Group Average,,,' + ',,,')
    writer.writeLine('Control avg Cell: 63 max,' + String.format('%.4f', controlAvg) + ',,Experimental avg Cell: 63 max,' + String.format('%.4f', experimentalAvg) + ',,')
}

print('\nGroup averages:')
print('  Control (NC) avg Cell: 63 max: ' + String.format('%.4f', controlAvg))
print('  Experimental avg Cell: 63 max: ' + String.format('%.4f', experimentalAvg))
print('\nSaved: group_comparison.csv')
print('Done!')
