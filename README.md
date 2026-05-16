These are scripts to automate image analysis in QuPath-0.6.0-arm64 
Performing protein coloclaization in cells on images takne with 3 channels. In this script the channels are blue, green, and red.
First open QuPath and create the project, import all the files. The run the scrips in the following order - make chnages for channel names, brightness, thresholds etc. as needed.
set_channel_colors_no_change_in_min.groovy
adjust_channel_min_max.groovy (if needed to ajsut channel brightness)
cell_detection.groovy
create_object_classifiers.groovy
load_classifiers.groovy
