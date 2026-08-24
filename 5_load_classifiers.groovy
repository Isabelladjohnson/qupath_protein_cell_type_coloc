def classifierNames = ['protein', 'iba1', 'coloc'] // apply classifiers from previous script
// open project
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

print('Found ' + projectEntries.size() + ' image(s) in project.') // print nuber of images
// loop over all images
for (def entry : projectEntries) {
    print('Processing: ' + entry.getImageName()) // print image being proccessed
    def imageData = entry.readImageData() //load image or print SKIPPED if image can not be read
    if (imageData == null) {
        print('  SKIPPED: Could not read image data.')
        continue
    }
    for (def classifierName : classifierNames) { // apply each classifier one at a time on each image
        try {
            def classifier = project.getObjectClassifiers().get(classifierName)
            if (classifier == null) {
                print('  WARNING: Classifier not found: ' + classifierName)
                continue
            }
            classifier.classifyObjects(imageData, true) // reset existing class on QuPath to avaid re-running and accumulating previous classes from past runs
            imageData.getHierarchy().fireHierarchyChangedEvent(null)
            print('  Applied: ' + classifierName) // prin classifier name to confirm or if there was an error
        } catch (Exception e) {
            print('  ERROR with ' + classifierName + ': ' + e.getMessage())
        }
    }
    entry.saveImageData(imageData) // saves the data
    print('  Saved.')
}

print('Done!')
