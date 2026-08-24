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
    .toFile()

if (!classifiersDir.exists()) {
    classifiersDir.mkdirs()
    print('Created classifiers directory.')
}

// Step 1: Create protein single measurement classifier
def jsonProtein = '''{
  "object_classifier_type": "SimpleClassifier",
  "function": {
    "classifier_fun": "ClassifyByMeasurementFunction",
    "measurement": "Cell: protein mean",
    "pathClassEquals": "protein",
    "pathClassAbove": "protein",
    "threshold": 175.0
  },
  "pathClasses": [
    "protein"
  ],
  "filter": "DETECTIONS_ALL",
  "timestamp": ''' + System.currentTimeMillis() + '''
}'''

new File(classifiersDir, 'protein.json').text = jsonProtein
print('Created classifier: protein')

// Step 2: Create iba1 single measurement classifier
def jsonIba1 = '''{
  "object_classifier_type": "SimpleClassifier",
  "function": {
    "classifier_fun": "ClassifyByMeasurementFunction",
    "measurement": "Cell: iba1 mean",
    "pathClassEquals": "iba1",
    "pathClassAbove": "iba1",
    "threshold": 550.0
  },
  "pathClasses": [
    "iba1"
  ],
  "filter": "DETECTIONS_ALL",
  "timestamp": ''' + System.currentTimeMillis() + '''
}'''

new File(classifiersDir, 'iba1.json').text = jsonIba1
print('Created classifier: iba1')

// Step 3: Create composite classifier (coloc = protein AND iba1)
def jsonColoc = '''{
  "object_classifier_type": "CompositeClassifier",
  "classifiers": [
    {
      "object_classifier_type": "SimpleClassifier",
      "function": {
        "classifier_fun": "ClassifyByMeasurementFunction",
        "measurement": "Cell: protein mean",
        "pathClassEquals": "protein",
        "pathClassAbove": "protein",
        "threshold": 175.0
      },
      "pathClasses": [
        "protein"
      ],
      "filter": "DETECTIONS_ALL",
      "timestamp": ''' + System.currentTimeMillis() + '''
    },
    {
      "object_classifier_type": "SimpleClassifier",
      "function": {
        "classifier_fun": "ClassifyByMeasurementFunction",
        "measurement": "Cell: iba1 mean",
        "pathClassEquals": "iba1",
        "pathClassAbove": "iba1",
        "threshold": 550.0
      },
      "pathClasses": [
        "iba1"
      ],
      "filter": "DETECTIONS_ALL",
      "timestamp": ''' + System.currentTimeMillis() + '''
    }
  ]
}'''

new File(classifiersDir, 'coloc.json').text = jsonColoc
print('Created classifier: coloc')

print('Done! All classifiers saved to project.')
