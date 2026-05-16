import com.google.gson.GsonBuilder
import java.nio.file.Files

def project = getProject()
if (project == null) {
    print('ERROR: No project is open.')
    return
}

// Set up the classifiers directory
def classifiersDir = project.getPath().getParent()
    .resolve('classifiers')
    .resolve('object_classifiers')

if (!classifiersDir.toFile().exists()) {
    classifiersDir.toFile().mkdirs()
    print('Created classifiers directory.')
}

// Step 1: Create 63 single measurement classifier JSON
def classifier63 = '''{
  "object_filter": "DETECTION",
  "measurement_filter": null,
  "training_class": null,
  "threshold_measurement": "Cell: 63 mean",
  "below_class": "Unclassified",
  "above_class": "63",
  "threshold": 175.0,
  "channel_filter": "63"
}'''

def path63 = classifiersDir.resolve('63.json')
Files.writeString(path63, classifier63)
print('Created classifier: 63')

// Step 2: Create iba1 single measurement classifier JSON
def classifierIba1 = '''{
  "object_filter": "DETECTION",
  "measurement_filter": null,
  "training_class": null,
  "threshold_measurement": "Cell: iba1 mean",
  "below_class": "Unclassified",
  "above_class": "iba1",
  "threshold": 550.0,
  "channel_filter": "iba1"
}'''

def pathIba1 = classifiersDir.resolve('iba1.json')
Files.writeString(pathIba1, classifierIba1)
print('Created classifier: iba1')

// Step 3: Create composite classifier JSON
def classifierColoc = '''{
  "classifiers": ["63", "iba1"]
}'''

def pathColoc = classifiersDir.resolve('coloc.json')
Files.writeString(pathColoc, classifierColoc)
print('Created classifier: coloc')

print('Done! All classifiers saved to project.')
