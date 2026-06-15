These are scripts to automate image analysis in QuPath-0.6.0-arm64 
Performing protein coloclaization in cells on images taken with 3 channels. In this script the channels are blue, green, and red.
First open QuPath and create the project, import all the files. The run the scrips in the following order - make changes for channel names, brightness, thresholds etc. as needed.
 1. set_channel_colors_no_change_in_min_max.groovy
2. adjust_channel_min_max.groovy (if needed to adjsut channel brightness, skip this script if no chnages needed)
3. cell_detection.groovy
4. create_object_classifiers.groovy
5. load_classifiers.groovy
6. add_intensity_features.groovy (looking at the max intensity of green channel per cell)
7. export_intensity_features (exports the values in csv files in results folder)
8. export_comparison (exports a csv file that takes the avg max intensity channel of the cells between the 2 treatment groups)
9. export_groups_comparisons.groovy (This file exports a csv file which gives the average max intensity per cell values from the control and experimental cells which are both colocalized and also only expressing green channel)
10. export_all_comparisons.groovy (this file exports a csv file whihc shows the mean, min, and max intensity values for each image and a summary at the bottom for average mean, min, mmax values per treatement group.)
