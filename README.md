These are scripts to automate image analysis in QuPath-0.6.0-arm64 
Performing protein coloclaization in cells on images taken with 3 channels. In this script the channels are blue, green, and red.
First open QuPath and create the project, import all the files. The run the scrips in the following order - make changes for channel names, brightness, thresholds etc. as needed.
 1. set_channel_colors_no_change_in_min_max.groovy
2. adjust_channel_min_max.groovy (if needed to adjsut channel brightness) *this script must be ran individually for each image*
3. cell_detection.groovy
4. create_object_classifiers.groovy (set thresholds here)
5. load_classifiers.groovy
6. 6. add_intensity_calculation_and_export.groovy (exports the min, max, and mean channel intensity value for every cell in each image to a csv file).
clear_all_previous.groovy (clears all previous data. MUST be run between everything you edit values/thresholds to any of the scripts, then re-run the entire pipeline).

details for modifications within each script:
4_create_object_classifiers: lines 25 and 45 are where to set the measurement type such as channel, mean vs minimum value, and area such as entire cell or nucleus of cell. Lines 28 and 48 are where to select the threshold. Lines 33, 53, 6, 91 is where you select object filter as all detections, cells, or tiles.

6_add_intensity_features: 
