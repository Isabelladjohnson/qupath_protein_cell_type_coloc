def project = getProject()

for (def entry : project.getImageList()) {
    print('Clearing: ' + entry.getImageName())
    def imageData = entry.readImageData()
    def hierarchy = imageData.getHierarchy()
    hierarchy.clearAll()
    hierarchy.fireHierarchyChangedEvent(null)
    entry.saveImageData(imageData)
    print('  Cleared.')
}
print('Done!')
