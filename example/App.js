// External libs
import React, { Component } from 'react'
import { StyleSheet } from 'react-native'
import RNDocumentScanner from 'react-native-document-scanner'

export default class App extends Component {
  render () {
    return (
      <RNDocumentScanner
        scannerRef={(ref) => (this.scanner = ref)}
      />
    )
  }
}

const styles = StyleSheet.create({
  container: {
    flex: 1
  }
})
