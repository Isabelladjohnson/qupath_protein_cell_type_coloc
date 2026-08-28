def project = getProject() // open the project
if (project == null) {
    print('ERROR: No project is open.')
    return
}

def outputDir = new File(project.getPath().getParent().toFile(), 'results') //confirm results folder exists or make it
if (!outputDir.exists()) {
    outputDir.mkdirs()
    print('Created results directory: ' + outputDir)
}

def projectEntries = project.getImageList() // get all the images in the project and print number of images
print('Found ' + projectEntries.size() + ' image(s) in project.')

// Helper closure to get average of a measurement from a list of cells - given list of cells and the name of the measurement, calculate the mean of that measurement across the list
def getAvg = { cells, measurementName ->
    def values = cells.collect { it.getMeasurementList().get(measurementName) }
        .findAll { it != null && !Double.isNaN(it) } //drop any null values
    return values ? (values.sum() as double) / values.size() : 0.0
}

// Helper closure to get average of a column across a list of row results. Each row is one image+category combo; pull the value at 'key' from each row and average it
def getColAvg = { rows, key ->
    def values = rows.collect { it[key] }.findAll { it != null }
    return values ? (values.sum() as double) / values.size() : 0.0
}

// The two channels/markers we want intensity stats for. Only the 'Cell' compartment this time - no Nucleus columns.
def CHANNELS = ['153', 'iba1']
def COMPARTMENTS = ['Cell']
def STATS = ['mean']

// Build the ordered list of QuPath measurement names we'll average for every category: 'Cell: 153 mean', 'Cell: iba1 mean'
def MEASUREMENT_KEYS = []
COMPARTMENTS.each { compartment ->
    CHANNELS.each { channel ->
        STATS.each { stat ->
            MEASUREMENT_KEYS << "${compartment}: ${channel} ${stat}"
        }
    }
}

// Collect all results - main per image loop. One row per (image, category) pair, long/tidy format.
def allRows = []

for (def entry : projectEntries) {
    print('Processing: ' + entry.getImageName()) // print image name

    def imageData = entry.readImageData()
    if (imageData == null) {
        print('  SKIPPED: Could not read image data.') // skip if there is an issue with reading image data
        continue
    }

    def hierarchy = imageData.getHierarchy() // QuPath's PathObjectHierarchy for this image
    def detections = hierarchy.getDetectionObjects()

    if (detections.isEmpty()) {
        print('  SKIPPED: No detections found.')
        continue
    }

    // Define the 5 categories for this image. 'All' is every detected cell regardless of class.
    def categories = [
        'All'         : detections,
        '153'         : detections.findAll { it.getPathClass()?.toString() == '153' },
        'iba1'        : detections.findAll { it.getPathClass()?.toString() == 'iba1' },
        'coloc'       : detections.findAll { it.getPathClass()?.toString() == '153: iba1' },
        'Unclassified': detections.findAll { it.getPathClass() == null || it.getPathClass().toString() == 'Unclassified' }
    ]

    print('  All: ' + detections.size() + '  153: ' + categories['153'].size() + '  iba1: ' + categories['iba1'].size() +
        '  coloc: ' + categories['coloc'].size() + '  unclassified: ' + categories['Unclassified'].size()) // one line summary of class counts for this image

    def isControl = entry.getImageName().startsWith('NC') // 'NC' filenames are Control
    def group = isControl ? 'Control' : 'Experimental'

    // Build one row per category for this image: image name, group, category label, cell count, then Cell: 153 mean and Cell: iba1 mean
    categories.each { categoryName, cells ->
        def row = [
            image   : entry.getImageName(),
            group   : group,
            category: categoryName,
            count   : cells.size()
        ]
        MEASUREMENT_KEYS.each { key ->
            row[key] = getAvg(cells, key)
        }
        allRows << row
    }
}

print('\nTotal image-category rows: ' + allRows.size())

// Helper to format one data row (one image, one category) into a CSV line
def formatRow = { r ->
    def parts = [r.image, r.group, r.category, r.count]
    MEASUREMENT_KEYS.each { key -> parts << String.format('%.4f', r[key] as double) }
    return parts.join(',')
}

// Helper to format a group-average row (one group x one category, averaged across that group's images)
def formatAvgRow = { label, group, categoryName, rows ->
    def parts = [label, group, categoryName, String.format('%.2f', getColAvg(rows, 'count') as double)]
    MEASUREMENT_KEYS.each { key -> parts << String.format('%.4f', getColAvg(rows, key) as double) }
    return parts.join(',')
}

// Header matches the column order built by formatRow/formatAvgRow above
def header = (['Image', 'Group', 'Category', 'Count'] + MEASUREMENT_KEYS).join(',')

def csvFile = new File(outputDir, 'cell_count_and_mean_intensity.csv')
csvFile.withWriter { writer ->

    writer.writeLine(header) // write the header

    // One row per image per category, grouped: all Control images first, then all Experimental images
    def controlRows = allRows.findAll { it.group == 'Control' }
    def experimentalRows = allRows.findAll { it.group == 'Experimental' }

    writer.writeLine('--- Control Group (NC) ---')
    for (def r : controlRows) {
        writer.writeLine(formatRow(r))
    }

    writer.writeLine('--- Experimental Group ---')
    for (def r : experimentalRows) {
        writer.writeLine(formatRow(r))
    }

    // Group averages: for each group (Control/Experimental) x each of the 5 categories,
    // average the count and Cell: 153/iba1 mean across that group's images.
    writer.writeLine('') // leave one row blank
    writer.writeLine((['--- Group Averages ---', 'Group', 'Category', 'Count'] + MEASUREMENT_KEYS).join(','))
    def categoryNames = ['All', '153', 'iba1', 'coloc', 'Unclassified']
    def groupMarkers = ['Control': '--- Control Group (NC) ---', 'Experimental': '--- Experimental Group ---']

    // Remember each group's average count per category as we go, so the percentage section below
    // can reuse these numbers directly instead of recalculating them.
    def avgCounts = [Control: [:], Experimental: [:]]

    ['Control', 'Experimental'].each { grp ->
        writer.writeLine(groupMarkers[grp])
        categoryNames.each { categoryName ->
            def subset = allRows.findAll { it.group == grp && it.category == categoryName }
            writer.writeLine(formatAvgRow(grp + ' Average', grp, categoryName, subset))
            avgCounts[grp][categoryName] = getColAvg(subset, 'count')
        }
    }

    // Percent composition per group: each classified category's average count as a percentage of
    // that group's average 'All' (total) cell count. E.g. % 153 (Control) = Control avg 153 count / Control avg All count * 100
    writer.writeLine('') // leave one row blank
    writer.writeLine('--- Percent Composition ---')
    writer.writeLine('Group,% 153,% iba1,% coloc,% Unclassified')
    ['Control', 'Experimental'].each { grp ->
        def allAvg = avgCounts[grp]['All']
        def pct153 = allAvg ? (avgCounts[grp]['153'] / allAvg * 100.0) : 0.0
        def pctIba1 = allAvg ? (avgCounts[grp]['iba1'] / allAvg * 100.0) : 0.0
        def pctColoc = allAvg ? (avgCounts[grp]['coloc'] / allAvg * 100.0) : 0.0
        def pctUnc = allAvg ? (avgCounts[grp]['Unclassified'] / allAvg * 100.0) : 0.0
        writer.writeLine(grp + ',' +
            String.format('%.2f', pct153 as double) + ',' +
            String.format('%.2f', pctIba1 as double) + ',' +
            String.format('%.2f', pctColoc as double) + ',' +
            String.format('%.2f', pctUnc as double))
    }
}

print('Done! Results saved to: ' + csvFile) // print for confirmation with path to csv file
