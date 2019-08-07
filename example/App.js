// External libs
import React, { Component } from 'react'
import { StyleSheet, View } from 'react-native'
import RNDocumentScanner from 'react-native-document-scanner'

export default class App extends Component {
  render () {
    return (
      <View style={styles.container}>
        <RNDocumentScanner />
      </View>
    )
  }
}

const styles = StyleSheet.create({
  container: {
    flex: 1
  }
})
