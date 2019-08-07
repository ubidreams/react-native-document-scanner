// External libs
import React, { Component } from 'react'
import PropTypes from 'prop-types'
import { NativeModules, Dimensions, StyleSheet, View, TouchableOpacity, Image, PanResponder } from 'react-native'
import { RNCamera } from 'react-native-camera'
import Svg, { Polygon } from 'react-native-svg'

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
      points: [],
      zoomOnPoint: null
    }
  }

  /**
   * Get image zoom style according to the current holding point
   */
  _getImageZoomStyleForCurrentHoldingPoint = () => {
    const { zoomOnPoint } = this.state
    const adjustment = ZOOM_CONTAINER_SIZE / 2

    return {
      marginLeft: -zoomOnPoint.x + adjustment - ZOOM_CURSOR_BORDER_SIZE,
      marginTop: -zoomOnPoint.y + adjustment - ZOOM_CURSOR_SIZE / 2
    }
  }

  /**
   * Get polygon from current points
   */
  _getPolygonPoints = () => {
    let pointsAsString = ''
    const { points } = this.state

    points.forEach((point, index) => {
      pointsAsString += `${point.x},${point.y}`

      if (index !== point.length - 1) {
        pointsAsString += ' '
      }
    })

    return pointsAsString
  }

  /**
   * Create PanResponder for given point.
   * Used for edge adjustment.
   * https://facebook.github.io/react-native/docs/0.59/panresponder
   * @param pointIndex
   * @return PanResponder
   */
  _createPanResponderForPoint = (pointIndex) => {
    const { points } = this.state

    return PanResponder.create({
      onStartShouldSetPanResponder: () => true,
      onMoveShouldSetPanResponder: () => true,
      onPanResponderGrant: () => {
        this.setState({ zoomOnPoint: points[pointIndex] })
      },
      onPanResponderMove: (evt, gestureState) => {
        this.setState({
          points: points.map((point, index) => {
            if (index === pointIndex) {
              return {
                x: point.x + gestureState.dx,
                y: point.y + gestureState.dy
              }
            } else {
              return point
            }
          }),
          zoomOnPoint: points[pointIndex]
        })
      },
      onPanResponderRelease: () => {
        this.setState({ zoomOnPoint: null })
      }
    })
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
    const points = await RNDocumentScanner.detectEdges(uri.replace('file://', ''), config)

    // update state
    this.setState({ photo: uri, points })
  }

  render () {
    const { photo, points, zoomOnPoint } = this.state

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

        {/* Image cropper (points) */}
        {points.map((point, index) => (
          <View
            key={index}
            style={[
              styles.imageCropperPointContainer,
              {
                top: point.y,
                left: point.x
              }
            ]}
            {...this._createPanResponderForPoint(index).panHandlers}
          >
            <View
              style={styles.imageCropperPoint}
            />
          </View>
        ))}

        {/* Image cropper (polygon) */}
        {points.length > 0 &&
          <Svg
            width={Dimensions.get('window').width}
            height={Dimensions.get('window').height}
            style={styles.imageCropperPolygonContainer}
          >
            <Polygon
              points={this._getPolygonPoints()}
              fill='transparent'
              stroke={CROPPER_COLOR}
              strokeWidth='1'
            />
          </Svg>
        }

        {/* Zoom on point holding */}
        {zoomOnPoint !== null &&
          <View style={styles.zoomContainer}>
            {/* Image */}
            <Image
              source={{ uri: photo }}
              style={[
                styles.zoomImage,
                this._getImageZoomStyleForCurrentHoldingPoint()
              ]}
              resizeMode='cover'
            />

            {/* Cursor */}
            <View style={styles.zoomCursor}>
              <View style={styles.zoomCursorHorizontal} />
              <View style={styles.zoomCursorVertical} />
            </View>
          </View>
        }
      </View>
    )
  }
}

const IMAGE_CROPPER_POINT_CONTAINER_SIZE = 40
const IMAGE_CROPPER_POINT_SIZE = 20

const CROPPER_COLOR = '#0082CA'

const ZOOM_CONTAINER_SIZE = 120
const ZOOM_CONTAINER_BORDER_WIDTH = 2
const ZOOM_CURSOR_SIZE = 10
const ZOOM_CURSOR_BORDER_SIZE = 1

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
  imageCropperPointContainer: {
    alignItems: 'center',
    justifyContent: 'center',
    position: 'absolute',
    width: IMAGE_CROPPER_POINT_CONTAINER_SIZE,
    height: IMAGE_CROPPER_POINT_CONTAINER_SIZE,
    marginTop: -IMAGE_CROPPER_POINT_CONTAINER_SIZE / 2,
    marginLeft: -IMAGE_CROPPER_POINT_CONTAINER_SIZE / 2
  },
  imageCropperPoint: {
    width: IMAGE_CROPPER_POINT_SIZE,
    height: IMAGE_CROPPER_POINT_SIZE,
    borderRadius: IMAGE_CROPPER_POINT_SIZE / 2,
    backgroundColor: 'rgba(255, 255, 255, 0.4)',
    borderWidth: 1,
    borderColor: CROPPER_COLOR
  },
  imageCropperPolygonContainer: {
    position: 'absolute',
    top: 0,
    left: 0
  },
  zoomContainer: {
    position: 'absolute',
    top: 0,
    left: 0,
    width: ZOOM_CONTAINER_SIZE,
    height: ZOOM_CONTAINER_SIZE,
    borderRadius: ZOOM_CONTAINER_SIZE / 2,
    borderColor: 'white',
    borderWidth: ZOOM_CONTAINER_BORDER_WIDTH,
    overflow: 'hidden',
    backgroundColor: 'black'
  },
  zoomImage: {
    width: Dimensions.get('window').width,
    height: Dimensions.get('window').height
  },
  zoomCursor: {
    position: 'absolute',
    alignItems: 'center',
    justifyContent: 'center',
    width: '100%',
    height: '100%'
  },
  zoomCursorHorizontal: {
    width: ZOOM_CURSOR_SIZE,
    height: ZOOM_CURSOR_BORDER_SIZE,
    backgroundColor: CROPPER_COLOR
  },
  zoomCursorVertical: {
    width: ZOOM_CURSOR_BORDER_SIZE,
    height: ZOOM_CURSOR_SIZE,
    marginTop: -ZOOM_CURSOR_SIZE / 2,
    backgroundColor: CROPPER_COLOR
  }
})

export default DocumentScanner
