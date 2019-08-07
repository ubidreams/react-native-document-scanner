// External libs
import React, { Component } from 'react'
import PropTypes from 'prop-types'
import { NativeModules, Dimensions, StyleSheet, View, TouchableOpacity, Image } from 'react-native'
import { RNCamera } from 'react-native-camera'

// Native modules
const { RNDocumentScanner } = NativeModules

class DocumentScanner extends Component {
  static propTypes = {
    RNCameraProps: PropTypes.object
  }

  static defaultProps = {
    RNCameraProps: {}
  }

  constructor (props) {
    super(props)

    this.state = {
      photo: null,
      points: []
    }
  }

  /**
   * When capture button is clicked
   * @param camera
   */
  _handlePressCapture = async (camera) => {
    // capture photo
    const options = { base64: false }
    const { uri } = await camera.takePictureAsync(options)

    // attempt to identify document from opencv
    const { width, height } = Dimensions.get('window')
    const config = { screenWidth: width, screenHeight: height }
    const points = await RNDocumentScanner.getLargestSquarePoints(uri.replace('file://', ''), config)

    // update state
    this.setState({ photo: uri, points })
  }

  render () {
    const { photo, points } = this.state

    return (
      <View style={styles.container}>
        {/* Camera */}
        {photo === null &&
          <RNCamera
            style={styles.camera}
            type={RNCamera.Constants.Type.back}
            captureAudio={false}
          >
            {({ camera }) => {
              // Capture button
              return (
                <TouchableOpacity
                  activeOpacity={0.6}
                  onPress={() => this._handlePressCapture(camera)}
                  style={styles.captureBtn}
                />
              )
            }}
          </RNCamera>
        }

        {/* Photo */}
        {photo !== null &&
          <Image
            source={{ uri: photo }}
            style={styles.photo}
          />
        }

        {/* Image cropper */}
        {points.map((point, index) => (
          <View
            key={index}
            style={[
              styles.imageCropperPoint,
              {
                top: point.y,
                left: point.x
              }
            ]}
          />
        ))}
      </View>
    )
  }
}

const styles = StyleSheet.create({
  container: {
    flex: 1
  },
  camera: {
    flex: 1
  },
  captureBtn: {
    alignSelf: 'center',
    position: 'absolute',
    bottom: 40,
    width: 60,
    height: 60,
    borderRadius: 30,
    backgroundColor: 'white',
    borderWidth: 5,
    borderColor: '#c2c2c2'
  },
  photo: {
    flex: 1
  },
  imageCropperPoint: {
    position: 'absolute',
    width: 20,
    height: 20,
    marginTop: -11,
    marginLeft: -11,
    borderRadius: 10,
    backgroundColor: 'white',
    borderWidth: 1,
    borderColor: 'black'
  }
})

export default DocumentScanner
