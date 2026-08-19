/*
 * Counts cells per class for every image in the project, grouped into
 * control (image name starts with "NC") and experimental, in the same
 * layout as 10_export_all_comparisons.groovy.
 *
 * Run AFTER: 1_set_channel_colors, 3_cell_detection (63 detection),
 *            4_create_object_classifiers, 5_load_classifiers
 *
 * Output: results/cell_counts.csv
 *
 * Class names produced by the classifiers:
 *   "63"        -> 63-positive only
 *   "iba1"      -> iba1-positive only
 *   "63: iba1"  -> colocalized (both)  [composite classifier output]
 *   null        -> unclassified (neither above threshold)
 */

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
if (projectEntries.isEmpty()) {
    print('ERROR: No images found in the project.')
    return
}
print('Found ' + projectEntries.size() + ' image(s) in project.')

// Normalise a PathClass to one of: '63', 'iba1', 'coloc', 'unclassified'
def classifyBucket = { pathObject ->
    def pc = pathObject.getPathClass()
    if (pc == null)
        return 'unclassified'
    def name = pc.toString().trim()
    if (name.isEmpty() || name.equalsIgnoreCase('Unclassified'))
        return 'unclassified'
    // Split composite classes like "63: iba1" into their parts
    def parts = name.split(':').collect { it.trim() }.findAll { it }
    def has63 = parts.any { it == '63' }
    def hasIba1 = parts.any { it == 'iba1' }
    if (has63 && hasIba1) return 'coloc'
    if (has63) return '63'
    if (hasIba1) return 'iba1'
    return 'unclassified'
}

// ---- Collect per-image counts ----
def allResults = []

for (def entry : projectEntries) {
    print('Processing: ' + entry.getImageName())

    def imageData = entry.readImageData()
    if (imageData == null) {
        print('  SKIPPED: Could not read image data.')
        continue
    }

    def detections = imageData.getHierarchy().getDetectionObjects()
    if (detections.isEmpty())
        print('  WARNING: No detections found.')

    int n63 = 0, nIba1 = 0, nColoc = 0, nUnc = 0
    for (def d : detections) {
        switch (classifyBucket(d)) {
            case '63':    n63++;    break
            case 'iba1':  nIba1++;  break
            case 'coloc': nColoc++; break
            default:      nUnc++;   break
        }
    }

    allResults.add([
        image: entry.getImageName(),
        isControl: entry.getImageName().startsWith('NC'),
        total: detections.size(),
        n63: n63,
        nIba1: nIba1,
        nColoc: nColoc,
        nUnc: nUnc,
        total63pos: n63 + nColoc      // every cell expressing 63
    ])

    print('  Total: ' + detections.size() + '  |  63 only: ' + n63 + '  iba1 only: ' + nIba1 +
          '  coloc: ' + nColoc + '  unclassified: ' + nUnc)
}

def controlResults = allResults.findAll { it.isControl }
def experimentalResults = allResults.findAll { !it.isControl }

print('\nControl images (NC): ' + controlResults.size())
print('Experimental images: ' + experimentalResults.size())

// ---- Helpers ----
def pct = { num, den -> den > 0 ? String.format('%.2f', num / (double) den * 100.0) : '0.00' }

def getColAvg = { rows, key ->
    def values = rows.collect { it[key] }.findAll { it != null }
    return values ? (values.sum() as double) / values.size() : 0.0
}

def getColSum = { rows, key ->
    def values = rows.collect { it[key] }.findAll { it != null }
    return values ? (values.sum() as int) : 0
}

// One data row per image
def formatRow = { r ->
    return '"' + r.image + '",' +
        r.total + ',' +
        r.n63 + ',' +
        r.nIba1 + ',' +
        r.nColoc + ',' +
        r.nUnc + ',' +
        r.total63pos + ',' +
        pct(r.nColoc, r.total63pos) + ',' +
        pct(r.n63, r.total63pos) + ',' +
        pct(r.n63, r.total) + ',' +
        pct(r.nIba1, r.total) + ',' +
        pct(r.nColoc, r.total) + ',' +
        pct(r.nUnc, r.total)
}

// Mean count per image within a group; percentages recomputed from the means
def formatAvgRow = { label, rows ->
    double aTotal = getColAvg(rows, 'total')
    double a63 = getColAvg(rows, 'n63')
    double aIba1 = getColAvg(rows, 'nIba1')
    double aColoc = getColAvg(rows, 'nColoc')
    double aUnc = getColAvg(rows, 'nUnc')
    double a63pos = getColAvg(rows, 'total63pos')
    return '"' + label + '",' +
        String.format('%.2f', aTotal) + ',' +
        String.format('%.2f', a63) + ',' +
        String.format('%.2f', aIba1) + ',' +
        String.format('%.2f', aColoc) + ',' +
        String.format('%.2f', aUnc) + ',' +
        String.format('%.2f', a63pos) + ',' +
        pct(aColoc, a63pos) + ',' +
        pct(a63, a63pos) + ',' +
        pct(a63, aTotal) + ',' +
        pct(aIba1, aTotal) + ',' +
        pct(aColoc, aTotal) + ',' +
        pct(aUnc, aTotal)
}

// Pooled totals within a group; percentages from the summed counts
def formatSumRow = { label, rows ->
    int sTotal = getColSum(rows, 'total')
    int s63 = getColSum(rows, 'n63')
    int sIba1 = getColSum(rows, 'nIba1')
    int sColoc = getColSum(rows, 'nColoc')
    int sUnc = getColSum(rows, 'nUnc')
    int s63pos = getColSum(rows, 'total63pos')
    return '"' + label + '",' +
        sTotal + ',' + s63 + ',' + sIba1 + ',' + sColoc + ',' + sUnc + ',' + s63pos + ',' +
        pct(sColoc, s63pos) + ',' +
        pct(s63, s63pos) + ',' +
        pct(s63, sTotal) + ',' +
        pct(sIba1, sTotal) + ',' +
        pct(sColoc, sTotal) + ',' +
        pct(sUnc, sTotal)
}

def header = 'Image,Total cells,Num 63 only,Num iba1 only,Num coloc (63: iba1),' +
    'Num unclassified,Total 63-expressing (63 only + coloc),' +
    'Percent of 63-expressing that are iba1,Percent of 63-expressing that are not iba1,' +
    'Percent 63 only,Percent iba1 only,Percent coloc,Percent unclassified'

// ---- Write CSV ----
def csvFile = new File(outputDir, 'cell_counts.csv')
csvFile.withWriter { writer ->

    writer.writeLine(header)

    writer.writeLine('--- Control Group (NC) ---')
    for (def r : controlResults)
        writer.writeLine(formatRow(r))

    writer.writeLine('--- Experimental Group ---')
    for (def r : experimentalResults)
        writer.writeLine(formatRow(r))

    // Mean per image (each image weighted equally)
    writer.writeLine('')
    writer.writeLine('--- Group Averages (mean per image) ---')
    writer.writeLine(formatAvgRow('Control Average (NC)', controlResults))
    writer.writeLine(formatAvgRow('Experimental Average', experimentalResults))

    // Pooled totals (each cell weighted equally)
    writer.writeLine('')
    writer.writeLine('--- Group Totals (pooled cells) ---')
    writer.writeLine(formatSumRow('Control Total (NC)', controlResults))
    writer.writeLine(formatSumRow('Experimental Total', experimentalResults))
    writer.writeLine(formatSumRow('All Images Total', allResults))
}

print('\nDone! Results saved to: ' + csvFile)
