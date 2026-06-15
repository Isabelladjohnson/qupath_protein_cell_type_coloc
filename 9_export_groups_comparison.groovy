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

// Function to get results for a list of entries
def getResults = { entries ->
    def results = []
    for (def entry : entries) {
        def imageData = entry.readImageData()
        if (imageData == null) {
            print('  SKIPPED: Could not read ' + entry.getImageName())
            continue
        }
        def detections = imageData.getHierarchy().getDetectionObjects()

        // Coloc cells (63: iba1)
        def colocCells = detections.findAll { it.getPathClass()?.toString() == '63: iba1' }
        def colocMax63Values = colocCells.collect { it.getMeasurementList().get('Cell: 63 max') }
            .findAll { it != null && !Double.isNaN(it) }
        double avgColocMax63 = colocMax63Values ? (colocMax63Values.sum() as double) / colocMax63Values.size() : 0.0

        // 63 only cells
        def cells63 = detections.findAll { it.getPathClass()?.toString() == '63' }
        def max63Values = cells63.collect { it.getMeasurementList().get('Cell: 63 max') }
            .findAll { it != null && !Double.isNaN(it) }
        double avg63Max = max63Values ? (max63Values.sum() as double) / max63Values.size() : 0.0

        results.add([
            image: entry.getImageName(),
            numColoc: colocCells.size(),
            avgColocMax63: avgColocMax63,
            num63: cells63.size(),
            avg63Max: avg63Max
        ])

        print('  ' + entry.getImageName())
        print('    63: iba1 cells: ' + colocCells.size() + ', avg Cell: 63 max = ' + String.format('%.4f', avgColocMax63))
        print('    63 cells: ' + cells63.size() + ', avg Cell: 63 max = ' + String.format('%.4f', avg63Max))
    }
    return results
}

print('\nProcessing control group (NC)...')
def controlResults = getResults(controlEntries)

print('\nProcessing experimental group...')
def experimentalResults = getResults(experimentalEntries)

// Calculate group averages
double controlAvgColoc = controlResults ? (controlResults.collect { it.avgColocMax63 }.sum() as double) / controlResults.size() : 0.0
double experimentalAvgColoc = experimentalResults ? (experimentalResults.collect { it.avgColocMax63 }.sum() as double) / experimentalResults.size() : 0.0
double controlAvg63 = controlResults ? (controlResults.collect { it.avg63Max }.sum() as double) / controlResults.size() : 0.0
double experimentalAvg63 = experimentalResults ? (experimentalResults.collect { it.avg63Max }.sum() as double) / experimentalResults.size() : 0.0

// Write summary CSV
def csvFile = new File(outputDir, 'group_comparison.csv')
csvFile.withWriter { writer ->

    // Per image data side by side
    writer.writeLine('Control Group,,,,,Experimental Group,,,,')
    writer.writeLine('Image,Num 63: iba1 Cells,Avg Cell: 63 max (coloc),Num 63 Cells,Avg Cell: 63 max (63 only),Image,Num 63: iba1 Cells,Avg Cell: 63 max (coloc),Num 63 Cells,Avg Cell: 63 max (63 only)')

    def maxRows = Math.max(controlResults.size(), experimentalResults.size())
    for (int i = 0; i < maxRows; i++) {
        def ctrlStr = i < controlResults.size() ?
            controlResults[i].image + ',' +
            controlResults[i].numColoc + ',' +
            String.format('%.4f', controlResults[i].avgColocMax63 as double) + ',' +
            controlResults[i].num63 + ',' +
            String.format('%.4f', controlResults[i].avg63Max as double) :
            ',,,,'
        def expStr = i < experimentalResults.size() ?
            experimentalResults[i].image + ',' +
            experimentalResults[i].numColoc + ',' +
            String.format('%.4f', experimentalResults[i].avgColocMax63 as double) + ',' +
            experimentalResults[i].num63 + ',' +
            String.format('%.4f', experimentalResults[i].avg63Max as double) :
            ',,,,'
        writer.writeLine(ctrlStr + ',' + expStr)
    }

    // Group averages at the bottom
    writer.writeLine('')
    writer.writeLine('Group Averages,,,,,,,,')
    writer.writeLine('Control avg Cell: 63 max (coloc),' + String.format('%.4f', controlAvgColoc) + ',,,Control avg Cell: 63 max (63 only),' + String.format('%.4f', controlAvg63) + ',,,')
    writer.writeLine('Experimental avg Cell: 63 max (coloc),' + String.format('%.4f', experimentalAvgColoc) + ',,,Experimental avg Cell: 63 max (63 only),' + String.format('%.4f', experimentalAvg63) + ',,,')
}

print('\nGroup averages:')
print('  Control (NC) avg Cell: 63 max (coloc): ' + String.format('%.4f', controlAvgColoc))
print('  Experimental avg Cell: 63 max (coloc): ' + String.format('%.4f', experimentalAvgColoc))
print('  Control (NC) avg Cell: 63 max (63 only): ' + String.format('%.4f', controlAvg63))
print('  Experimental avg Cell: 63 max (63 only): ' + String.format('%.4f', experimentalAvg63))
print('\nSaved: group_comparison.csv')
print('Done!')
