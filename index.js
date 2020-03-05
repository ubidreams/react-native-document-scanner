// External libs
import React, { Component } from 'react'
import PropTypes from 'prop-types'
import { requireNativeComponent, NativeModules, PermissionsAndroid, StyleSheet, View, Text, PanResponder, PixelRatio } from 'react-native'
import Svg, { Polygon } from 'react-native-svg'

// Native modules
const RNDocumentScannerModule = NativeModules.RNDocumentScannerModule

// Native components
const RNDocumentScanner = requireNativeComponent('RNDocumentScanner')

class DocumentScanner extends Component {
  static propTypes = {
    scanHintOptions: PropTypes.object,
    androidCameraPermissionOptions: PropTypes.object,
    onCapture: PropTypes.func
  }

  static defaultProps = {
    scanHintOptions: {
      findRect: null,
      moveCloser: {
        color: '#ff9c00',
        message: 'Move closer'
      },
      moveAway: {
        color: '#ff9c00',
        message: 'Move away'
      },
      adjustAngle: {
        color: '#ff9c00',
        message: 'Adjust angle'
      },
      capturingImage: {
        color: '#26d84c',
        message: 'Still hold'
      }
    },
    androidCameraPermissionOptions: {
      title: '',
      message: ''
    },
    onCapture: () => {}
  }

  constructor (props) {
    super(props)

    this.initialState = {
      ready: false,
      scanHint: null,
      points: []
    }

    this.state = {
      ...this.initialState
    }
  }

  componentDidMount = async () => {
    const { androidCameraPermissionOptions } = this.props

    const granted = await PermissionsAndroid.request(
      PermissionsAndroid.PERMISSIONS.CAMERA,
      androidCameraPermissionOptions
    )

    this.setState({
      ready: granted === PermissionsAndroid.RESULTS.GRANTED
    })
  }

  /**
   * Allow to restart and scan document again
   */
  restart = () => {
    this.setState(this.initialState, () => {
      setTimeout(() => this.setState({ ready: true }), 25)
    })
  }

  /**
   * Start image cropping according to current points and return path of cached file
   * @param options = {
   *    width: Number
   *    height: Number,
   *    thumbnail: Boolean
   * }
   * @return Promise
   */
  cropImage = (options = {}) => {
    const finalOptions = {
      width: -1,
      height: -1,
      thumbnail: false,
      ...options
    }

    return RNDocumentScannerModule.crop(
      this.state.points.map((point) => ({
        x: point.x * PixelRatio.get(),
        y: point.y * PixelRatio.get()
      })),
      finalOptions
    )
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

  render () {
    const { scanHintOptions, onCapture } = this.props
    const { ready, scanHint, points } = this.state

    return (
      <View
        style={styles.container}
      >
        {/* Document scanner */}
        {ready &&
          <RNDocumentScanner
            scanHintOptions={scanHintOptions}
            style={styles.documentScanner}
            displayHint={(event) => {
              if (event.nativeEvent.type !== undefined) {
                this.setState({
                  scanHint: scanHintOptions[event.nativeEvent.type]
                })
              }
            }}
            onPictureClicked={(event) => {
              this.setState({
                points: event.nativeEvent.points.map((point) => ({
                  x: point.x / PixelRatio.get(),
                  y: point.y / PixelRatio.get()
                })),
                scanHint: null
              }, () => {
                onCapture()
              })
            }}
          />
        }

        {/* Scan hint */}
        {scanHint !== null &&
          <View style={styles.scanHintContainer}>
            <View
              style={[
                styles.scanHint,
                { backgroundColor: scanHint.color }
              ]}
            >
              <Text style={styles.scanHintText}>
                {scanHint.message}
              </Text>
            </View>
          </View>
        }

        {/* Scan hint */}
        {scanHint !== null &&
          <View style={styles.scanHintContainer}>
            <View
              style={[
                styles.scanHint,
                { backgroundColor: scanHint.color }
              ]}
            >
              <Text style={styles.scanHintText}>
                {scanHint.message}
              </Text>
            </View>
          </View>
        }

        {/* Image cropper (polygon) */}
        {points.length > 0 &&
          <Svg
            style={styles.imageCropperPolygonContainer}
          >
            <Polygon
              points={this._getPolygonPoints()}
              fill='transparent'
              stroke={CROPPER_COLOR}
              strokeWidth='4'
            />
          </Svg>
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
      </View>
    )
  }
}

const IMAGE_CROPPER_POINT_CONTAINER_SIZE = 40
const IMAGE_CROPPER_POINT_SIZE = 20

const CROPPER_COLOR = '#0082CA'

const styles = StyleSheet.create({
  container: {
    flex: 1
  },
  documentScanner: {
    flex: 1
  },
  scanHintContainer: {
    position: 'absolute',
    top: 0,
    bottom: 0,
    left: 0,
    right: 0,
    zIndex: 1,
    elevation: 1,
    alignItems: 'center',
    justifyContent: 'center'
  },
  scanHint: {
    paddingHorizontal: 15,
    paddingVertical: 10,
    textAlign: 'center'
  },
  scanHintText: {
    color: 'white',
    fontSize: 16
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
  imageCropperPointContainer: {
    alignItems: 'center',
    justifyContent: 'center',
    position: 'absolute',
    width: IMAGE_CROPPER_POINT_CONTAINER_SIZE,
    height: IMAGE_CROPPER_POINT_CONTAINER_SIZE,
    marginTop: -IMAGE_CROPPER_POINT_CONTAINER_SIZE / 2,
    marginLeft: -IMAGE_CROPPER_POINT_CONTAINER_SIZE / 2,
    zIndex: 2,
    elevation: 2
  },
  imageCropperPoint: {
    width: IMAGE_CROPPER_POINT_SIZE,
    height: IMAGE_CROPPER_POINT_SIZE,
    borderRadius: IMAGE_CROPPER_POINT_SIZE / 2,
    backgroundColor: CROPPER_COLOR
  },
  imageCropperPolygonContainer: {
    position: 'absolute',
    top: 0,
    bottom: 0,
    left: 0,
    right: 0,
    zIndex: 1,
    elevation: 1
  }
})

export default DocumentScanner
