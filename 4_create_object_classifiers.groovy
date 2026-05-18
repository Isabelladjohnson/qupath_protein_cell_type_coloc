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

// Step 1: Create 63 single measurement classifier
def json63 = '''{
  "object_classifier_type": "SimpleClassifier",
  "function": {
    "classifier_fun": "ClassifyByMeasurementFunction",
    "measurement": "Cell: 63 mean",
    "pathClassEquals": "63",
    "pathClassAbove": "63",
    "threshold": 175.0
  },
  "pathClasses": [
    "63"
  ],
  "filter": "DETECTIONS_ALL",
  "timestamp": ''' + System.currentTimeMillis() + '''
}'''

new File(classifiersDir, '63.json').text = json63
print('Created classifier: 63')

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

// Step 3: Create composite classifier (coloc = 63 AND iba1)
def jsonColoc = '''{
  "object_classifier_type": "CompositeClassifier",
  "classifiers": [
    {
      "object_classifier_type": "SimpleClassifier",
      "function": {
        "classifier_fun": "ClassifyByMeasurementFunction",
        "measurement": "Cell: 63 mean",
        "pathClassEquals": "63",
        "pathClassAbove": "63",
        "threshold": 175.0
      },
      "pathClasses": [
        "63"
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
