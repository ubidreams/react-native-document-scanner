
# react-native-document-scanner

## Getting started

`$ yarn add https://github.com/ubidreams/react-native-document-scanner`

### Mostly automatic installation

`$ react-native link react-native-document-scanner`

### Manual installation


#### iOS

1. In XCode, in the project navigator, right click `Libraries` ➜ `Add Files to [your project's name]`
2. Go to `node_modules` ➜ `react-native-document-scanner` and add `RNDocumentScanner.xcodeproj`
3. In XCode, in the project navigator, select your project. Add `libRNDocumentScanner.a` to your project's `Build Phases` ➜ `Link Binary With Libraries`
4. Add OpenCV in your `Podfile` :
  	```
    ...
      # Pods for [your project]
      pod 'OpenCV', '~> 3.4.2'
    end
  	```
5. Run `pod install` in your ios folder
6. Run your project (`Cmd+R`)<

#### Android

1. Open up `android/app/src/main/java/[...]/MainActivity.java`
  - Add `import com.reactlibrary.RNDocumentScannerPackage;` to the imports at the top of the file
  - Add `new RNDocumentScannerPackage()` to the list returned by the `getPackages()` method
2. Append the following lines to `android/settings.gradle`:
  	```
  	include ':react-native-document-scanner'
  	project(':react-native-document-scanner').projectDir = new File(rootProject.projectDir, '../node_modules/react-native-document-scanner/android')
  	```
3. Insert the following lines inside the dependencies block in `android/app/build.gradle`:
  	```
      compile project(':react-native-document-scanner')
  	```

## Usage
```javascript
import RNDocumentScanner from 'react-native-document-scanner'

render () {
  return (
    <View style={styles.container}>
      <RNDocumentScanner />
    </View>
  )
}
```

## Credits
- for iOS : [SmartCrop](https://github.com/kronik/smartcrop)
- for Android : [SimpleDocumentScanner-Android](https://github.com/jbttn/SimpleDocumentScanner-Android)
- for image cropper component : [react-native-perspective-image-cropper](https://github.com/Michaelvilleneuve/react-native-perspective-image-cropper)
