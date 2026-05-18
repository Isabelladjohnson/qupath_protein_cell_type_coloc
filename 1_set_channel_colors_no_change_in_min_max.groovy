import qupath.lib.display.ChannelDisplayInfo

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

    // Step 1: Set image type to Fluorescence
    imageData.setImageType(qupath.lib.images.ImageData.ImageType.FLUORESCENCE)
    print('  Image type set to Fluorescence.')

    // Step 2: Set channel names and colors
    def server = imageData.getServer()
    def metadata = server.getMetadata()
    def channels = metadata.getChannels()

    def newChannels = [
        qupath.lib.images.servers.ImageChannel.getInstance('dapi', qupath.lib.common.ColorTools.packRGB(0, 0, 255)),       // CH1 - Blue
        qupath.lib.images.servers.ImageChannel.getInstance('63', qupath.lib.common.ColorTools.packRGB(0, 255, 0)),         // CH2 - Lime
        qupath.lib.images.servers.ImageChannel.getInstance('iba1', qupath.lib.common.ColorTools.packRGB(255, 0, 0))        // CH3 - Red
    ]

    def newMetadata = new qupath.lib.images.servers.ImageServerMetadata.Builder(metadata)
        .channels(newChannels)
        .build()

    server.setMetadata(newMetadata)
    print('  Channels set: dapi (Blue), 63 (Lime), iba1 (Red).')

    entry.saveImageData(imageData)
    print('  Saved.')
}

print('Done!')
