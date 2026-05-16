def viewer = getCurrentViewer()
def display = viewer.getImageDisplay()

for (def channel : display.availableChannels()) {
    def name = channel.getName()
    if (name == '63 (C2)') {
        display.setMinMaxDisplay(channel, 0, 2000)
        print('63: min=0, max=2000')
    } else if (name == 'iba1 (C3)') {
        display.setMinMaxDisplay(channel, 0, 1200)
        print('iba1: min=0, max=1200')
    }
}
