def project = getProject() // open the project
if (project == null) {
    print('ERROR: No project is open.')
    return
}

def outputDir = new File(project.getPath().getParent().toFile(), 'results') //confirem results folder exists or make it
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

// The two channels/markers and two measurement compartments we want intensity stats for.
// Channel names stay exactly as used everywhere else in the pipeline: '153' and 'iba1'.
def CHANNELS = ['153', 'iba1']
def COMPARTMENTS = ['Cell', 'Nucleus']
def STATS = ['mean']

// Build the ordered list of QuPath measurement names we'll average for every category,
// e.g. 'Cell: 153 mean', 'Cell: iba1 mean', 'Nucleus: 153 mean', 'Nucleus: iba1 mean'
// (min/max dropped to keep the output manageable - only mean is pulled now)
def MEASUREMENT_KEYS = []
COMPARTMENTS.each { compartment ->
    CHANNELS.each { channel ->
        STATS.each { stat ->
            MEASUREMENT_KEYS << "${compartment}: ${channel} ${stat}"
        }
    }
}

// Collect all results - main per image loop. Unlike before, we now build ONE ROW PER (image, category)
// pair instead of one wide row per image, so allRows ends up 5x longer than the number of images (long/tidy format).
def allRows = []

for (def entry : projectEntries) {
    print('Processing: ' + entry.getImageName()) // print image name

    def imageData = entry.readImageData()
    if (imageData == null) {
        print('  SKIPPED: Could not read image data.') // skip if there is an issue with reading image data
        continue
    }

    def hierarchy = imageData.getHierarchy() // uses QuPath's PathObjectHierarchy data structure for every image with every annotation, detected cell, and organization.
    def detections = hierarchy.getDetectionObjects()

    if (detections.isEmpty()) {
        print('  SKIPPED: No detections found.')
        continue
    }

    // Define the 5 categories for this image. 
    def categories = [
        'All'         : detections, // all cells detected
        '153'         : detections.findAll { it.getPathClass()?.toString() == '153' }, // cells detected as only 153 + 
        'iba1'        : detections.findAll { it.getPathClass()?.toString() == 'iba1' }, // cells detecetd as only iba1 +
        'coloc'       : detections.findAll { it.getPathClass()?.toString() == '153: iba1' }, // colocalized cells detected to have both iba1 and 153
        'Unclassified': detections.findAll { it.getPathClass() == null || it.getPathClass().toString() == 'Unclassified' } // all other cells
    ]

    print('  All: ' + detections.size() + '  153: ' + categories['153'].size() + '  iba1: ' + categories['iba1'].size() +
        '  coloc: ' + categories['coloc'].size() + '  unclassified: ' + categories['Unclassified'].size()) // print a one line summary of the class counts for this image

    def isControl = entry.getImageName().startsWith('NC') // if the image name starts with 'NC' label it as 'Control'
    def group = isControl ? 'Control' : 'Experimental' // every other imaeg is 'Experimental'

    // Build one row per category for this image: image name, group, category label, cell count,
    // then the Cell:/Nucleus: x 153/iba1 mean average for that category's cells.
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

// Helper to format a group-average row (one group x one category, averaged across that group's images) (ex: average control cell count = ( image 1 all cell counts + image 2 all cell counts) / (2) )
def formatAvgRow = { label, group, categoryName, rows ->
    def parts = [label, group, categoryName, String.format('%.2f', getColAvg(rows, 'count') as double)]
    MEASUREMENT_KEYS.each { key -> parts << String.format('%.4f', getColAvg(rows, key) as double) }
    return parts.join(',')
}

// Header matches the column order built by formatRow/formatAvgRow above
def header = (['Image', 'Group', 'Category', 'Count'] + MEASUREMENT_KEYS).join(',')

def csvFile = new File(outputDir, 'all_classes_intensity_long.csv') // new filename so this doesn't overwrite the old wide-format export
csvFile.withWriter { writer ->

    writer.writeLine(header) // write the header

    // One row per image per category, grouped like the original script 10: all Control images first, then all Experimental images
    def controlRows = allRows.findAll { it.group == 'Control' }
    def experimentalRows = allRows.findAll { it.group == 'Experimental' }

    writer.writeLine('--- Control Group (NC) ---') // control section marker
    for (def r : controlRows) {
        writer.writeLine(formatRow(r))
    }

    writer.writeLine('--- Experimental Group ---') // experimental section marker
    for (def r : experimentalRows) {
        writer.writeLine(formatRow(r))
    }

    // Group averages: for each group (Control/Experimental) x each of the 5 categories,
    // average the count and every intensity measurement across that group's images.
    writer.writeLine('') // leave one row blank
    // 'Group Averages' title in column A, with the same column labels as the header (Group, Category, Count, Cell: 153 mean, ...) repeated in columns B-H for easy reference
    writer.writeLine((['--- Group Averages ---', 'Group', 'Category', 'Count'] + MEASUREMENT_KEYS).join(','))
    def categoryNames = ['All', '153', 'iba1', 'coloc', 'Unclassified']
    def groupMarkers = ['Control': '--- Control Group (NC) ---', 'Experimental': '--- Experimental Group ---']
    ['Control', 'Experimental'].each { grp ->
        writer.writeLine(groupMarkers[grp]) // same Control/Experimental section marker style as the rows above
        categoryNames.each { categoryName ->
            def subset = allRows.findAll { it.group == grp && it.category == categoryName }
            writer.writeLine(formatAvgRow(grp + ' Average', grp, categoryName, subset))
        }
    }
}

print('Done! Results saved to: ' + csvFile) // print for confirmation with path to csv file
