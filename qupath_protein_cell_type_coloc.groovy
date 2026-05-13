def classifierNames = ['protein', 'cell_type', 'coloc']

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

    for (def classifierName : classifierNames) {
        try {
            def classifier = project.getObjectClassifiers().get(classifierName)
            if (classifier == null) {
                print('  WARNING: Classifier not found: ' + classifierName)
                continue
            }
            classifier.classifyObjects(imageData, true)
            imageData.getHierarchy().fireHierarchyChangedEvent(null)
            print('  Applied: ' + classifierName)
        } catch (Exception e) {
            print('  ERROR with ' + classifierName + ': ' + e.getMessage())
        }
    }

    entry.saveImageData(imageData)
    print('  Saved.')
}

print('Done!')
