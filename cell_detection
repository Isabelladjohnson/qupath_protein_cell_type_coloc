def project = getProject()
if (project == null) {
    print('ERROR: No project is open.')
    return
}

def projectEntries = project.getImageList()
if (projectEntries.isEmpty()) {
    print('ERROR: No images found in the project.')
    return
}

print('Found ' + projectEntries.size() + ' image(s) in project.')

for (def entry : projectEntries) {
    print('Processing: ' + entry.getImageName())

    def imageData = entry.readImageData()
    if (imageData == null) {
        print('  SKIPPED: Could not read image data.')
        continue
    }

    // Step 1: Create full image annotation
    def hierarchy = imageData.getHierarchy()
    def server = imageData.getServer()
    def fullAnnotation = qupath.lib.objects.PathObjects.createAnnotationObject(
        qupath.lib.roi.ROIs.createRectangleROI(0, 0, server.getWidth(), server.getHeight(), qupath.lib.regions.ImagePlane.getDefaultPlane())
    )
    hierarchy.addObject(fullAnnotation)
    hierarchy.getSelectionModel().setSelectedObject(fullAnnotation)
    print('  Created full image annotation.')

    // Step 2: Run cell detection with specified parameters
    qupath.lib.scripting.QP.runPlugin(
        'qupath.imagej.detect.cells.WatershedCellDetection',
        imageData,
        '{"detectionImageBrightfield": "Hematoxylin OD",' +
        '"detectionImage": "dapi (C1)",' +
        '"requestedPixelSizeMicrons": 0.5,' +
        '"backgroundRadiusMicrons": 8.0,' +
        '"backgroundByReconstruction": true,' +
        '"medianRadiusMicrons": 0.0,' +
        '"sigmaMicrons": 1.5,' +
        '"minAreaMicrons": 10.0,' +
        '"maxAreaMicrons": 400.0,' +
        '"threshold": 100.0,' +
        '"splitByShape": true,' +
        '"cellExpansionMicrons": 3.0,' +
        '"includeNuclei": true,' +
        '"smoothBoundaries": true,' +
        '"makeMeasurements": true}'
    )
    print('  Cell detection complete.')

    entry.saveImageData(imageData)
    print('  Saved.')
}

print('Done!')
