// External libs
import React, { Component } from 'react'
import { StyleSheet, View, TouchableOpacity, Image } from 'react-native'
import RNDocumentScanner from 'react-native-document-scanner'
import FontAwesome from 'react-native-vector-icons/FontAwesome'

export default class App extends Component {
  constructor (props) {
    super(props)

    this.state = {
      isCropping: false,
      image: null
    }
  }

  /**
   * When confirm button is clicked
   */
  _handlePressConfirmScan = () => {
    this.scanner.cropImage().then(({ image }) => {
      this.setState({ image })
    })
  }

  render () {
    const { isCropping, image } = this.state

    if (image === null) {
      return (
        <View style={styles.container}>
          {/* Document scanner */}
          <RNDocumentScanner
            style={styles.documentScanner}
            ref={(ref) => (this.scanner = ref)}
            onCapture={() => this.setState({ isCropping: true })}
            androidCameraPermissionOptions={{
              title: 'Permission to use camera',
              message: 'We need your permission to use your camera',
              buttonPositive: 'Ok',
              buttonNegative: 'Cancel'
            }}
          />

          {/* Preview buttons */}
          {isCropping &&
            <View style={styles.previewButtons}>
              {/* Button to confirm scan */}
              <TouchableOpacity
                style={styles.previewButton}
                onPress={() => this._handlePressConfirmScan()}
              >
                <FontAwesome
                  name='check-circle'
                  size={28}
                  color='black'
                />
              </TouchableOpacity>

              {/* Button to restart scan */}
              <TouchableOpacity
                style={styles.previewButton}
                onPress={() => this.scanner.restart()}
              >
                <FontAwesome
                  name='undo'
                  size={28}
                  color='black'
                />
              </TouchableOpacity>
            </View>
          }
        </View>

      )
    } else {
      return (
        <Image
          style={styles.container}
          source={{ uri: image }}
          resizeMode='contain'
        />
      )
    }
  }
}

const styles = StyleSheet.create({
  container: {
    flex: 1
  },
  documentScanner: {
    flex: 1
  },
  previewButtons: {
    position: 'absolute',
    top: 0,
    bottom: 0,
    right: 15,
    zIndex: 5,
    elevation: 5,
    alignItems: 'center',
    justifyContent: 'flex-end'
  },
  previewButton: {
    alignItems: 'center',
    justifyContent: 'center',
    width: 48,
    height: 48,
    marginBottom: 15,
    borderRadius: 24,
    backgroundColor: 'rgba(255, 255, 255, 0.75)'
  }
})
