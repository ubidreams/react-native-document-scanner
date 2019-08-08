// External libs
import React, { Component } from 'react'
import { StyleSheet, View, Button, Image } from 'react-native'
import RNDocumentScanner from 'react-native-document-scanner'

export default class App extends Component {
  constructor (props) {
    super(props)

    this.state = {
      isCropping: false,
      documentPath: null
    }
  }

  /**
   * When crop button is clicked
   */
  _handlePressCrop = () => {
    this.scanner.cropImage().then((documentPath) => {
      this.setState({ documentPath })
    })
  }

  render () {
    const { isCropping, documentPath } = this.state

    if (documentPath === null) {
      return (
        <View style={styles.container}>
          {/* Document scanner */}
          <RNDocumentScanner
            ref={(ref) => (this.scanner = ref)}
            onCapture={() => this.setState({ isCropping: true })}
          />

          {/* Button to scan document */}
          {isCropping &&
            <Button
              disabled={!isCropping}
              onPress={this._handlePressCrop}
              title='Validate'
              color='#0082CA'
            />
          }
        </View>
      )
    } else {
      return (
        <Image
          style={styles.container}
          source={{ uri: documentPath }}
          resizeMode='contain'
        />
      )
    }
  }
}

const styles = StyleSheet.create({
  container: {
    flex: 1
  }
})
