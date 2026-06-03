These are scripts to automate image analysis in QuPath-0.6.0-arm64 
Performing protein coloclaization in cells on images taken with 3 channels. In this script the channels are blue, green, and red.
First open QuPath and create the project, import all the files. The run the scrips in the following order - make changes for channel names, brightness, thresholds etc. as needed.
 1. set_channel_colors_no_change_in_min_max.groovy
2. adjust_channel_min_max.groovy (if needed to adjsut channel brightness)
3. cell_detection.groovy
4. create_object_classifiers.groovy (set thresholds here)
5. load_classifiers.groovy
